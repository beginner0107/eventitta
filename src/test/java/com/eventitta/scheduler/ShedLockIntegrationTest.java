package com.eventitta.scheduler;

import com.eventitta.IntegrationTestSupport;
import com.eventitta.auth.repository.RefreshTokenRepository;
import com.eventitta.auth.scheduler.RefreshTokenCleanupTask;
import com.eventitta.festivals.scheduler.FestivalScheduler;
import com.eventitta.festivals.service.FestivalService;
import com.eventitta.meeting.repository.MeetingRepository;
import com.eventitta.meeting.scheduler.MeetingStatusScheduler;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.sql.Timestamp;
import java.time.Duration;
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
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("동시 실행 방지 기능 테스트 - 같은 작업이 여러 번 동시에 실행되지 않는지 확인")
@SpringBootTest(properties = {
    "scheduler.festival-sync.enabled=true",
    "scheduler.meeting-status.enabled=true",
    "scheduler.token-cleanup.enabled=true"
})
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

    @Autowired
    private LockProvider lockProvider;

    private void runConcurrently(Runnable task) throws Exception {
        int threads = 2;
        CyclicBarrier start = new CyclicBarrier(threads);
        CountDownLatch done = new CountDownLatch(threads);
        ExecutorService es = Executors.newFixedThreadPool(threads);
        for (int i = 0; i < threads; i++) {
            es.submit(() -> {
                try {
                    start.await(2, TimeUnit.SECONDS);
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

    @Test
    @DisplayName("같은 스케줄러를 동시에 2번 실행해도 실제로는 1번만 실행되는지 테스트")
    void givenConcurrentSchedulerExecution_whenSameSchedulerRunsTwice_thenExecutesOnlyOnceAndPersistsLockRow() throws Exception {
        AtomicInteger executionCount = new AtomicInteger(0);
        doAnswer(inv -> {
            executionCount.incrementAndGet();
            return null;
        }).when(festivalService).loadInitialNationalFestivalData();

        Runnable attemptLockAndRun = () -> {
            LockConfiguration cfg = new LockConfiguration(
                Instant.now(),
                "syncNationalFestivalData",
                Duration.ofMinutes(2),
                Duration.ofSeconds(30)
            );

            lockProvider.lock(cfg).ifPresent(lock -> {
                try {
                    festivalService.loadInitialNationalFestivalData();
                    Thread.sleep(80);
                } catch (InterruptedException ignored) {
                } finally {
                    lock.unlock();
                }
            });
        };

        runConcurrently(attemptLockAndRun);

        Thread.sleep(100);

        assertThat(executionCount.get()).isEqualTo(1);
        verify(festivalService, times(1)).loadInitialNationalFestivalData();
        assertThat(lockRowCount("syncNationalFestivalData")).isEqualTo(1);
        assertThat(lockUntil("syncNationalFestivalData")).isAfter(Instant.now().minusSeconds(5));
    }

    @Test
    @DisplayName("모임 상태 업데이트 스케줄러를 동시에 실행해도 한 번만 실행되는지 테스트")
    void givenConcurrentMeetingStatusScheduler_whenExecutedSimultaneously_thenExecutesOnlyOnce() throws Exception {
        AtomicInteger executionCount = new AtomicInteger(0);

        when(meetingRepository.updateStatusToFinished(any(), any()))
            .thenAnswer(invocation -> {
                executionCount.incrementAndGet();
                Thread.sleep(80);
                return 1;
            });

        runConcurrently(() -> meetingStatusScheduler.markFinishedMeetings());

        Thread.sleep(100); // 락 row 생성 대기

        assertThat(executionCount.get()).isEqualTo(1);
        verify(meetingRepository, times(1)).updateStatusToFinished(any(), any());
        assertThat(lockRowCount("markFinishedMeetings")).isEqualTo(1);
    }

    @Test
    @DisplayName("만료된 토큰 삭제 스케줄러를 동시에 실행해도 한 번만 실행되는지 테스트")
    void givenConcurrentRefreshTokenCleanup_whenExecutedSimultaneously_thenExecutesOnlyOnce() throws Exception {
        AtomicInteger executionCount = new AtomicInteger(0);

        when(refreshTokenRepository.deleteByExpiresAtBefore(any()))
            .thenAnswer(invocation -> {
                executionCount.incrementAndGet();
                Thread.sleep(50);
                return 5L;
            });

        runConcurrently(() -> refreshTokenCleanupTask.removeExpiredRefreshTokens());

        Thread.sleep(100); // 락 row 생성 대기

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
