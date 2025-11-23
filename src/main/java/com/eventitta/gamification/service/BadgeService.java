package com.eventitta.gamification.service;

import com.eventitta.gamification.domain.ActivityType;
import com.eventitta.gamification.domain.Badge;
import com.eventitta.gamification.domain.BadgeRule;
import com.eventitta.gamification.domain.UserBadge;
import com.eventitta.gamification.dto.projection.ActivitySummaryProjection;
import com.eventitta.gamification.evaluator.BadgeRuleEvaluator;
import com.eventitta.gamification.repository.BadgeRuleRepository;
import com.eventitta.gamification.repository.UserActivityRepository;
import com.eventitta.gamification.repository.UserBadgeRepository;
import com.eventitta.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BadgeService {

    private final BadgeRuleRepository badgeRuleRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final UserActivityRepository userActivityRepository;
    private final List<BadgeRuleEvaluator> evaluators;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<String> checkAndAwardBadges(User user) {
        List<BadgeRule> rules = badgeRuleRepository.findAllEnabledWithBadge();

        List<ActivitySummaryProjection> activityStats =
            userActivityRepository.countActivitiesByUser(user.getId());

        Map<ActivityType, Long> activityCountMap = activityStats.stream()
            .collect(Collectors.toMap(
                ActivitySummaryProjection::getActivityType,
                ActivitySummaryProjection::getCount
            ));

        Map<ActivityType, Long> activityPointsMap = activityStats.stream()
            .collect(Collectors.toMap(
                ActivitySummaryProjection::getActivityType,
                ActivitySummaryProjection::getTotalPoints
            ));

        Set<Long> ownedBadgeIds = userBadgeRepository.findBadgeIdsByUserId(user.getId());

        List<String> awarded = new ArrayList<>();

        for (BadgeRule rule : rules) {
            if (ownedBadgeIds.contains(rule.getBadge().getId())) {
                continue;
            }

            for (BadgeRuleEvaluator evaluator : evaluators) {
                if (evaluator.supports(rule)) {
                    if (evaluator.isSatisfied(user, rule, activityCountMap, activityPointsMap)) {
                        awardBadge(user, rule.getBadge());
                        awarded.add(rule.getBadge().getName());
                        break;
                    }
                }
            }
        }

        return awarded;
    }

    private void awardBadge(User user, Badge badge) {
        userBadgeRepository.save(new UserBadge(user, badge));
    }
}
