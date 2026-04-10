package com.eventitta.domain.gamification.service;

import com.eventitta.domain.gamification.domain.Badge;
import com.eventitta.domain.gamification.domain.BadgeRule;
import com.eventitta.domain.gamification.domain.RewardActionType;
import com.eventitta.domain.gamification.domain.UserBadge;
import com.eventitta.domain.gamification.repository.BadgeRuleRepository;
import com.eventitta.domain.gamification.repository.UserBadgeRepository;
import com.eventitta.domain.user.api.internal.facade.UserInternalFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class BadgeService {

    private final BadgeRuleRepository badgeRuleRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final UserInternalFacade userInternalFacade;

    @Transactional
    public List<String> checkAndAwardBadges(
        Long userId,
        RewardActionType actionType,
        long actionCount,
        int actionPointsTotal
    ) {
        if (!userInternalFacade.findUserProfile(userId).map(profile -> !profile.deleted()).orElse(false)) {
            return List.of();
        }

        List<BadgeRule> rules = badgeRuleRepository.findEnabledWithBadgeByActivityType(actionType.getLegacyType());
        if (rules.isEmpty()) {
            return List.of();
        }

        Set<Long> ownedBadgeIds = userBadgeRepository.findBadgeIdsByUserId(userId);
        List<String> awarded = new ArrayList<>();

        for (BadgeRule rule : rules) {
            Badge badge = rule.getBadge();
            if (ownedBadgeIds.contains(badge.getId())) {
                continue;
            }

            if (!isSatisfied(rule, actionCount, actionPointsTotal)) {
                continue;
            }

            if (awardBadge(userId, badge)) {
                awarded.add(badge.getName());
            }
        }

        return awarded;
    }

    private boolean isSatisfied(BadgeRule rule, long actionCount, int actionPointsTotal) {
        return switch (rule.getEvaluationType()) {
            case COUNT -> actionCount >= rule.getThreshold();
            case POINTS -> actionPointsTotal >= rule.getThreshold();
        };
    }

    private boolean awardBadge(Long userId, Badge badge) {
        try {
            userBadgeRepository.save(new UserBadge(userId, badge));
            return true;
        } catch (DataIntegrityViolationException e) {
            log.debug("[Badge] Duplicate badge ignored. userId={}, badgeId={}", userId, badge.getId());
            return false;
        }
    }
}
