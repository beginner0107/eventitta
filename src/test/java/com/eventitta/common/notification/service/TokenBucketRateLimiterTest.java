package com.eventitta.common.notification.service;

import com.eventitta.common.notification.domain.AlertLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenBucketRateLimiterTest {

    private TokenBucketRateLimiter rateLimiter;
    private Clock mockClock;
    private long currentTimeMillis;

    @BeforeEach
    void setUp() {
        mockClock = mock(Clock.class);
        currentTimeMillis = System.currentTimeMillis();
        when(mockClock.millis()).thenReturn(currentTimeMillis);
        rateLimiter = new TokenBucketRateLimiter(mockClock);
    }

    @Test
    @DisplayName("처음 요청 시 모든 AlertLevel에 대해 알림 허용")
    void shouldAllowFirstAlert() {
        // given
        String errorCode = "ERROR_001";

        // when & then
        assertThat(rateLimiter.shouldSendAlert(errorCode, AlertLevel.CRITICAL)).isTrue();
        assertThat(rateLimiter.shouldSendAlert(errorCode, AlertLevel.HIGH)).isTrue();
        assertThat(rateLimiter.shouldSendAlert(errorCode, AlertLevel.MEDIUM)).isTrue();
        assertThat(rateLimiter.shouldSendAlert(errorCode, AlertLevel.INFO)).isTrue();
    }

    @Test
    @DisplayName("CRITICAL 레벨 토큰 소모 및 충전 확인 (10개 제한)")
    void shouldManageCriticalLevelTokens() {
        // given
        String errorCode = "CRITICAL_ERROR";
        AlertLevel level = AlertLevel.CRITICAL;

        // when: 최대 허용량까지 요청
        for (int i = 0; i < AlertLevel.CRITICAL.getAlertLimit(); i++) {
            assertThat(rateLimiter.shouldSendAlert(errorCode, level)).isTrue();
        }

        // then: 추가 요청은 거부
        assertThat(rateLimiter.shouldSendAlert(errorCode, level)).isFalse();
    }

    @Test
    @DisplayName("시간 경과에 따른 토큰 충전 확인")
    void shouldRefillTokensOverTime() {
        // given
        String errorCode = "TIME_TEST";
        AlertLevel level = AlertLevel.HIGH;

        // when: 모든 토큰 소모
        for (int i = 0; i < AlertLevel.HIGH.getAlertLimit(); i++) {
            rateLimiter.shouldSendAlert(errorCode, level);
        }
        assertThat(rateLimiter.shouldSendAlert(errorCode, level)).isFalse();

        // 1분 경과 시뮬레이션 (5개 용량, 5분 주기 = 1개/분)
        long oneMinuteLater = currentTimeMillis + TimeUnit.MINUTES.toMillis(1);
        when(mockClock.millis()).thenReturn(oneMinuteLater);

        // then: 토큰이 충전되어 요청 허용
        assertThat(rateLimiter.shouldSendAlert(errorCode, level))
            .isTrue();
    }

    @Test
    @DisplayName("각 AlertLevel별 정확한 제한값 확인")
    void shouldRespectExactLimitsForEachLevel() {
        String errorCode = "LIMIT_TEST";

        // CRITICAL: 10개
        testExactLimit(errorCode, AlertLevel.CRITICAL, AlertLevel.CRITICAL.getAlertLimit());

        // HIGH: 5개
        testExactLimit(errorCode + "_HIGH", AlertLevel.HIGH, AlertLevel.HIGH.getAlertLimit());

        // MEDIUM: 2개
        testExactLimit(errorCode + "_MEDIUM", AlertLevel.MEDIUM, AlertLevel.MEDIUM.getAlertLimit());

        // INFO: 1개
        testExactLimit(errorCode + "_INFO", AlertLevel.INFO, AlertLevel.INFO.getAlertLimit());
    }

    private void testExactLimit(String errorCode, AlertLevel level, int expectedLimit) {
        // 정확히 제한값까지 허용
        for (int i = 0; i < expectedLimit; i++) {
            assertThat(rateLimiter.shouldSendAlert(errorCode, level)).isTrue();
        }

        // 제한값 초과 시 거부
        assertThat(rateLimiter.shouldSendAlert(errorCode, level)).isFalse();
    }

    @Test
    @DisplayName("동일한 errorCode, 다른 AlertLevel은 독립적으로 관리")
    void shouldManageIndependentlyByErrorCodeAndLevel() {
        // given
        String errorCode = "SAME_ERROR";

        // when: 같은 errorCode로 다른 레벨 요청
        assertThat(rateLimiter.shouldSendAlert(errorCode, AlertLevel.CRITICAL)).isTrue();
        assertThat(rateLimiter.shouldSendAlert(errorCode, AlertLevel.HIGH)).isTrue();
        assertThat(rateLimiter.shouldSendAlert(errorCode, AlertLevel.MEDIUM)).isTrue();
        assertThat(rateLimiter.shouldSendAlert(errorCode, AlertLevel.INFO)).isTrue();

        // then: 각각 독립적으로 관리되어야 함
        // INFO 제한 도달 후에도 다른 레벨은 영향받지 않음
        assertThat(rateLimiter.shouldSendAlert(errorCode, AlertLevel.INFO)).isFalse();
        assertThat(rateLimiter.shouldSendAlert(errorCode, AlertLevel.MEDIUM)).isTrue();
    }

    @Test
    @DisplayName("매우 긴 시간 경과 후 토큰이 최대값을 초과하지 않음")
    void shouldNotExceedMaxCapacityAfterLongTime() {
        // given
        String errorCode = "LONG_TIME_TEST";
        AlertLevel level = AlertLevel.MEDIUM;

        // when: 매우 긴 시간 경과 (10시간)
        long tenHoursLater = currentTimeMillis + TimeUnit.HOURS.toMillis(10);
        when(mockClock.millis()).thenReturn(tenHoursLater);

        // then: 최대 용량만큼만 사용 가능 (2개)
        assertThat(rateLimiter.shouldSendAlert(errorCode, level)).isTrue();
        assertThat(rateLimiter.shouldSendAlert(errorCode, level)).isTrue();
        assertThat(rateLimiter.shouldSendAlert(errorCode, level)).isFalse();
    }

    @Test
    @DisplayName("시간이 역행해도 토큰이 음수가 되지 않음")
    void shouldHandleTimeGoingBackwards() {
        // given
        String errorCode = "TIME_BACKWARDS";
        AlertLevel level = AlertLevel.INFO;

        // 토큰 소모
        rateLimiter.shouldSendAlert(errorCode, level);

        // when: 시간이 과거로 돌아감
        long pastTime = currentTimeMillis - TimeUnit.MINUTES.toMillis(5);
        when(mockClock.millis()).thenReturn(pastTime);

        // then: 추가 토큰 충전 없이 거부되어야 함
        assertThat(rateLimiter.shouldSendAlert(errorCode, level)).isFalse();
    }

    @Test
    @DisplayName("null errorCode 처리 - null도 유효한 키로 처리됨")
    void shouldHandleNullErrorCode() {
        // when & then: null도 문자열로 변환되어 처리됨
        assertThat(rateLimiter.shouldSendAlert(null, AlertLevel.INFO)).isTrue();
        assertThat(rateLimiter.shouldSendAlert(null, AlertLevel.INFO)).isFalse(); // INFO는 1개 제한
    }

    @Test
    @DisplayName("null AlertLevel 처리")
    void shouldHandleNullAlertLevel() {
        // when & then
        assertThatThrownBy(() -> rateLimiter.shouldSendAlert("ERROR", null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("빈 문자열 errorCode 처리")
    void shouldHandleEmptyErrorCode() {
        // when & then: 빈 문자열도 유효한 키로 처리되어야 함
        assertThat(rateLimiter.shouldSendAlert("", AlertLevel.INFO)).isTrue();
        assertThat(rateLimiter.shouldSendAlert("", AlertLevel.INFO)).isFalse();
    }

    @Test
    @DisplayName("공백 문자열 errorCode 처리")
    void shouldHandleWhitespaceErrorCode() {
        // when & then
        assertThat(rateLimiter.shouldSendAlert("   ", AlertLevel.INFO)).isTrue();
        assertThat(rateLimiter.shouldSendAlert("   ", AlertLevel.INFO)).isFalse();
    }

    @Test
    @DisplayName("동일한 키에 대한 동시 요청 처리")
    void shouldHandleConcurrentRequestsForSameKey() throws InterruptedException {
        // given
        String errorCode = "CONCURRENT_TEST";
        AlertLevel level = AlertLevel.MEDIUM; // 2개 제한
        int threadCount = 10;
        Thread[] threads = new Thread[threadCount];
        boolean[] results = new boolean[threadCount];

        // when: 여러 스레드가 동시에 요청
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            threads[i] = new Thread(() -> results[index] = rateLimiter.shouldSendAlert(errorCode, level));
        }

        for (Thread thread : threads) {
            thread.start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        // then: 정확히 2개만 허용되어야 함
        long allowedCount = 0;
        for (boolean result : results) {
            if (result) allowedCount++;
        }
        assertThat(allowedCount).isEqualTo(AlertLevel.MEDIUM.getAlertLimit());
    }

    @Test
    @DisplayName("reset 후 모든 제한이 초기화됨")
    void shouldClearAllLimitsAfterReset() {
        // given: 여러 키에 대해 제한 도달
        rateLimiter.shouldSendAlert("ERROR1", AlertLevel.INFO);
        rateLimiter.shouldSendAlert("ERROR2", AlertLevel.MEDIUM);
        rateLimiter.shouldSendAlert("ERROR2", AlertLevel.MEDIUM);

        // 제한 확인
        assertThat(rateLimiter.shouldSendAlert("ERROR1", AlertLevel.INFO)).isFalse();
        assertThat(rateLimiter.shouldSendAlert("ERROR2", AlertLevel.MEDIUM)).isFalse();

        // when: 리셋
        rateLimiter.reset();

        // then: 모든 제한이 초기화됨
        assertThat(rateLimiter.shouldSendAlert("ERROR1", AlertLevel.INFO)).isTrue();
        assertThat(rateLimiter.shouldSendAlert("ERROR2", AlertLevel.MEDIUM)).isTrue();
    }

    @Test
    @DisplayName("빈 상태에서 reset 호출해도 문제없음")
    void shouldHandleResetOnEmptyState() {
        // when & then: 예외가 발생하지 않아야 함
        rateLimiter.reset();

        // 정상 동작 확인
        assertThat(rateLimiter.shouldSendAlert("TEST", AlertLevel.INFO)).isTrue();
    }

    @Test
    @DisplayName("정확한 충전률로 토큰이 충전됨 (HIGH 레벨: 5개/5분 = 1개/분)")
    void shouldRefillAtCorrectRate() {
        // given
        String errorCode = "REFILL_TEST";
        AlertLevel level = AlertLevel.HIGH; // 5개 제한, 5분 주기

        // 모든 토큰 소모
        for (int i = 0; i < AlertLevel.HIGH.getAlertLimit(); i++) {
            rateLimiter.shouldSendAlert(errorCode, level);
        }
        assertThat(rateLimiter.shouldSendAlert(errorCode, level)).isFalse();

        // when: 30초 경과 (0.5분)
        long thirtySecondsLater = currentTimeMillis + TimeUnit.SECONDS.toMillis(30);
        when(mockClock.millis()).thenReturn(thirtySecondsLater);

        // then: 아직 토큰이 충전되지 않아야 함 (1분 단위로 충전)
        assertThat(rateLimiter.shouldSendAlert(errorCode, level)).isFalse();

        // when: 1분 경과
        long oneMinuteLater = currentTimeMillis + TimeUnit.MINUTES.toMillis(1);
        when(mockClock.millis()).thenReturn(oneMinuteLater);

        // then: 1개 토큰이 충전되어야 함
        assertThat(rateLimiter.shouldSendAlert(errorCode, level)).isTrue();
        assertThat(rateLimiter.shouldSendAlert(errorCode, level)).isFalse();
    }

    @Test
    @DisplayName("부분적 시간 경과에 대한 토큰 충전")
    void shouldHandlePartialTimeRefill() {
        // given
        String errorCode = "PARTIAL_REFILL";
        AlertLevel level = AlertLevel.CRITICAL;

        // 모든 토큰 소모
        for (int i = 0; i < AlertLevel.CRITICAL.getAlertLimit(); i++) {
            rateLimiter.shouldSendAlert(errorCode, level);
        }

        // when: 2.5분 경과 (5개 토큰 충전 예상)
        long twoAndHalfMinutes = currentTimeMillis + TimeUnit.SECONDS.toMillis(150);
        when(mockClock.millis()).thenReturn(twoAndHalfMinutes);

        // then: 5개 토큰이 충전되어야 함
        for (int i = 0; i < 5; i++) {
            assertThat(rateLimiter.shouldSendAlert(errorCode, level))
                .as("%d번째 토큰은 사용 가능해야 함", i + 1)
                .isTrue();
        }
        assertThat(rateLimiter.shouldSendAlert(errorCode, level)).isFalse();
    }
}
