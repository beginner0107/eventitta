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

class ActivityPointsRuleEvaluatorTest {

    private ActivityPointsRuleEvaluator evaluator;
    private User testUser;

    @BeforeEach
    void setUp() {
        evaluator = new ActivityPointsRuleEvaluator();
        testUser = User.builder()
                .email("test@test.com")
                .nickname("testUser")
                .password("password")
                .build();
    }

    @Test
    @DisplayName("POINTS 타입과 활동 타입이 있으면 지원한다")
    void supports_WithPointsTypeAndActivityType_ReturnsTrue() {
        // given
        BadgeRule rule = BadgeRule.builder()
                .activityType(ActivityType.CREATE_POST)
                .evaluationType(EvaluationType.POINTS)
                .threshold(100)
                .build();

        // when
        boolean result = evaluator.supports(rule);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("POINTS 타입이 아니면 지원하지 않는다")
    void supports_WithNonPointsType_ReturnsFalse() {
        // given
        BadgeRule rule = BadgeRule.builder()
                .activityType(ActivityType.CREATE_POST)
                .evaluationType(EvaluationType.COUNT)
                .threshold(100)
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
                .evaluationType(EvaluationType.POINTS)
                .threshold(100)
                .build();

        // when
        boolean result = evaluator.supports(rule);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("포인트가 임계치 이상이면 만족한다")
    void isSatisfied_WhenPointsMeetThreshold_ReturnsTrue() {
        // given
        BadgeRule rule = BadgeRule.builder()
                .activityType(ActivityType.CREATE_POST)
                .evaluationType(EvaluationType.POINTS)
                .threshold(50)
                .build();

        Map<ActivityType, Long> activityPointsMap = new HashMap<>();
        activityPointsMap.put(ActivityType.CREATE_POST, 50L);

        // when
        boolean result = evaluator.isSatisfied(testUser, rule, new HashMap<>(), activityPointsMap);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("포인트가 임계치를 초과하면 만족한다")
    void isSatisfied_WhenPointsExceedThreshold_ReturnsTrue() {
        // given
        BadgeRule rule = BadgeRule.builder()
                .activityType(ActivityType.CREATE_COMMENT)
                .evaluationType(EvaluationType.POINTS)
                .threshold(100)
                .build();

        Map<ActivityType, Long> activityPointsMap = new HashMap<>();
        activityPointsMap.put(ActivityType.CREATE_COMMENT, 150L);

        // when
        boolean result = evaluator.isSatisfied(testUser, rule, new HashMap<>(), activityPointsMap);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("포인트가 임계치 미만이면 만족하지 않는다")
    void isSatisfied_WhenPointsBelowThreshold_ReturnsFalse() {
        // given
        BadgeRule rule = BadgeRule.builder()
                .activityType(ActivityType.JOIN_MEETING)
                .evaluationType(EvaluationType.POINTS)
                .threshold(200)
                .build();

        Map<ActivityType, Long> activityPointsMap = new HashMap<>();
        activityPointsMap.put(ActivityType.JOIN_MEETING, 150L);

        // when
        boolean result = evaluator.isSatisfied(testUser, rule, new HashMap<>(), activityPointsMap);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("포인트 데이터가 없으면 0으로 처리한다")
    void isSatisfied_WhenNoPointsData_TreatsAsZero() {
        // given
        BadgeRule rule = BadgeRule.builder()
                .activityType(ActivityType.CREATE_POST)
                .evaluationType(EvaluationType.POINTS)
                .threshold(10)
                .build();

        Map<ActivityType, Long> activityPointsMap = new HashMap<>();
        // CREATE_POST에 대한 포인트 데이터 없음

        // when
        boolean result = evaluator.isSatisfied(testUser, rule, new HashMap<>(), activityPointsMap);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("임계치가 0이면 항상 만족한다")
    void isSatisfied_WhenThresholdIsZero_AlwaysReturnsTrue() {
        // given
        BadgeRule rule = BadgeRule.builder()
                .activityType(ActivityType.CREATE_POST)
                .evaluationType(EvaluationType.POINTS)
                .threshold(0)
                .build();

        Map<ActivityType, Long> activityPointsMap = new HashMap<>();
        // 데이터 없어도 0 >= 0이므로 true

        // when
        boolean result = evaluator.isSatisfied(testUser, rule, new HashMap<>(), activityPointsMap);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("다양한 활동 타입 중 정확한 타입만 평가한다")
    void isSatisfied_EvaluatesCorrectActivityType() {
        // given
        BadgeRule rule = BadgeRule.builder()
                .activityType(ActivityType.CREATE_COMMENT)
                .evaluationType(EvaluationType.POINTS)
                .threshold(75)
                .build();

        Map<ActivityType, Long> activityPointsMap = new HashMap<>();
        activityPointsMap.put(ActivityType.CREATE_POST, 200L);  // 다른 타입
        activityPointsMap.put(ActivityType.CREATE_COMMENT, 100L);  // 평가 대상
        activityPointsMap.put(ActivityType.JOIN_MEETING, 50L);  // 다른 타입

        // when
        boolean result = evaluator.isSatisfied(testUser, rule, new HashMap<>(), activityPointsMap);

        // then
        assertThat(result).isTrue();  // CREATE_COMMENT의 100이 임계치 75 이상
    }

    @Test
    @DisplayName("활동 카운트 맵이 아닌 포인트 맵을 사용한다")
    void isSatisfied_UsesPointsMapNotCountMap() {
        // given
        BadgeRule rule = BadgeRule.builder()
                .activityType(ActivityType.CREATE_POST)
                .evaluationType(EvaluationType.POINTS)
                .threshold(50)
                .build();

        Map<ActivityType, Long> activityCountMap = new HashMap<>();
        activityCountMap.put(ActivityType.CREATE_POST, 100L);  // Count는 100

        Map<ActivityType, Long> activityPointsMap = new HashMap<>();
        activityPointsMap.put(ActivityType.CREATE_POST, 30L);  // Points는 30

        // when
        boolean result = evaluator.isSatisfied(testUser, rule, activityCountMap, activityPointsMap);

        // then
        assertThat(result).isFalse();  // 포인트 30이 임계치 50 미만이므로 false
    }
}
