package com.eventitta.gamification.evaluator;

import com.eventitta.gamification.domain.BadgeRule;
import com.eventitta.gamification.repository.UserActivityRepository;
import com.eventitta.user.domain.User;
import org.springframework.stereotype.Component;

@Component
public class ActivityCountRuleEvaluator implements BadgeRuleEvaluator {

    private final UserActivityRepository userActivityRepository;

    public ActivityCountRuleEvaluator(UserActivityRepository repo) {
        this.userActivityRepository = repo;
    }

    @Override
    public boolean supports(BadgeRule rule) {
        return rule.getActivityType() != null;
    }

    @Override
    public boolean isSatisfied(User user, BadgeRule rule) {
        long count = userActivityRepository.countByUserIdAndActivityType_Id(
            user.getId(), rule.getActivityType().getId()
        );
        return count >= rule.getThreshold();
    }
}
