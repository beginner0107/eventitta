package com.eventitta.domain.gamification.evaluator;

import com.eventitta.domain.gamification.domain.ActivityType;
import com.eventitta.domain.gamification.domain.BadgeRule;
import com.eventitta.domain.gamification.domain.EvaluationType;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ActivityPointsRuleEvaluator implements BadgeRuleEvaluator {

    @Override
    public boolean supports(BadgeRule rule) {
        return rule.getActivityType() != null && rule.getEvaluationType() == EvaluationType.POINTS;
    }

    @Override
    public boolean isSatisfied(BadgeRule rule, Map<ActivityType, Long> activityCountMap, Map<ActivityType, Long> activityPointsMap) {
        long totalPoints = activityPointsMap.getOrDefault(rule.getActivityType(), 0L);
        return totalPoints >= rule.getThreshold();
    }
}
