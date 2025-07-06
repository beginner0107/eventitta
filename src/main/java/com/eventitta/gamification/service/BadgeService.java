package com.eventitta.gamification.service;

import com.eventitta.gamification.domain.Badge;
import com.eventitta.gamification.domain.BadgeRule;
import com.eventitta.gamification.domain.UserBadge;
import com.eventitta.gamification.evaluator.BadgeRuleEvaluator;
import com.eventitta.gamification.repository.BadgeRepository;
import com.eventitta.gamification.repository.BadgeRuleRepository;
import com.eventitta.gamification.repository.UserBadgeRepository;
import com.eventitta.user.domain.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BadgeService {

    private final BadgeRepository badgeRepository;
    private final BadgeRuleRepository badgeRuleRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final Map<String, BadgeRuleEvaluator> evaluators;

    @Transactional
    public List<String> checkAndAwardBadges(User user) {
        List<BadgeRule> rules = badgeRuleRepository.findAll();
        List<String> awarded = new ArrayList<>();

        for (BadgeRule rule : rules) {
            if (!rule.isEnabled()) continue;

            String conditionJson = rule.getConditionJson();
            String type = extractType(conditionJson);

            BadgeRuleEvaluator evaluator = evaluators.get(type);
            if (evaluator == null) continue;

            boolean passed = evaluator.evaluate(user, conditionJson);
            if (passed) {
                awardBadge(user, rule.getBadge()).ifPresent(awarded::add);
            }
        }

        return awarded;
    }

    private String extractType(String json) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readTree(json).get("type").asText();
        } catch (Exception e) {
            throw new RuntimeException("conditionJson에서 type 추출 실패: " + json, e);
        }
    }

    private Optional<String> awardBadge(User user, Badge badge) {
        if (userBadgeRepository.existsByUserIdAndBadgeId(user.getId(), badge.getId())) {
            return Optional.empty();
        }
        userBadgeRepository.save(new UserBadge(user, badge));
        return Optional.of(badge.getName());
    }
}
