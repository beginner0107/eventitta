package com.eventitta.notification.service;

import com.eventitta.notification.domain.AlertLevel;
import com.eventitta.notification.service.ratelimit.FixedWindowRateLimiter;
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

import static com.eventitta.notification.service.ratelimit.FixedWindowRateLimiter.CLEANUP_LOOKBACK_MILLIS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FixedWindowRateLimiterTest {

    @Mock
    private Clock clock;

    private FixedWindowRateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        rateLimiter = new FixedWindowRateLimiter(clock);
    }

    @Test
    @DisplayName("알림 횟수 제한 - 정해진 횟수까지만 알림을 보낼 수 있다")
    void shouldAllowRequestsWithinLimit() {
        // given
        long fixedTime = 1000000000L; // 고정된 시간
        when(clock.millis()).thenReturn(fixedTime);

        // when & then
        for (int i = 1; i <= AlertLevel.CRITICAL.getAlertLimit(); i++) {
            boolean result = rateLimiter.shouldSendAlert("ERROR_001", AlertLevel.CRITICAL);
            assertThat(result).isTrue();
        }

        boolean exceededResult = rateLimiter.shouldSendAlert("ERROR_001", AlertLevel.CRITICAL);
        assertThat(exceededResult).isFalse();
    }

    @Test
    @DisplayName("시간 구간별 독립 카운팅 - 시간이 지나면 알림 횟수가 초기화된다")
    void shouldCountIndependentlyPerWindow() {
        // given
        long firstWindow = 1000000000L;   // 첫 번째 윈도우
        long secondWindow = 1000060000L;  // 두 번째 윈도우 (1분 후)

        // when: 첫 번째 윈도우에서 제한까지 사용
        when(clock.millis()).thenReturn(firstWindow);
        for (int i = 0; i < AlertLevel.CRITICAL.getAlertLimit(); i++) {
            rateLimiter.shouldSendAlert("ERROR_001", AlertLevel.CRITICAL);
        }
        boolean firstWindowExceeded = rateLimiter.shouldSendAlert("ERROR_001", AlertLevel.CRITICAL);
        assertThat(firstWindowExceeded).isFalse();

        // then: 새 윈도우에서는 다시 허용
        when(clock.millis()).thenReturn(secondWindow);
        boolean secondWindowAllowed = rateLimiter.shouldSendAlert("ERROR_001", AlertLevel.CRITICAL);
        assertThat(secondWindowAllowed).isTrue();
    }

    @Test
    @DisplayName("시간 경계선 문제 - 시간 구간이 바뀌는 순간 예상보다 많은 알림이 허용된다")
    void shouldDemonstrateBurstVulnerability() {
        // given
        long firstWindowTime = 1000020000L;   // 첫 번째 윈도우
        long secondWindowTime = 1000080000L;  // 두 번째 윈도우 (60초 후)

        // when: 첫 번째 윈도우 끝에서 최대치까지 요청
        when(clock.millis()).thenReturn(firstWindowTime);
        for (int i = 0; i < AlertLevel.CRITICAL.getAlertLimit(); i++) {
            boolean result = rateLimiter.shouldSendAlert("ERROR_001", AlertLevel.CRITICAL);
            assertThat(result).isTrue();
        }

        // then: 새 윈도우에서 또 최대치까지 요청 가능
        when(clock.millis()).thenReturn(secondWindowTime);
        for (int i = 0; i < AlertLevel.CRITICAL.getAlertLimit(); i++) {
            boolean result = rateLimiter.shouldSendAlert("ERROR_001", AlertLevel.CRITICAL);
            assertThat(result).isTrue();
        }
    }

    @Test
    @DisplayName("불공평한 처리 - 먼저 온 요청이 모든 할당량을 사용하면 나중 요청은 거부된다")
    void shouldDemonstrateUnfairProcessing() {
        // given
        long windowStart = 1000080000L;     // 윈도우 시작
        long windowMiddle = 1000110000L;    // 같은 윈도우 중간 (30초 후)

        // 같은 윈도우인지 검증
        long normalizedStart = (windowStart / 60000) * 60000;
        long normalizedMiddle = (windowMiddle / 60000) * 60000;
        assertThat(normalizedStart).isEqualTo(normalizedMiddle);

        // when: 윈도우 시작 시점에 모든 할당량을 사용 (같은 errorCode 사용!)
        when(clock.millis()).thenReturn(windowStart);
        for (int i = 0; i < AlertLevel.INFO.getAlertLimit(); i++) {
            boolean result = rateLimiter.shouldSendAlert("SAME_ERROR", AlertLevel.INFO);
            assertThat(result).isTrue(); // 초기 요청들은 허용되어야 함
        }

        // then: 같은 윈도우 내에서 나중에 온 요청은 거부됨
        when(clock.millis()).thenReturn(windowMiddle);
        boolean lateUserResult = rateLimiter.shouldSendAlert("SAME_ERROR", AlertLevel.INFO);
        assertThat(lateUserResult).isFalse(); // 이제 실패해야 함
    }

    @Test
    @DisplayName("알림 중요도별 제한 - 중요한 알림은 많이, 일반 알림은 적게 보낼 수 있다")
    void shouldApplyDifferentLimitsPerAlertLevel() {
        // given
        long fixedTime = 1000000000L;
        when(clock.millis()).thenReturn(fixedTime);

        // when & then: CRITICAL (10개)
        for (int i = 0; i < AlertLevel.CRITICAL.getAlertLimit(); i++) {
            assertThat(rateLimiter.shouldSendAlert("ERROR", AlertLevel.CRITICAL)).isTrue();
        }
        assertThat(rateLimiter.shouldSendAlert("ERROR", AlertLevel.CRITICAL)).isFalse();

        // when & then: INFO (1개) - 다른 에러코드 사용
        assertThat(rateLimiter.shouldSendAlert("INFO_ERROR", AlertLevel.INFO)).isTrue();
        assertThat(rateLimiter.shouldSendAlert("INFO_ERROR", AlertLevel.INFO)).isFalse();
    }

    @Test
    @DisplayName("동시 접근 처리 - 여러 요청이 동시에 와도 정확한 개수만 허용된다")
    void shouldHandleConcurrentRequests() throws InterruptedException {
        // given
        long fixedTime = 1000000000L;
        when(clock.millis()).thenReturn(fixedTime);

        int threadCount = 20;
        int requestsPerThread = 1;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        // when: 20개 스레드에서 동시에 요청
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < requestsPerThread; j++) {
                        if (rateLimiter.shouldSendAlert("CONCURRENT_TEST", AlertLevel.CRITICAL)) {
                            successCount.incrementAndGet();
                        }
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
    @DisplayName("시간 정규화 - 같은 시간 구간의 다른 시점들이 올바르게 그룹핑된다")
    void shouldNormalizeWindowCorrectly() {
        // given - 다양한 시간 포인트들
        long[] testTimes = {
            1000000000L,  // 정확히 윈도우 시작
            1000030000L,  // 윈도우 중간
            1000059999L   // 윈도우 끝
        };

        // when & then
        for (long testTime : testTimes) {
            when(clock.millis()).thenReturn(testTime);
            rateLimiter.shouldSendAlert("NORMALIZE_TEST", AlertLevel.INFO);
        }

        // 모든 요청이 같은 윈도우에서 카운팅되어 2번째부터는 거부되어야 함
        when(clock.millis()).thenReturn(1000059999L);
        boolean shouldReject = rateLimiter.shouldSendAlert("NORMALIZE_TEST", AlertLevel.INFO);
        assertThat(shouldReject).isFalse();
    }

    @Test
    @DisplayName("갑작스런 허용 현상 - 시간 구간이 바뀌면 갑자기 모든 요청이 허용된다")
    void shouldDemonstrateResetPhenomenon() {
        // given
        long firstWindow = 1000000000L;
        long secondWindow = 1000060000L; // 정확히 1분 후

        // when: 첫 윈도우에서 제한까지 사용
        when(clock.millis()).thenReturn(firstWindow);
        for (int i = 0; i < AlertLevel.INFO.getAlertLimit(); i++) {
            rateLimiter.shouldSendAlert("RESET_TEST", AlertLevel.INFO);
        }

        // 제한 초과 확인
        boolean exceededInFirstWindow = rateLimiter.shouldSendAlert("RESET_TEST", AlertLevel.INFO);
        assertThat(exceededInFirstWindow).isFalse();

        // then: 새 윈도우에서는  모든 요청이 허용됨
        when(clock.millis()).thenReturn(secondWindow);
        boolean allowedInSecondWindow = rateLimiter.shouldSendAlert("RESET_TEST", AlertLevel.INFO);
        assertThat(allowedInSecondWindow).isTrue();
    }

    @Test
    @DisplayName("오래된 데이터 정리 - 시간이 지난 알림 기록들을 자동으로 삭제한다")
    void shouldCleanUpExpiredEntries() {
        // given
        long oldTime = 1000000000L;
        long newTime = oldTime + (CLEANUP_LOOKBACK_MILLIS + 1000);

        // when: 오래된 시간에 요청 생성
        when(clock.millis()).thenReturn(oldTime);
        rateLimiter.shouldSendAlert("OLD_REQUEST", AlertLevel.CRITICAL);

        // then: cleanup 후에는 제거되어야 함
        when(clock.millis()).thenReturn(newTime);
        rateLimiter.cleanUpAlerts();

        // cleanup 후 새 요청이 허용되는지 확인 (내부 맵이 정리되었다는 의미)
        boolean newRequestAllowed = rateLimiter.shouldSendAlert("OLD_REQUEST", AlertLevel.CRITICAL);
        assertThat(newRequestAllowed).isTrue();
    }
}
