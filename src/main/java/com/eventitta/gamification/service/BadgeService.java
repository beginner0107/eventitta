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

@Service
@RequiredArgsConstructor
public class BadgeService {

    private final BadgeRepository badgeRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final UserActivityRepository userActivityRepository;

    @Transactional
    public void checkAndAwardBadges(User user, ActivityType activityType) {
        for (BadgeRule rule : BadgeRule.values()) {
            if (rule.getActivityType() != activityType) {
                continue;
            }
            long count = userActivityRepository.countByUserIdAndActivityType(user.getId(), rule.getActivityType());
            if (count >= rule.getThreshold()) {
                awardBadge(user, rule.getBadgeName());
            }
        }
    }

    private void awardBadge(User user, String badgeName) {
        badgeRepository.findByName(badgeName).ifPresent(badge -> {
            boolean alreadyHasBadge = userBadgeRepository.existsByUserIdAndBadgeId(user.getId(), badge.getId());
            if (!alreadyHasBadge) {
                userBadgeRepository.save(new UserBadge(user, badge));
            }
        });
    }
}
