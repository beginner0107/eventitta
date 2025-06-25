package com.eventitta.gamification.service;

import com.eventitta.gamification.domain.ActivityType;
import com.eventitta.gamification.domain.BadgeRule;
import com.eventitta.gamification.domain.UserBadge;
import com.eventitta.gamification.repository.BadgeRepository;
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

    private final BadgeRepository badgeRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final UserActivityRepository userActivityRepository;
    private final BadgeRuleRepository badgeRuleRepository;

    @Transactional
    public List<String> checkAndAwardBadges(User user, ActivityType activityType) {
        List<String> awardedBadges = new ArrayList<>();
        List<BadgeRule> rules = badgeRuleRepository.findByActivityType(activityType);

        if (rules.isEmpty()) {
            return awardedBadges;
        }

        long count = userActivityRepository.countByUserIdAndActivityType(user.getId(), activityType);

        for (BadgeRule rule : rules) {
            if (count >= rule.getThreshold()) {
                awardBadge(user, rule.getBadge().getName())
                    .ifPresent(awardedBadges::add);
            }
        }
        return awardedBadges;
    }

    private Optional<String> awardBadge(User user, String badgeName) {
        return badgeRepository.findByName(badgeName)
            .filter(badge -> !userBadgeRepository.existsByUserIdAndBadgeId(user.getId(), badge.getId()))
            .map(badge -> {
                userBadgeRepository.save(new UserBadge(user, badge));
                return badge.getName();
            });
    }
}
