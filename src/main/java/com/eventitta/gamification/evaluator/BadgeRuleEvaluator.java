package com.eventitta.gamification.evaluator;

import com.eventitta.gamification.domain.BadgeRule;
import com.eventitta.user.domain.User;

public interface BadgeRuleEvaluator {
    boolean supports(BadgeRule rule);

    boolean isSatisfied(User user, BadgeRule rule);
}
