package com.eventitta.gamification.evaluator;

import com.eventitta.gamification.domain.ActivityType;
import com.eventitta.gamification.domain.BadgeRule;
import com.eventitta.gamification.domain.EvaluationType;
import com.eventitta.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ActivityCountRuleEvaluatorTest {

    private ActivityCountRuleEvaluator evaluator;
    private User testUser;

    @BeforeEach
    void setUp() {
        evaluator = new ActivityCountRuleEvaluator();
        testUser = User.builder()
                .email("test@test.com")
                .nickname("testUser")
                .password("password")
                .build();
    }

    @Test
    @DisplayName("COUNT 타입과 활동 타입이 있으면 지원한다")
    void supports_WithCountTypeAndActivityType_ReturnsTrue() {
        // given
        BadgeRule rule = BadgeRule.builder()
                .activityType(ActivityType.CREATE_POST)
                .evaluationType(EvaluationType.COUNT)
                .threshold(5)
                .build();

        // when
        boolean result = evaluator.supports(rule);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("COUNT 타입이 아니면 지원하지 않는다")
    void supports_WithNonCountType_ReturnsFalse() {
        // given
        BadgeRule rule = BadgeRule.builder()
                .activityType(ActivityType.CREATE_POST)
                .evaluationType(EvaluationType.POINTS)
                .threshold(5)
                .build();

        // when
        boolean result = evaluator.supports(rule);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("활동 타입이 없으면 지원하지 않는다")
    void supports_WithoutActivityType_ReturnsFalse() {
        // given
        BadgeRule rule = BadgeRule.builder()
                .activityType(null)
                .evaluationType(EvaluationType.COUNT)
                .threshold(5)
                .build();

        // when
        boolean result = evaluator.supports(rule);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("활동 횟수가 임계치 이상이면 만족한다")
    void isSatisfied_WhenCountMeetsThreshold_ReturnsTrue() {
        // given
        BadgeRule rule = BadgeRule.builder()
                .activityType(ActivityType.CREATE_POST)
                .evaluationType(EvaluationType.COUNT)
                .threshold(5)
                .build();

        Map<ActivityType, Long> activityCountMap = new HashMap<>();
        activityCountMap.put(ActivityType.CREATE_POST, 5L);

        // when
        boolean result = evaluator.isSatisfied(testUser, rule, activityCountMap, new HashMap<>());

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("활동 횟수가 임계치를 초과하면 만족한다")
    void isSatisfied_WhenCountExceedsThreshold_ReturnsTrue() {
        // given
        BadgeRule rule = BadgeRule.builder()
                .activityType(ActivityType.CREATE_COMMENT)
                .evaluationType(EvaluationType.COUNT)
                .threshold(10)
                .build();

        Map<ActivityType, Long> activityCountMap = new HashMap<>();
        activityCountMap.put(ActivityType.CREATE_COMMENT, 15L);

        // when
        boolean result = evaluator.isSatisfied(testUser, rule, activityCountMap, new HashMap<>());

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("활동 횟수가 임계치 미만이면 만족하지 않는다")
    void isSatisfied_WhenCountBelowThreshold_ReturnsFalse() {
        // given
        BadgeRule rule = BadgeRule.builder()
                .activityType(ActivityType.JOIN_MEETING)
                .evaluationType(EvaluationType.COUNT)
                .threshold(3)
                .build();

        Map<ActivityType, Long> activityCountMap = new HashMap<>();
        activityCountMap.put(ActivityType.JOIN_MEETING, 2L);

        // when
        boolean result = evaluator.isSatisfied(testUser, rule, activityCountMap, new HashMap<>());

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("활동 데이터가 없으면 0으로 처리한다")
    void isSatisfied_WhenNoActivityData_TreatsAsZero() {
        // given
        BadgeRule rule = BadgeRule.builder()
                .activityType(ActivityType.CREATE_POST)
                .evaluationType(EvaluationType.COUNT)
                .threshold(1)
                .build();

        Map<ActivityType, Long> activityCountMap = new HashMap<>();
        // CREATE_POST에 대한 데이터 없음

        // when
        boolean result = evaluator.isSatisfied(testUser, rule, activityCountMap, new HashMap<>());

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("임계치가 0이면 항상 만족한다")
    void isSatisfied_WhenThresholdIsZero_AlwaysReturnsTrue() {
        // given
        BadgeRule rule = BadgeRule.builder()
                .activityType(ActivityType.CREATE_POST)
                .evaluationType(EvaluationType.COUNT)
                .threshold(0)
                .build();

        Map<ActivityType, Long> activityCountMap = new HashMap<>();
        // 데이터 없어도 0 >= 0이므로 true

        // when
        boolean result = evaluator.isSatisfied(testUser, rule, activityCountMap, new HashMap<>());

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("다양한 활동 타입 중 정확한 타입만 평가한다")
    void isSatisfied_EvaluatesCorrectActivityType() {
        // given
        BadgeRule rule = BadgeRule.builder()
                .activityType(ActivityType.CREATE_COMMENT)
                .evaluationType(EvaluationType.COUNT)
                .threshold(5)
                .build();

        Map<ActivityType, Long> activityCountMap = new HashMap<>();
        activityCountMap.put(ActivityType.CREATE_POST, 10L);  // 다른 타입
        activityCountMap.put(ActivityType.CREATE_COMMENT, 6L);  // 평가 대상
        activityCountMap.put(ActivityType.JOIN_MEETING, 3L);  // 다른 타입

        // when
        boolean result = evaluator.isSatisfied(testUser, rule, activityCountMap, new HashMap<>());

        // then
        assertThat(result).isTrue();  // CREATE_COMMENT의 6이 임계치 5 이상
    }
}
