package com.eventitta.gamification.service;

import com.eventitta.gamification.domain.ActivityType;
import com.eventitta.gamification.domain.BadgeRule;
import com.eventitta.gamification.domain.UserBadge;
import com.eventitta.gamification.repository.BadgeRepository;
import com.eventitta.gamification.repository.UserActivityRepository;
import com.eventitta.gamification.repository.UserBadgeRepository;
import com.eventitta.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BadgeService {

    private final BadgeRepository badgeRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final UserActivityRepository userActivityRepository;

    @Transactional
    public Optional<String> checkAndAwardBadges(User user, ActivityType activityType) {
        for (BadgeRule rule : BadgeRule.values()) {
            if (rule.getActivityType() != activityType) {
                continue;
            }
            long count = userActivityRepository.countByUserIdAndActivityType(user.getId(), rule.getActivityType());
            if (count >= rule.getThreshold()) {
                awardBadge(user, rule.getBadgeName());
                return awardBadge(user, rule.getBadgeName());
            }
        }
        return Optional.empty();
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
