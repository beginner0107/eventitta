package com.eventitta.notification.service;

import com.eventitta.notification.domain.AlertLevel;
import com.eventitta.notification.service.ratelimit.SlidingWindowLogRateLimiter;
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
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SlidingWindowLogRateLimiterTest {

    @Mock
    private Clock clock;

    private SlidingWindowLogRateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        rateLimiter = new SlidingWindowLogRateLimiter(clock);
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
            assertThat(result).isTrue();
        }

        // 제한 초과시 거부
        boolean exceededResult = rateLimiter.shouldSendAlert("ERROR_001", AlertLevel.CRITICAL);
        assertThat(exceededResult).isFalse();
    }

    @Test
    @DisplayName("시간 윈도우 슬라이딩 - 오래된 요청은 자동으로 만료되어 새 요청이 허용된다")
    void shouldSlideWindowAndAllowNewRequests() {
        // given
        String uniqueErrorCode = "SLIDE_TEST_" + System.nanoTime();
        long startTime = 1000000000L;
        long afterWindow = startTime + (5 * 60 * 1000) + 1;

        // when: 초기 시간에 제한까지 사용
        when(clock.millis()).thenReturn(startTime);
        boolean firstRequest = rateLimiter.shouldSendAlert(uniqueErrorCode, AlertLevel.INFO);
        assertThat(firstRequest).isTrue();

        // 제한 초과 확인
        boolean exceededAtStart = rateLimiter.shouldSendAlert(uniqueErrorCode, AlertLevel.INFO);
        assertThat(exceededAtStart).isFalse();

        // then: 5분 후에는 다시 허용
        when(clock.millis()).thenReturn(afterWindow);
        boolean allowedAfterSlide = rateLimiter.shouldSendAlert(uniqueErrorCode, AlertLevel.INFO);
        assertThat(allowedAfterSlide).isTrue();
    }

    @Test
    @DisplayName("부분적 윈도우 슬라이딩 - 일부 요청만 만료되어 정확한 개수만큼 허용된다")
    void shouldPartiallySlideWindow() {
        // given
        String uniqueErrorCode = "PARTIAL_TEST_" + System.nanoTime();
        long startTime = 1000000000L;
        long halfWindow = startTime + (2 * 60 * 1000) + (30 * 1000); // 2분 30초 후
        long afterFullWindow = startTime + (5 * 60 * 1000) + 1; // 5분 1밀리초 후

        // when: 초기에 2개 요청 (MEDIUM 레벨, 제한 2개)
        when(clock.millis()).thenReturn(startTime);
        boolean first = rateLimiter.shouldSendAlert(uniqueErrorCode, AlertLevel.MEDIUM);
        assertThat(first).isTrue();
        boolean second = rateLimiter.shouldSendAlert(uniqueErrorCode, AlertLevel.MEDIUM);
        assertThat(second).isTrue();

        // 2분 30초 후에 1개 더 요청 (총 3개, 제한 초과)
        when(clock.millis()).thenReturn(halfWindow);
        boolean exceededAtHalf = rateLimiter.shouldSendAlert(uniqueErrorCode, AlertLevel.MEDIUM);
        assertThat(exceededAtHalf).isFalse();

        // then: 5분 1밀리초 후에는 초기 요청들이 만료되어 새 요청 허용
        when(clock.millis()).thenReturn(afterFullWindow);
        boolean allowedAfterWindow = rateLimiter.shouldSendAlert(uniqueErrorCode, AlertLevel.MEDIUM);
        assertThat(allowedAfterWindow).isTrue();
    }

    @Test
    @DisplayName("정확한 윈도우 경계 처리 - 정확히 5분 경계에서의 동작")
    void shouldHandleExactWindowBoundary() {
        // given
        long startTime = 1000000000L;
        long exactWindow = startTime + (5 * 60 * 1000); // 정확히 5분 후
        String uniqueErrorCode = "BOUNDARY_TEST_" + System.nanoTime();

        // when: 첫 번째 요청
        when(clock.millis()).thenReturn(startTime);
        boolean first = rateLimiter.shouldSendAlert(uniqueErrorCode, AlertLevel.INFO);
        assertThat(first).isTrue();

        // then: 정확히 5분 후에는 이전 타임스탬프가 제거되어 허용됨 (<= 조건)
        when(clock.millis()).thenReturn(exactWindow);
        boolean allowedAtBoundary = rateLimiter.shouldSendAlert(uniqueErrorCode, AlertLevel.INFO);
        assertThat(allowedAtBoundary).isTrue(); // <= 조건이므로 경계에서 제거되어 허용
    }

    @Test
    @DisplayName("알림 중요도별 제한 - 중요한 알림은 많이, 일반 알림은 적게 보낼 수 있다")
    void shouldApplyDifferentLimitsPerAlertLevel() {
        // given
        long fixedTime = 1000000000L;
        when(clock.millis()).thenReturn(fixedTime);

        // when & then: CRITICAL (10개)
        for (int i = 0; i < AlertLevel.CRITICAL.getAlertLimit(); i++) {
            boolean result = rateLimiter.shouldSendAlert("ERROR_001", AlertLevel.CRITICAL);
            assertThat(result).isTrue();
        }

        // 제한 초과시 거부
        boolean exceededResult = rateLimiter.shouldSendAlert("ERROR_001", AlertLevel.CRITICAL);
        assertThat(exceededResult).isFalse();

        // when & then: INFO (1개)
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

        // when: 20개 스레드에서 동시에 요청
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    if (rateLimiter.shouldSendAlert("CONCURRENT_TEST", AlertLevel.CRITICAL)) {
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
    @DisplayName("연속적인 윈도우 슬라이딩 - 시간이 계속 지나면서 요청이 지속적으로 허용된다")
    void shouldContinuouslySlideWindow() {
        // given
        String uniqueErrorCode = "CONTINUOUS_TEST_" + System.nanoTime();
        long startTime = 1000000000L;

        // when: 초기 요청
        when(clock.millis()).thenReturn(startTime);
        boolean firstAllowed = rateLimiter.shouldSendAlert(uniqueErrorCode, AlertLevel.INFO);
        assertThat(firstAllowed).isTrue();

        // 제한 확인
        boolean secondBlocked = rateLimiter.shouldSendAlert(uniqueErrorCode, AlertLevel.INFO);
        assertThat(secondBlocked).isFalse();

        // then: 매 5분마다 새로운 요청이 허용됨
        for (int interval = 1; interval <= 3; interval++) {
            long timeAfterIntervals = startTime + (interval * 5 * 60 * 1000) + 1; // 5분 간격 + 1ms
            when(clock.millis()).thenReturn(timeAfterIntervals);

            boolean allowed = rateLimiter.shouldSendAlert(uniqueErrorCode, AlertLevel.INFO);
            assertThat(allowed).isTrue();

            // 바로 다음 요청은 거부되어야 함
            boolean blocked = rateLimiter.shouldSendAlert(uniqueErrorCode, AlertLevel.INFO);
            assertThat(blocked).isFalse();
        }
    }

    @Test
    @DisplayName("복합 시나리오 - 여러 에러 코드와 레벨이 섞인 실제 상황을 시뮬레이션한다")
    void shouldHandleComplexScenario() {
        // given
        long startTime = 1000000000L;
        when(clock.millis()).thenReturn(startTime);

        // when: 다양한 알림 레벨과 에러 코드로 요청
        // CRITICAL 에러 5개
        for (int i = 0; i < 5; i++) {
            assertThat(rateLimiter.shouldSendAlert("DB_CONNECTION_FAILED", AlertLevel.CRITICAL)).isTrue();
        }

        // HIGH 에러 3개
        for (int i = 0; i < 3; i++) {
            assertThat(rateLimiter.shouldSendAlert("PAYMENT_FAILED", AlertLevel.HIGH)).isTrue();
        }

        // INFO 에러 1개
        assertThat(rateLimiter.shouldSendAlert("USER_LOGIN", AlertLevel.INFO)).isTrue();

        // then: 각 레벨별로 추가 요청 제한 확인
        // CRITICAL은 5개 더 허용 (총 10개)
        for (int i = 0; i < 5; i++) {
            assertThat(rateLimiter.shouldSendAlert("DB_CONNECTION_FAILED", AlertLevel.CRITICAL)).isTrue();
        }
        assertThat(rateLimiter.shouldSendAlert("DB_CONNECTION_FAILED", AlertLevel.CRITICAL)).isFalse();

        // HIGH는 2개 더 허용 (총 5개)
        for (int i = 0; i < 2; i++) {
            assertThat(rateLimiter.shouldSendAlert("PAYMENT_FAILED", AlertLevel.HIGH)).isTrue();
        }
        assertThat(rateLimiter.shouldSendAlert("PAYMENT_FAILED", AlertLevel.HIGH)).isFalse();

        // INFO는 이미 제한 도달
        assertThat(rateLimiter.shouldSendAlert("USER_LOGIN", AlertLevel.INFO)).isFalse();
    }

    @Test
    @DisplayName("메모리 효율성 - 시간이 지나면서 오래된 타임스탬프가 정리된다")
    void shouldCleanUpOldTimestamps() {
        // given
        String uniqueErrorCode = "CLEANUP_TEST_" + System.nanoTime();
        long startTime = 1000000000L;
        long afterWindow = startTime + (5 * 60 * 1000) + 1; // 5분 1밀리초 후

        // when: 여러 요청으로 타임스탬프 생성
        when(clock.millis()).thenReturn(startTime);
        for (int i = 0; i < AlertLevel.MEDIUM.getAlertLimit(); i++) {
            boolean result = rateLimiter.shouldSendAlert(uniqueErrorCode, AlertLevel.MEDIUM);
            assertThat(result).isTrue();
        }

        // then: 윈도우 이후 새 요청시 오래된 타임스탬프가 정리되고 새 요청 허용
        when(clock.millis()).thenReturn(afterWindow);
        boolean cleanedAndAllowed = rateLimiter.shouldSendAlert(uniqueErrorCode, AlertLevel.MEDIUM);
        assertThat(cleanedAndAllowed).isTrue();

        // 제한까지 다시 채울 수 있음 (이전 타임스탬프들이 정리되었다는 증거)
        for (int i = 1; i < AlertLevel.MEDIUM.getAlertLimit(); i++) {
            assertThat(rateLimiter.shouldSendAlert(uniqueErrorCode, AlertLevel.MEDIUM)).isTrue();
        }
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
}
