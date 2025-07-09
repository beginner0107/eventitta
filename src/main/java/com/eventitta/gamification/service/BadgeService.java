package com.eventitta.gamification.service;

import com.eventitta.gamification.domain.Badge;
import com.eventitta.gamification.domain.BadgeRule;
import com.eventitta.gamification.domain.UserBadge;
import com.eventitta.gamification.domain.UserPoints;
import com.eventitta.gamification.evaluator.BadgeRuleEvaluator;
import com.eventitta.gamification.repository.BadgeRuleRepository;
import com.eventitta.gamification.repository.UserBadgeRepository;
import com.eventitta.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BadgeService {

    private final BadgeRuleRepository badgeRuleRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final List<BadgeRuleEvaluator> evaluators;

    @Transactional
    public List<String> checkAndAwardBadges(User user, UserPoints userPoints) {
        List<BadgeRule> rules = badgeRuleRepository.findAll();
        List<String> awarded = new ArrayList<>();

        for (BadgeRule rule : rules) {
            if (!rule.isEnabled()) continue;

            for (BadgeRuleEvaluator evaluator : evaluators) {
                if (evaluator.supports(rule) && evaluator.isSatisfied(user, rule)) {
                    awardBadge(user, rule.getBadge()).ifPresent(awarded::add);
                    break;
                }
            }
        }

        return awarded;
    }

    private Optional<String> awardBadge(User user, Badge badge) {
        if (userBadgeRepository.existsByUserIdAndBadgeId(user.getId(), badge.getId())) {
            return Optional.empty();
        }
        userBadgeRepository.save(new UserBadge(user, badge));
        return Optional.of(badge.getName());
    }
}
