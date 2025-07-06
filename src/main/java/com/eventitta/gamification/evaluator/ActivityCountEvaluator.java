package com.eventitta.gamification.evaluator;

import com.eventitta.gamification.repository.ActivityTypeRepository;
import com.eventitta.gamification.repository.UserActivityRepository;
import com.eventitta.user.domain.User;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component("ACTIVITY_COUNT")
@RequiredArgsConstructor
public class ActivityCountEvaluator implements BadgeRuleEvaluator {

    private final UserActivityRepository userActivityRepository;
    private final ActivityTypeRepository activityTypeRepository;
    private final ObjectMapper objectMapper;

    @Override
    public boolean evaluate(User user, String conditionJson) {
        try {
            JsonNode json = objectMapper.readTree(conditionJson);
            String activityCode = json.get("activityCode").asText();
            int threshold = json.get("threshold").asInt();

            Long activityTypeId = activityTypeRepository.findByCode(activityCode)
                .orElseThrow(() -> new IllegalArgumentException("Invalid activityCode: " + activityCode))
                .getId();

            long count = userActivityRepository.countByUserIdAndActivityType_Id(user.getId(), activityTypeId);
            return count >= threshold;
        } catch (Exception e) {
            throw new RuntimeException("Badge condition JSON 파싱 실패: " + conditionJson, e);
        }
    }
}
