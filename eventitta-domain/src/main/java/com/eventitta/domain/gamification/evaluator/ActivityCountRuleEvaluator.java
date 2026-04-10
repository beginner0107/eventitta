package com.eventitta.domain.gamification.evaluator;

import com.eventitta.domain.gamification.domain.ActivityType;
import com.eventitta.domain.gamification.domain.BadgeRule;
import com.eventitta.domain.gamification.domain.EvaluationType;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ActivityCountRuleEvaluator implements BadgeRuleEvaluator {

    @Override
    public boolean supports(BadgeRule rule) {
        return rule.getActivityType() != null && rule.getEvaluationType() == EvaluationType.COUNT;
    }

    @Override
    public boolean isSatisfied(BadgeRule rule, Map<ActivityType, Long> activityCountMap, Map<ActivityType, Long> activityPointsMap) {
        long count = activityCountMap.getOrDefault(rule.getActivityType(), 0L);
        return count >= rule.getThreshold();
    }
}
