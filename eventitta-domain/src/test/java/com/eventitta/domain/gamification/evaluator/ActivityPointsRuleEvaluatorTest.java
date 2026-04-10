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

class ActivityPointsRuleEvaluatorTest {

    private ActivityPointsRuleEvaluator evaluator;

    @BeforeEach
    void setUp() {
        evaluator = new ActivityPointsRuleEvaluator();
    }

    @Test
    @DisplayName("POINTS 타입과 활동 타입이 있으면 지원한다")
    void supports_WithPointsTypeAndActivityType_ReturnsTrue() {
        BadgeRule rule = BadgeRule.builder()
            .activityType(ActivityType.CREATE_POST)
            .evaluationType(EvaluationType.POINTS)
            .threshold(100)
            .build();

        assertThat(evaluator.supports(rule)).isTrue();
    }

    @Test
    @DisplayName("POINTS 타입이 아니면 지원하지 않는다")
    void supports_WithNonPointsType_ReturnsFalse() {
        BadgeRule rule = BadgeRule.builder()
            .activityType(ActivityType.CREATE_POST)
            .evaluationType(EvaluationType.COUNT)
            .threshold(100)
            .build();

        assertThat(evaluator.supports(rule)).isFalse();
    }

    @Test
    @DisplayName("포인트가 임계치 이상이면 만족한다")
    void isSatisfied_WhenPointsMeetThreshold_ReturnsTrue() {
        BadgeRule rule = BadgeRule.builder()
            .activityType(ActivityType.CREATE_POST)
            .evaluationType(EvaluationType.POINTS)
            .threshold(50)
            .build();
        Map<ActivityType, Long> activityPointsMap = new HashMap<>();
        activityPointsMap.put(ActivityType.CREATE_POST, 50L);

        assertThat(evaluator.isSatisfied(rule, new HashMap<>(), activityPointsMap)).isTrue();
    }

    @Test
    @DisplayName("활동 카운트 맵이 아닌 포인트 맵을 사용한다")
    void isSatisfied_UsesPointsMapNotCountMap() {
        BadgeRule rule = BadgeRule.builder()
            .activityType(ActivityType.CREATE_POST)
            .evaluationType(EvaluationType.POINTS)
            .threshold(50)
            .build();
        Map<ActivityType, Long> activityCountMap = new HashMap<>();
        activityCountMap.put(ActivityType.CREATE_POST, 100L);
        Map<ActivityType, Long> activityPointsMap = new HashMap<>();
        activityPointsMap.put(ActivityType.CREATE_POST, 30L);

        assertThat(evaluator.isSatisfied(rule, activityCountMap, activityPointsMap)).isFalse();
    }
}
