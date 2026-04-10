package com.eventitta.domain.gamification.evaluator;

import com.eventitta.domain.gamification.domain.ActivityType;
import com.eventitta.domain.gamification.domain.BadgeRule;
import com.eventitta.domain.gamification.domain.EvaluationType;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ActivityCountRuleEvaluatorTest {

    private ActivityCountRuleEvaluator evaluator;

    @BeforeEach
    void setUp() {
        evaluator = new ActivityCountRuleEvaluator();
    }

    @Test
    @DisplayName("COUNT 타입과 활동 타입이 있으면 지원한다")
    void supports_WithCountTypeAndActivityType_ReturnsTrue() {
        BadgeRule rule = BadgeRule.builder()
            .activityType(ActivityType.CREATE_POST)
            .evaluationType(EvaluationType.COUNT)
            .threshold(5)
            .build();

        assertThat(evaluator.supports(rule)).isTrue();
    }

    @Test
    @DisplayName("COUNT 타입이 아니면 지원하지 않는다")
    void supports_WithNonCountType_ReturnsFalse() {
        BadgeRule rule = BadgeRule.builder()
            .activityType(ActivityType.CREATE_POST)
            .evaluationType(EvaluationType.POINTS)
            .threshold(5)
            .build();

        assertThat(evaluator.supports(rule)).isFalse();
    }

    @Test
    @DisplayName("활동 횟수가 임계치 이상이면 만족한다")
    void isSatisfied_WhenCountMeetsThreshold_ReturnsTrue() {
        BadgeRule rule = BadgeRule.builder()
            .activityType(ActivityType.CREATE_POST)
            .evaluationType(EvaluationType.COUNT)
            .threshold(5)
            .build();
        Map<ActivityType, Long> activityCountMap = new HashMap<>();
        activityCountMap.put(ActivityType.CREATE_POST, 5L);

        assertThat(evaluator.isSatisfied(rule, activityCountMap, new HashMap<>())).isTrue();
    }

    @Test
    @DisplayName("활동 데이터가 없으면 0으로 처리한다")
    void isSatisfied_WhenNoActivityData_TreatsAsZero() {
        BadgeRule rule = BadgeRule.builder()
            .activityType(ActivityType.CREATE_POST)
            .evaluationType(EvaluationType.COUNT)
            .threshold(1)
            .build();

        assertThat(evaluator.isSatisfied(rule, new HashMap<>(), new HashMap<>())).isFalse();
    }
}
