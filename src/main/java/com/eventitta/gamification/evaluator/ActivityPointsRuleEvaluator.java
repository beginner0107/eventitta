package com.eventitta.gamification.evaluator;

import com.eventitta.gamification.domain.ActivityType;
import com.eventitta.gamification.domain.BadgeRule;
import com.eventitta.gamification.domain.EvaluationType;
import com.eventitta.user.domain.User;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ActivityPointsRuleEvaluator implements BadgeRuleEvaluator {

    @Override
    public boolean supports(BadgeRule rule) {
        return rule.getActivityType() != null && rule.getEvaluationType() == EvaluationType.POINTS;
    }

    @Override
    public boolean isSatisfied(User user, BadgeRule rule, Map<ActivityType, Long> activityCountMap, Map<ActivityType, Long> activityPointsMap) {
        long totalPoints = activityPointsMap.getOrDefault(rule.getActivityType(), 0L);
        return totalPoints >= rule.getThreshold();
    }
}
