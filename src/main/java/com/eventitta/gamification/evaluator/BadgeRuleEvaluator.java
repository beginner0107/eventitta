package com.eventitta.gamification.evaluator;

import com.eventitta.user.domain.User;

public interface BadgeRuleEvaluator {
    boolean evaluate(User user, String conditionJson);
}
