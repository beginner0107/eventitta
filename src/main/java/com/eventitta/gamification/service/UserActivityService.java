package com.eventitta.gamification.service;

import com.eventitta.gamification.domain.ActivityType;
import com.eventitta.gamification.domain.UserActivity;
import com.eventitta.gamification.dto.query.ActivitySummary;
import com.eventitta.gamification.repository.ActivityTypeRepository;
import com.eventitta.gamification.repository.UserActivityRepository;
import com.eventitta.user.domain.User;
import com.eventitta.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.eventitta.user.exception.UserErrorCode.NOT_FOUND_USER_ID;

@Service
@RequiredArgsConstructor
public class UserActivityService {

    private final UserActivityRepository userActivityRepository;
    private final ActivityTypeRepository activityTypeRepository;
    private final UserRepository userRepository;
    private final BadgeService badgeService;

    @Transactional
    public List<String> recordActivity(Long userId, String activityCode, Long targetId) {
        User user = userRepository.findById(userId)
            .orElseThrow(NOT_FOUND_USER_ID::defaultException);

        ActivityType activityType = activityTypeRepository.findByCode(activityCode)
            .orElseThrow(() -> new IllegalArgumentException("Invalid activityCode: " + activityCode));

        if (userActivityRepository.existsByUserIdAndActivityType_IdAndTargetId(userId, activityType.getId(), targetId)) {
            return List.of();
        }

        user.addPoints(activityType.getDefaultPoint());
        userActivityRepository.save(new UserActivity(user, activityType, targetId));

        return badgeService.checkAndAwardBadges(user);
    }

    @Transactional
    public void revokeActivity(Long userId, String activityCode, Long targetId) {
        User user = userRepository.findById(userId)
            .orElseThrow(NOT_FOUND_USER_ID::defaultException);

        ActivityType activityType = activityTypeRepository.findByCode(activityCode)
            .orElseThrow(() -> new IllegalArgumentException("Invalid activityCode: " + activityCode));

        userActivityRepository.findByUserIdAndActivityType_IdAndTargetId(userId, activityType.getId(), targetId)
            .ifPresent(activity -> {
                user.subtractPoints(activityType.getDefaultPoint());
                userActivityRepository.delete(activity);
            });
    }

    public List<ActivitySummary> getActivitySummary(Long userId) {
        return userActivityRepository.countActivitiesByUser(userId);
    }
}
