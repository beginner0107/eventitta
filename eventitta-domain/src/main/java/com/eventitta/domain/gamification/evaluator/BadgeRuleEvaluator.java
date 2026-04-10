package com.eventitta.domain.gamification.evaluator;

import com.eventitta.domain.gamification.domain.ActivityType;
import com.eventitta.domain.gamification.domain.BadgeRule;

import java.util.Map;

public interface BadgeRuleEvaluator {
    boolean supports(BadgeRule rule);

    boolean isSatisfied(BadgeRule rule, Map<ActivityType, Long> activityCountMap, Map<ActivityType, Long> activityPointsMap);
}
