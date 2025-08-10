package com.eventitta.scheduler;

import com.eventitta.IntegrationTestSupport;
import com.eventitta.auth.repository.RefreshTokenRepository;
import com.eventitta.auth.schedule.RefreshTokenCleanupTask;
import com.eventitta.festivals.scheduler.FestivalScheduler;
import com.eventitta.festivals.service.FestivalService;
import com.eventitta.meeting.repository.MeetingRepository;
import com.eventitta.meeting.scheduler.MeetingStatusScheduler;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 동시 실행 방지 기능 테스트 - 같은 작업이 동시에 여러 번 실행되지 않도록 하는 기능을 확인
 * - 여러 개의 요청이 동시에 와도 한 번만 실행되는지 확인
 * - 데이터베이스에 실행 기록이 올바르게 저장되는지 확인
 * - 다른 종류의 작업은 서로 방해하지 않는지 확인
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("동시 실행 방지 기능 테스트 - 같은 작업이 여러 번 동시에 실행되지 않는지 확인")
class ShedLockIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private FestivalScheduler festivalScheduler;

    @Autowired
    private MeetingStatusScheduler meetingStatusScheduler;

    @Autowired
    private RefreshTokenCleanupTask refreshTokenCleanupTask;

    @MockitoBean
    private FestivalService festivalService;

    @MockitoBean
    private MeetingRepository meetingRepository;

    @MockitoBean
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private JdbcTemplate jdbc;

    private void runConcurrently(int threads, Runnable task) throws Exception {
        CyclicBarrier start = new CyclicBarrier(threads);
        CountDownLatch done = new CountDownLatch(threads);
        ExecutorService es = Executors.newFixedThreadPool(threads);
        for (int i = 0; i < threads; i++) {
            es.submit(() -> {
                try {
                    start.await(2, TimeUnit.SECONDS); // 동시 출발
                    task.run();
                } catch (Exception ignored) {
                } finally {
                    done.countDown();
                }
            });
        }
        assertThat(done.await(5, TimeUnit.SECONDS)).isTrue();
        es.shutdownNow();
    }

    private int lockRowCount(String name) {
        Integer count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM shedlock WHERE name = ?",
            Integer.class, name
        );
        return count == null ? 0 : count;
    }

    private Instant lockUntil(String name) {
        Timestamp ts = jdbc.queryForObject(
            "SELECT lock_until FROM shedlock WHERE name = ?",
            Timestamp.class, name
        );
        return ts == null ? Instant.EPOCH : ts.toInstant();
    }

    private void seedPastLock(String name) {
        jdbc.update(
            "INSERT INTO shedlock (name, lock_until, locked_at, locked_by) VALUES (?, TIMESTAMPADD(SECOND, -5, CURRENT_TIMESTAMP(3)), TIMESTAMPADD(SECOND, -5, CURRENT_TIMESTAMP(3)), 'test') " +
                "ON DUPLICATE KEY UPDATE lock_until = TIMESTAMPADD(SECOND, -5, CURRENT_TIMESTAMP(3)), locked_at = TIMESTAMPADD(SECOND, -5, CURRENT_TIMESTAMP(3))",
            name
        );
    }

    @Test
    @DisplayName("같은 스케줄러를 동시에 2번 실행해도 실제로는 1번만 실행되는지 테스트")
    void givenConcurrentSchedulerExecution_whenSameSchedulerRunsTwice_thenExecutesOnlyOnceAndPersistsLockRow() throws Exception {
        // given
        AtomicInteger nationalSyncCount = new AtomicInteger(0);
        AtomicInteger seoulSyncCount = new AtomicInteger(0);

        // 바디는 약간의 시간을 소비하여 경합을 유도
        doAnswer(invocation -> {
            nationalSyncCount.incrementAndGet();
            Thread.sleep(80);
            return null;
        }).when(festivalService).loadInitialNationalFestivalData();

        doAnswer(invocation -> {
            seoulSyncCount.incrementAndGet();
            Thread.sleep(80);
            return null;
        }).when(festivalService).syncDailySeoulFestivalData();

        // when - 전국/서울 각각 2스레드 동시 실행
        runConcurrently(2, () -> festivalScheduler.syncNationalFestivalData());
        runConcurrently(2, () -> festivalScheduler.syncSeoulFestivalData());

        // then - 바디는 각각 1회만
        assertThat(nationalSyncCount.get()).isEqualTo(1);
        assertThat(seoulSyncCount.get()).isEqualTo(1);
        verify(festivalService, times(1)).loadInitialNationalFestivalData();
        verify(festivalService, times(1)).syncDailySeoulFestivalData();

        // 그리고 shedlock 레코드는 name 당 1행이어야 한다
        assertThat(lockRowCount("syncNationalFestivalData")).isEqualTo(1);
        assertThat(lockRowCount("syncSeoulFestivalData")).isEqualTo(1);

        // lock_until 값이 "최근 시각" 기준으로 유효하게 기록되었는지(=락이 잡혔었는지) 확인
        assertThat(lockUntil("syncNationalFestivalData"))
            .isAfter(Instant.now().minusSeconds(2));
        assertThat(lockUntil("syncSeoulFestivalData"))
            .isAfter(Instant.now().minusSeconds(2));
    }

    @Test
    @DisplayName("첫 번째 작업이 진행 중일 때 두 번째 요청은 기다리지 않고 바로 스킵되는지 테스트")
    void givenFirstTaskRunning_whenSecondInvocationAttempted_thenSkipsImmediatelyWithFastReturn() throws Exception {
        // given: 이전 테스트의 락 상태를 정리
        jdbc.update("DELETE FROM shedlock WHERE name = ?", "syncNationalFestivalData");

        // 첫 호출이 바디에 실제 진입했음을 확실히 보장(락 획득 이후에만 카운트다운)
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch holdLock = new CountDownLatch(1); // 락을 유지하기 위한 신호

        doAnswer(invocation -> {
            entered.countDown();              // 바디 진입 신호(= 락 성공)
            try {
                boolean completed = holdLock.await(3, TimeUnit.SECONDS); // 테스트에서 신호를 줄 때까지 대기
                if (!completed) {
                    throw new RuntimeException("테스트 타임아웃: holdLock 신호를 받지 못했습니다");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("테스트 중단됨", e);
            }
            return null;
        }).when(festivalService).loadInitialNationalFestivalData();

        // 전용 스레드풀(포크조인풀 간섭 방지)
        ExecutorService es = Executors.newFixedThreadPool(2);
        try {
            // when: 첫 호출을 별도 스레드에서 시작
            CompletableFuture<Void> first = CompletableFuture.runAsync(() -> {
                try {
                    festivalScheduler.syncNationalFestivalData();
                } catch (Exception e) {
                    // 예외가 발생해도 테스트는 계속 진행
                }
            }, es);

            // 첫 호출이 실제 바디에 진입했는지 확인(=락을 잡았다는 신호)
            boolean entered1 = entered.await(3, TimeUnit.SECONDS);
            assertThat(entered1)
                .as("첫 호출이 메서드 바디에 진입하지 못했습니다(락 선점 실패/스케줄 지연).")
                .isTrue();

            // 이제 두 번째 호출 시도(즉시 스킵되어야 함)
            long t0 = System.nanoTime();
            CompletableFuture<Void> second = CompletableFuture.runAsync(() -> {
                try {
                    festivalScheduler.syncNationalFestivalData();
                } catch (Exception e) {
                    // 예외가 발생해도 테스트는 계속 진행
                }
            }, es);

            // 두 번째 호출이 즉시 완료되는지 확인
            second.get(1, TimeUnit.SECONDS); // 1초 내에 완료되어야 함
            long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t0);

            // 첫 번째 작업 완료 신호
            holdLock.countDown();
            first.get(2, TimeUnit.SECONDS);

            // then: 바디는 1회만, 두 번째는 즉시 스킵되어 총 시간은 짧다
            verify(festivalService, times(1)).loadInitialNationalFestivalData();
            assertThat(lockRowCount("syncNationalFestivalData")).isEqualTo(1);
            assertThat(elapsedMs).isLessThan(500); // 더 엄격한 시간 제한
        } finally {
            holdLock.countDown(); // 혹시 모를 데드락 방지
            es.shutdownNow();
        }
    }

    @Test
    @DisplayName("서로 다른 종류의 스케줄러는 동시에 실행될 수 있는지 테스트 (간섭하지 않음)")
    void givenDifferentSchedulerTypes_whenExecutedConcurrently_thenRunIsolatedWithoutInterference() throws Exception {
        AtomicInteger national = new AtomicInteger();
        AtomicInteger seoul = new AtomicInteger();

        doAnswer(inv -> {
            national.incrementAndGet();
            Thread.sleep(60);
            return null;
        })
            .when(festivalService).loadInitialNationalFestivalData();
        doAnswer(inv -> {
            seoul.incrementAndGet();
            Thread.sleep(60);
            return null;
        })
            .when(festivalService).syncDailySeoulFestivalData();

        // 선행 락 레코드를 과거 시각으로 심어 UPDATE 경로로 락 획득을 유도
        seedPastLock("syncNationalFestivalData");
        seedPastLock("syncSeoulFestivalData");
        runConcurrently(2, () -> festivalScheduler.syncNationalFestivalData());
        runConcurrently(2, () -> festivalScheduler.syncSeoulFestivalData());

        assertThat(national.get()).isEqualTo(1);
        assertThat(seoul.get()).isEqualTo(1);
        assertThat(lockRowCount("syncNationalFestivalData")).isEqualTo(1);
        assertThat(lockRowCount("syncSeoulFestivalData")).isEqualTo(1);
    }

    @Test
    @DisplayName("모임 상태 업데이트 스케줄러를 동시에 실행해도 한 번만 실행되는지 테스트")
    void givenConcurrentMeetingStatusScheduler_whenExecutedSimultaneously_thenExecutesOnlyOnce() throws Exception {
        // given
        AtomicInteger executionCount = new AtomicInteger(0);

        when(meetingRepository.updateStatusToFinished(any(), any()))
            .thenAnswer(invocation -> {
                executionCount.incrementAndGet();
                Thread.sleep(80);
                return 1;
            });

        // when
        runConcurrently(2, () -> meetingStatusScheduler.markFinishedMeetings());

        // then
        assertThat(executionCount.get()).isEqualTo(1);
        verify(meetingRepository, times(1)).updateStatusToFinished(any(), any());
        assertThat(lockRowCount("markFinishedMeetings")).isEqualTo(1);
    }

    @Test
    @DisplayName("만료된 토큰 삭제 스케줄러를 동시에 실행해도 한 번만 실행되는지 테스트")
    void givenConcurrentRefreshTokenCleanup_whenExecutedSimultaneously_thenExecutesOnlyOnce() throws Exception {
        // given
        AtomicInteger executionCount = new AtomicInteger(0);

        when(refreshTokenRepository.deleteByExpiresAtBefore(any()))
            .thenAnswer(invocation -> {
                executionCount.incrementAndGet();
                Thread.sleep(50);
                return 5L; // 5개 삭제됨
            });

        // when
        runConcurrently(2, () -> refreshTokenCleanupTask.removeExpiredRefreshTokens());

        // then
        assertThat(executionCount.get()).isEqualTo(1);
        verify(refreshTokenRepository, times(1)).deleteByExpiresAtBefore(any());
        assertThat(lockRowCount("removeExpiredRefreshTokens")).isEqualTo(1);
    }

    @Test
    @DisplayName("동시 실행 방지 설정이 올바르게 되어있는지 확인하는 테스트")
    void givenSchedulerLockAnnotation_whenCheckingConfiguration_thenVerifiesCorrectSettings() throws Exception {
        var m1 = FestivalScheduler.class.getMethod("syncNationalFestivalData");
        var a1 = m1.getAnnotation(SchedulerLock.class);
        assertThat(a1).isNotNull();
        assertThat(a1.name()).isEqualTo("syncNationalFestivalData");

        var m2 = FestivalScheduler.class.getMethod("syncSeoulFestivalData");
        var a2 = m2.getAnnotation(SchedulerLock.class);
        assertThat(a2).isNotNull();
        assertThat(a2.name()).isEqualTo("syncSeoulFestivalData");
    }
}
