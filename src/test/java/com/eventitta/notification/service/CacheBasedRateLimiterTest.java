package com.eventitta.notification.service;

import com.eventitta.notification.domain.AlertLevel;
import com.eventitta.notification.service.ratelimit.CacheBasedRateLimiter;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class CacheBasedRateLimiterTest {

    private CacheBasedRateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        rateLimiter = new CacheBasedRateLimiter();
    }

    @Test
    @DisplayName("첫 번째 알림 요청은 항상 허용되어야 한다")
    void shouldAllowFirstAlert() {
        // given

        // when
        boolean result = rateLimiter.shouldSendAlert("ERROR_001", AlertLevel.HIGH);

        // then
        assertThat(result).isTrue();
    }

    @ParameterizedTest
    @EnumSource(AlertLevel.class)
    @DisplayName("각 레벨별 제한값이 올바르게 적용되어야 한다")
    void shouldApplyCorrectLimitsForEachLevel(AlertLevel level) {
        // given
        String errorCode = "PARAM_TEST_" + level; // 레벨별로 다른 키 사용
        int expectedLimit = level.getAlertLimit();

        // when & then: 제한까지 허용
        for (int i = 0; i < expectedLimit; i++) {
            // when
            boolean allowed = rateLimiter.shouldSendAlert(errorCode, level);
            // then
            assertThat(allowed).isTrue();
        }

        // when: 초과 요청
        boolean afterLimit = rateLimiter.shouldSendAlert(errorCode, level);
        // then: 초과시 거부
        assertThat(afterLimit).isFalse();
    }


    @Test
    @DisplayName("다른 에러 코드는 독립적으로 카운트되어야 한다")
    void shouldCountDifferentErrorCodesIndependently() {
        // given
        AlertLevel level = AlertLevel.HIGH;

        // when
        boolean result1 = rateLimiter.shouldSendAlert("ERROR_001", level);
        boolean result2 = rateLimiter.shouldSendAlert("ERROR_002", level);

        // then
        assertThat(result1).isTrue();
        assertThat(result2).isTrue();

        assertThat(rateLimiter.getCacheSize()).isEqualTo(2);
    }

    @Test
    @DisplayName("같은 에러 코드라도 다른 레벨은 독립적으로 카운트되어야 한다")
    void shouldCountDifferentLevelsIndependently() {
        // given
        String errorCode = "ERROR_001";

        // when
        boolean highResult = rateLimiter.shouldSendAlert(errorCode, AlertLevel.HIGH);
        boolean mediumResult = rateLimiter.shouldSendAlert(errorCode, AlertLevel.MEDIUM);

        // then
        assertThat(highResult).isTrue();
        assertThat(mediumResult).isTrue();

        assertThat(rateLimiter.getCacheSize()).isEqualTo(2);
    }

    @Test
    @DisplayName("reset() 호출 시 모든 카운트가 초기화되어야 한다")
    void shouldResetAllCountsWhenReset() {
        // given
        rateLimiter.shouldSendAlert("ERROR_001", AlertLevel.HIGH);
        rateLimiter.shouldSendAlert("ERROR_002", AlertLevel.MEDIUM);

        assertThat(rateLimiter.getCacheSize()).isEqualTo(2);

        // when
        rateLimiter.reset();

        // then
        assertThat(rateLimiter.getCacheSize()).isEqualTo(0);

        // when
        boolean result = rateLimiter.shouldSendAlert("ERROR_001", AlertLevel.HIGH);
        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("캐시 통계를 조회할 수 있어야 한다")
    void shouldProvidesCacheStats() {
        // given
        rateLimiter.shouldSendAlert("ERROR_001", AlertLevel.HIGH);
        rateLimiter.shouldSendAlert("ERROR_001", AlertLevel.HIGH);

        // when
        var stats = rateLimiter.getCacheStats();

        // then
        assertThat(stats.requestCount()).isEqualTo(2);
        assertThat(stats.hitCount()).isEqualTo(1);
        assertThat(stats.missCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("연속된 요청이 올바르게 제한되어야 한다")
    void shouldLimitConsecutiveRequests() {
        // given
        String errorCode = "SEQUENTIAL_TEST";
        AlertLevel level = AlertLevel.MEDIUM;

        // when & then: 첫 2개 요청은 허용
        assertThat(rateLimiter.shouldSendAlert(errorCode, level)).isTrue();
        assertThat(rateLimiter.shouldSendAlert(errorCode, level)).isTrue();

        // when & then: 그 이후 요청들은 모두 거부
        for (int i = 0; i < 5; i++) {
            // when
            boolean blocked = rateLimiter.shouldSendAlert(errorCode, level);
            // then
            assertThat(blocked).isFalse();
        }
    }

    @Test
    @DisplayName("수동 정리는 만료되지 않은 엔트리에 영향을 주지 않는다")
        // 설명: cleanUp() 호출이 만료되지 않은 항목에는 영향을 주지 않는지 확인합니다.
    void shouldNotAffectNonExpiredEntriesAfterCleanup() {
        // given
        assertThat(rateLimiter.shouldSendAlert("ERROR_001", AlertLevel.INFO)).isTrue();
        assertThat(rateLimiter.shouldSendAlert("ERROR_001", AlertLevel.INFO)).isFalse();
        assertThat(rateLimiter.getCacheSize()).isEqualTo(1);

        // when
        rateLimiter.cleanUp();

        // then
        assertThat(rateLimiter.shouldSendAlert("ERROR_001", AlertLevel.INFO)).isFalse();
        assertThat(rateLimiter.getCacheSize()).isEqualTo(1);
    }

    @Test
    @DisplayName("캐시 히트율이 올바르게 계산되어야 한다")
    void shouldCalculateHitRateCorrectly() {
        // given
        String errorCode = "STATS_TEST";
        AlertLevel level = AlertLevel.HIGH;

        // when: 첫 번째: miss (캐시에 없음)
        rateLimiter.shouldSendAlert(errorCode, level);

        // when: 두 번째: hit (캐시에 있음)
        rateLimiter.shouldSendAlert(errorCode, level);

        // then
        var stats = rateLimiter.getCacheStats();
        assertThat(stats.missCount()).isEqualTo(1);
        assertThat(stats.hitCount()).isEqualTo(1);
        assertThat(stats.hitRate()).isEqualTo(0.5, Offset.offset(0.01));
    }

    @Test
    @DisplayName("빈 문자열 에러코드 처리")
    void shouldHandleEmptyErrorCode() {
        // given

        // when
        boolean result = rateLimiter.shouldSendAlert("", AlertLevel.INFO);
        // then
        assertThat(result).isTrue();

        // when
        boolean secondResult = rateLimiter.shouldSendAlert("", AlertLevel.INFO);
        // then
        assertThat(secondResult).isFalse();
    }

}
