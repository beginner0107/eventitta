package com.eventitta.common.notification.service;

import com.eventitta.common.notification.domain.AlertLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SlidingWindowCounterRateLimiterTest {

    @Mock
    private Clock clock;

    private SlidingWindowCounterRateLimiter rateLimiter;

    private static final long BUCKET_SIZE_MILLIS = TimeUnit.MINUTES.toMillis(1);
    private static final int NUM_BUCKETS = 5;

    @BeforeEach
    void setUp() {
        rateLimiter = new SlidingWindowCounterRateLimiter(clock);
        rateLimiter.reset();
    }

    @Test
    @DisplayName("알림 횟수 제한 - 정해진 횟수까지만 알림을 보낼 수 있다")
    void shouldAllowRequestsWithinLimit() {
        // given
        long fixedTime = 1000000000L;
        when(clock.millis()).thenReturn(fixedTime);

        // when & then: 제한 내에서는 허용
        for (int i = 1; i <= AlertLevel.CRITICAL.getAlertLimit(); i++) {
            boolean result = rateLimiter.shouldSendAlert("ERROR_001", AlertLevel.CRITICAL);
            assertThat(result)
                .as("Request %d should be allowed", i)
                .isTrue();
        }

        // 제한 초과시 거부
        boolean exceededResult = rateLimiter.shouldSendAlert("ERROR_001", AlertLevel.CRITICAL);
        assertThat(exceededResult).isFalse();
    }

    @Test
    @DisplayName("버킷 단위 슬라이딩 - 새로운 버킷으로 이동하면서 오래된 버킷이 윈도우에서 제외된다")
    void shouldSlideWindowByBuckets() {
        // given
        String uniqueErrorCode = "BUCKET_SLIDE_TEST_" + System.nanoTime();
        long startTime = 1000000000L;
        long nextBucket = startTime + BUCKET_SIZE_MILLIS; // 다음 버킷 (1분 후)
        long afterFiveBuckets = startTime + (NUM_BUCKETS * BUCKET_SIZE_MILLIS); // 5분 후

        // when: 첫 버킷에서 제한까지 사용
        when(clock.millis()).thenReturn(startTime);
        boolean firstRequest = rateLimiter.shouldSendAlert(uniqueErrorCode, AlertLevel.INFO);
        assertThat(firstRequest).isTrue();

        // 같은 버킷에서 초과
        boolean exceededInSameBucket = rateLimiter.shouldSendAlert(uniqueErrorCode, AlertLevel.INFO);
        assertThat(exceededInSameBucket).isFalse();

        // 다음 버킷으로 이동해도 여전히 윈도우 내에 있으므로 제한
        when(clock.millis()).thenReturn(nextBucket);
        boolean stillBlocked = rateLimiter.shouldSendAlert(uniqueErrorCode, AlertLevel.INFO);
        assertThat(stillBlocked).isFalse();

        // then: 5분 후 첫 버킷이 윈도우에서 벗어나면 다시 허용
        when(clock.millis()).thenReturn(afterFiveBuckets);
        boolean allowedAfterWindow = rateLimiter.shouldSendAlert(uniqueErrorCode, AlertLevel.INFO);
        assertThat(allowedAfterWindow).isTrue();
    }

    @Test
    @DisplayName("버킷 경계 처리 - 버킷 경계에서 정확한 카운팅")
    void shouldHandleBucketBoundaries() {
        // given
        long bucketStart = (1000000000L / BUCKET_SIZE_MILLIS) * BUCKET_SIZE_MILLIS;
        long justBeforeBoundary = bucketStart + BUCKET_SIZE_MILLIS - 1; // 버킷 끝
        long justAfterBoundary = bucketStart + BUCKET_SIZE_MILLIS; // 다음 버킷 시작
        String uniqueErrorCode = "BOUNDARY_TEST_" + System.nanoTime();

        // when: 버킷 끝에서 요청
        when(clock.millis()).thenReturn(justBeforeBoundary);
        boolean firstInBucket = rateLimiter.shouldSendAlert(uniqueErrorCode, AlertLevel.INFO);
        assertThat(firstInBucket).isTrue();

        // 같은 버킷에서 두 번째 요청은 거부
        boolean secondInSameBucket = rateLimiter.shouldSendAlert(uniqueErrorCode, AlertLevel.INFO);
        assertThat(secondInSameBucket).isFalse();

        // then: 다음 버킷 시작에서는 여전히 전체 윈도우 카운트 때문에 거부
        when(clock.millis()).thenReturn(justAfterBoundary);
        boolean firstInNextBucket = rateLimiter.shouldSendAlert(uniqueErrorCode, AlertLevel.INFO);
        assertThat(firstInNextBucket).isFalse(); // 5분 윈도우 내 총 2개가 되므로 거부
    }

    @Test
    @DisplayName("다중 버킷 누적 - 여러 버킷에 걸쳐 요청이 누적되어 계산된다")
    void shouldAccumulateAcrossMultipleBuckets() {
        // given
        String uniqueErrorCode = "MULTI_BUCKET_" + System.nanoTime();
        long startTime = 1000000000L;

        // when: MEDIUM (제한 2개)를 여러 버킷에 걸쳐 사용
        // 첫 번째 버킷에서 1개
        when(clock.millis()).thenReturn(startTime);
        boolean first = rateLimiter.shouldSendAlert(uniqueErrorCode, AlertLevel.MEDIUM);
        assertThat(first).isTrue();

        // 두 번째 버킷에서 1개
        when(clock.millis()).thenReturn(startTime + BUCKET_SIZE_MILLIS);
        boolean second = rateLimiter.shouldSendAlert(uniqueErrorCode, AlertLevel.MEDIUM);
        assertThat(second).isTrue();

        // 세 번째 버킷에서 시도하면 거부 (윈도우 내 총 2개)
        when(clock.millis()).thenReturn(startTime + (2 * BUCKET_SIZE_MILLIS));
        boolean third = rateLimiter.shouldSendAlert(uniqueErrorCode, AlertLevel.MEDIUM);
        assertThat(third).isFalse();
    }

    @Test
    @DisplayName("알림 중요도별 제한 - 중요한 알림은 많이, 일반 알림은 적게 보낼 수 있다")
    void shouldApplyDifferentLimitsPerAlertLevel() {
        // given
        long fixedTime = 1000000000L;
        when(clock.millis()).thenReturn(fixedTime);

        // when & then: CRITICAL (10개)
        for (int i = 0; i < AlertLevel.CRITICAL.getAlertLimit(); i++) {
            assertThat(rateLimiter.shouldSendAlert("CRITICAL_ERROR", AlertLevel.CRITICAL))
                .isTrue();
        }
        assertThat(rateLimiter.shouldSendAlert("CRITICAL_ERROR", AlertLevel.CRITICAL)).isFalse();

        // HIGH (5개)
        for (int i = 0; i < AlertLevel.HIGH.getAlertLimit(); i++) {
            assertThat(rateLimiter.shouldSendAlert("HIGH_ERROR", AlertLevel.HIGH))
                .isTrue();
        }
        assertThat(rateLimiter.shouldSendAlert("HIGH_ERROR", AlertLevel.HIGH)).isFalse();

        // INFO (1개)
        assertThat(rateLimiter.shouldSendAlert("INFO_ERROR", AlertLevel.INFO)).isTrue();
        assertThat(rateLimiter.shouldSendAlert("INFO_ERROR", AlertLevel.INFO)).isFalse();
    }

    @Test
    @DisplayName("에러 코드별 독립 카운팅 - 다른 에러는 각각 별도로 제한이 적용된다")
    void shouldCountIndependentlyPerErrorCode() {
        // given
        long fixedTime = 1000000000L;
        when(clock.millis()).thenReturn(fixedTime);

        // when & then: 첫 번째 에러코드로 제한까지 사용
        rateLimiter.shouldSendAlert("ERROR_A", AlertLevel.INFO);
        boolean firstErrorExceeded = rateLimiter.shouldSendAlert("ERROR_A", AlertLevel.INFO);
        assertThat(firstErrorExceeded).isFalse();

        // 두 번째 에러코드는 여전히 허용
        boolean secondErrorAllowed = rateLimiter.shouldSendAlert("ERROR_B", AlertLevel.INFO);
        assertThat(secondErrorAllowed).isTrue();
    }

    @Test
    @DisplayName("동시 접근 처리 - 여러 요청이 동시에 와도 정확한 개수만 허용된다")
    void shouldHandleConcurrentRequests() throws InterruptedException {
        // given
        long fixedTime = 1000000000L;
        when(clock.millis()).thenReturn(fixedTime);

        int threadCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        String uniqueErrorCode = "CONCURRENT_TEST_" + System.nanoTime();

        // when: 20개 스레드에서 동시에 요청
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    if (rateLimiter.shouldSendAlert(uniqueErrorCode, AlertLevel.CRITICAL)) {
                        successCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        // then: CRITICAL_LIMIT(10)개만 허용되어야 함
        assertThat(successCount.get()).isEqualTo(AlertLevel.CRITICAL.getAlertLimit());
    }

    @Test
    @DisplayName("윈도우 슬라이딩 시뮬레이션 - 5개 버킷 윈도우")
    void shouldSimulateWindowSliding() {
        // given
        String uniqueErrorCode = "SLIDING_SIM_" + System.nanoTime();
        long baseTime = (1000000000L / BUCKET_SIZE_MILLIS) * BUCKET_SIZE_MILLIS;

        // when: 매 분마다 1개씩 요청 (INFO는 5분 윈도우에 1개만 허용)
        for (int minute = 0; minute < 10; minute++) {
            long currentTime = baseTime + (minute * BUCKET_SIZE_MILLIS);
            when(clock.millis()).thenReturn(currentTime);

            boolean result = rateLimiter.shouldSendAlert(uniqueErrorCode, AlertLevel.INFO);

            if (minute == 0) {
                assertThat(result)
                    .isTrue();
            } else if (minute < 5) {
                assertThat(result)
                    .isFalse();
            } else if (minute == 5) {
                assertThat(result)
                    .isTrue();
                break;
            }
        }
    }

    @Test
    @DisplayName("scheduledCleanup 테스트 - 오래된 버킷 데이터 정리")
    void shouldCleanupOldBuckets() {
        // given
        long startTime = 1000000000L;
        long oldTime = startTime - (10 * BUCKET_SIZE_MILLIS); // 10분 전

        // when: 오래된 시간과 최근 시간에 요청
        when(clock.millis()).thenReturn(oldTime);
        rateLimiter.shouldSendAlert("OLD_ERROR", AlertLevel.INFO);

        when(clock.millis()).thenReturn(startTime);
        rateLimiter.shouldSendAlert("RECENT_ERROR", AlertLevel.INFO);

        // cleanup 실행
        when(clock.millis()).thenReturn(startTime);
        rateLimiter.scheduledCleanup();

        // then: 최근 요청은 유지, 오래된 요청은 정리되어 새 요청 허용
        when(clock.millis()).thenReturn(startTime);
        boolean recentStillBlocked = rateLimiter.shouldSendAlert("RECENT_ERROR", AlertLevel.INFO);
        assertThat(recentStillBlocked).isFalse(); // 여전히 제한

        // 오래된 에러는 정리되어 있어야 하지만, 새로운 버킷에서는 허용
        when(clock.millis()).thenReturn(startTime);
        boolean oldAllowed = rateLimiter.shouldSendAlert("OLD_ERROR", AlertLevel.INFO);
        assertThat(oldAllowed).isTrue(); // 오래된 데이터 정리되어 허용
    }

    @Test
    @DisplayName("리셋 기능 - 모든 알림 기록을 초기화할 수 있다")
    void shouldResetAllAlerts() {
        // given
        long fixedTime = 1000000000L;
        when(clock.millis()).thenReturn(fixedTime);

        // when: 제한까지 사용
        rateLimiter.shouldSendAlert("RESET_TEST", AlertLevel.INFO);
        boolean beforeReset = rateLimiter.shouldSendAlert("RESET_TEST", AlertLevel.INFO);
        assertThat(beforeReset).isFalse();

        // reset 후
        rateLimiter.reset();

        // then: 다시 허용됨
        boolean afterReset = rateLimiter.shouldSendAlert("RESET_TEST", AlertLevel.INFO);
        assertThat(afterReset).isTrue();
    }

    @Test
    @DisplayName("복합 시나리오 - 여러 에러 코드와 레벨이 섞인 실제 상황을 시뮬레이션한다")
    void shouldHandleComplexScenario() {
        // given
        long startTime = 1000000000L;
        when(clock.millis()).thenReturn(startTime);

        // when: 다양한 알림 레벨과 에러 코드로 요청
        for (int i = 0; i < 3; i++) {
            assertThat(rateLimiter.shouldSendAlert("DB_ERROR", AlertLevel.CRITICAL)).isTrue();
        }

        // 버킷 2: Payment 에러
        when(clock.millis()).thenReturn(startTime + BUCKET_SIZE_MILLIS);
        for (int i = 0; i < 2; i++) {
            assertThat(rateLimiter.shouldSendAlert("PAYMENT_ERROR", AlertLevel.HIGH)).isTrue();
        }

        // 버킷 3: 다양한 에러
        when(clock.millis()).thenReturn(startTime + (2 * BUCKET_SIZE_MILLIS));
        assertThat(rateLimiter.shouldSendAlert("DB_ERROR", AlertLevel.CRITICAL)).isTrue(); // 4/10
        assertThat(rateLimiter.shouldSendAlert("PAYMENT_ERROR", AlertLevel.HIGH)).isTrue(); // 3/5
        assertThat(rateLimiter.shouldSendAlert("USER_LOGIN", AlertLevel.INFO)).isTrue(); // 1/1

        // then: 각 제한 확인
        assertThat(rateLimiter.shouldSendAlert("USER_LOGIN", AlertLevel.INFO)).isFalse(); // INFO 초과

        // DB는 아직 여유 있음 (4/10)
        for (int i = 0; i < 6; i++) {
            assertThat(rateLimiter.shouldSendAlert("DB_ERROR", AlertLevel.CRITICAL)).isTrue();
        }
        assertThat(rateLimiter.shouldSendAlert("DB_ERROR", AlertLevel.CRITICAL)).isFalse(); // 이제 초과
    }
}
