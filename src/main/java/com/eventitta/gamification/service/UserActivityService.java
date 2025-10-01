package com.eventitta.gamification.service;

import com.eventitta.gamification.domain.ActivityType;
import com.eventitta.gamification.domain.UserActivity;
import com.eventitta.gamification.dto.query.ActivitySummary;
import com.eventitta.gamification.repository.ActivityTypeRepository;
import com.eventitta.gamification.repository.UserActivityRepository;
import com.eventitta.user.domain.User;
import com.eventitta.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.eventitta.gamification.exception.UserActivityErrorCode.INVALID_ACTIVITY_TYPE;
import static com.eventitta.user.exception.UserErrorCode.NOT_FOUND_USER_ID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserActivityService {

    private final UserActivityRepository userActivityRepository;
    private final ActivityTypeRepository activityTypeRepository;
    private final UserRepository userRepository;
    private final BadgeService badgeService;

    @Transactional
    public void recordActivity(Long userId, String activityCode, Long targetId) {
        User user = userRepository.findById(userId)
            .orElseThrow(NOT_FOUND_USER_ID::defaultException);

        ActivityType type = activityTypeRepository.findByCode(activityCode)
            .orElseThrow(INVALID_ACTIVITY_TYPE::defaultException);

        userActivityRepository.save(new UserActivity(user, type, targetId));

        int delta = type.getDefaultPoint();

        user.earnPoints(delta);
        badgeService.checkAndAwardBadges(user);
    }

    @Transactional
    public void revokeActivity(Long userId, String activityCode, Long targetId) {
        User user = userRepository.findById(userId)
            .orElseThrow(NOT_FOUND_USER_ID::defaultException);

        ActivityType activityType = activityTypeRepository.findByCode(activityCode)
            .orElseThrow(INVALID_ACTIVITY_TYPE::defaultException);

        long deletedCount = userActivityRepository
            .deleteByUserIdAndActivityType_IdAndTargetId(userId, activityType.getId(), targetId);

        if (deletedCount > 0) {
            user.deductPoints(activityType.getDefaultPoint());
        }
    }

    @Transactional(readOnly = true)
    public List<ActivitySummary> getActivitySummary(Long userId) {
        return userActivityRepository.countActivitiesByUser(userId);
    }
}
