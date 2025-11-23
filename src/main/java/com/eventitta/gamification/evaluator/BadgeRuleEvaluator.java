package com.eventitta.gamification.evaluator;

import com.eventitta.gamification.domain.ActivityType;
import com.eventitta.gamification.domain.BadgeRule;
import com.eventitta.user.domain.User;

import java.util.Map;

public interface BadgeRuleEvaluator {
    boolean supports(BadgeRule rule);

    boolean isSatisfied(User user, BadgeRule rule, Map<ActivityType, Long> activityCountMap, Map<ActivityType, Long> activityPointsMap);
}
