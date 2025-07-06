package com.eventitta.gamification.service;

import com.eventitta.gamification.domain.Badge;
import com.eventitta.gamification.domain.BadgeRule;
import com.eventitta.gamification.domain.UserBadge;
import com.eventitta.gamification.repository.BadgeRuleRepository;
import com.eventitta.gamification.repository.UserActivityRepository;
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
    private final UserActivityRepository userActivityRepository;

    @Transactional
    public List<String> checkAndAwardBadges(User user) {
        List<BadgeRule> rules = badgeRuleRepository.findAll();
        List<String> awarded = new ArrayList<>();

        for (BadgeRule rule : rules) {
            if (!rule.isEnabled()) continue;

            long count = userActivityRepository.countByUserIdAndActivityType_Id(user.getId(), rule.getActivityType().getId());
            if (count >= rule.getThreshold()) {
                awardBadge(user, rule.getBadge()).ifPresent(awarded::add);
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
