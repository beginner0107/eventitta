package com.eventitta.common.notification.service;

import com.eventitta.common.notification.domain.AlertLevel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("알림 발송 제한 시스템의 멀티 서버 환경 문제 검증")
class SimpleRateLimiterScaleOutTest {

    private static final ZoneId ZONE_UTC = ZoneId.of("UTC");
    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2025-01-01T00:00:00Z"), ZONE_UTC);

    private static final int INFO_LIMIT = 1;
    private static final int MEDIUM_LIMIT = 2;
    private static final int HIGH_LIMIT = 5;
    private static final int CRITICAL_LIMIT = 10;

    private static SimpleRateLimiter createServerInstance() {
        return new SimpleRateLimiter(FIXED_CLOCK);
    }


    @DisplayName("두 서버 환경에서 알림이 의도한 제한의 2배로 발송되는 문제")
    @Test
    void twoServers_InfoLevel_doublesLimit() {
        // given
        SimpleRateLimiter server1 = createServerInstance();
        SimpleRateLimiter server2 = createServerInstance();
        String errorCode = "DUPLICATE_ERROR";
        AlertLevel alertLevel = AlertLevel.INFO;

        // when
        int allowedByServer1First = server1.shouldSendAlert(errorCode, alertLevel) ? 1 : 0;
        int allowedByServer1Second = server1.shouldSendAlert(errorCode, alertLevel) ? 1 : 0;
        int allowedByServer2First = server2.shouldSendAlert(errorCode, alertLevel) ? 1 : 0;
        int allowedByServer2Second = server2.shouldSendAlert(errorCode, alertLevel) ? 1 : 0;

        int totalAllowed = allowedByServer1First + allowedByServer1Second + allowedByServer2First + allowedByServer2Second;

        // then
        assertThat(totalAllowed).isEqualTo(2);
    }

    @DisplayName("서버별 트래픽 분산이 되었을 때 알림 제한이 적용되지 않는 문제")
    @Test
    void unbalancedLoad_increasesTotalBeyondGlobalLimit() {
        // given
        SimpleRateLimiter heavyTrafficServer = createServerInstance();
        SimpleRateLimiter lightTrafficServer = createServerInstance();
        String errorCode = "API_TIMEOUT";
        AlertLevel alertLevel = AlertLevel.HIGH;

        // when
        int heavyServerAllowed = sendAttempts(heavyTrafficServer, errorCode, alertLevel, 8);
        int lightServerAllowed = sendAttempts(lightTrafficServer, errorCode, alertLevel, 2);
        int totalAllowed = heavyServerAllowed + lightServerAllowed;

        // then
        assertThat(totalAllowed).isEqualTo(7);

        // 각 서버의 현재 상태 검증
        assertThat(heavyTrafficServer.shouldSendAlert(errorCode, alertLevel)).isFalse();
        assertThat(lightTrafficServer.shouldSendAlert(errorCode, alertLevel)).isTrue();
    }

    @DisplayName("서버 재시작으로 알림 제한 상태가 리셋되어 중복 발송되는 문제")
    @Test
    void serverRestart_resetsState_andAllowsAgain() {
        // given
        SimpleRateLimiter runningServer = createServerInstance();
        String errorCode = "RESTART_ERROR";
        AlertLevel alertLevel = AlertLevel.INFO;

        // when
        int allowedBeforeRestart = sendAttempts(runningServer, errorCode, alertLevel, 2);

        SimpleRateLimiter restartedServer = createServerInstance();
        int allowedAfterRestart = sendAttempts(restartedServer, errorCode, alertLevel, 1);

        int totalAllowed = allowedBeforeRestart + allowedAfterRestart;

        // then
        assertThat(allowedBeforeRestart).isEqualTo(1);
        assertThat(allowedAfterRestart).isEqualTo(1);
        assertThat(totalAllowed).isEqualTo(2);
    }

    @DisplayName("동시 요청 시 서버별로만 제한이 적용되어 전체 발송량이 2배가 되는 문제")
    @Test
    void concurrentBursts_onTwoServers_doubleTheGlobalLimit() throws InterruptedException {
        // given
        SimpleRateLimiter server1 = createServerInstance();
        SimpleRateLimiter server2 = createServerInstance();
        String errorCode = "CONCURRENT_ERROR";
        AlertLevel alertLevel = AlertLevel.MEDIUM;
        int requestsPerServer = 10;

        ExecutorService threadPool = Executors.newFixedThreadPool(2);
        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch completionSignal = new CountDownLatch(2);
        AtomicInteger totalAllowedCount = new AtomicInteger(0);

        // when
        threadPool.submit(() -> runBurst(server1, errorCode, alertLevel, requestsPerServer,
            startSignal, completionSignal, totalAllowedCount));
        threadPool.submit(() -> runBurst(server2, errorCode, alertLevel, requestsPerServer,
            startSignal, completionSignal, totalAllowedCount));

        startSignal.countDown();
        completionSignal.await();
        threadPool.shutdown();

        // then
        assertThat(totalAllowedCount.get()).isEqualTo(4);
    }

    private static void runBurst(SimpleRateLimiter limiter,
                                 String errorCode,
                                 AlertLevel level,
                                 int attempts,
                                 CountDownLatch start,
                                 CountDownLatch done,
                                 AtomicInteger totalAllowed) {
        try {
            start.await();
            for (int i = 0; i < attempts; i++) {
                if (limiter.shouldSendAlert(errorCode, level)) {
                    totalAllowed.incrementAndGet();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            done.countDown();
        }
    }

    private static int sendAttempts(SimpleRateLimiter limiter, String errorCode, AlertLevel level, int attempts) {
        int allowed = 0;
        for (int i = 0; i < attempts; i++) {
            if (limiter.shouldSendAlert(errorCode, level)) {
                allowed++;
            }
        }
        return allowed;
    }
}
