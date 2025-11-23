package com.eventitta.gamification.service;

import com.eventitta.gamification.domain.ActivityType;
import com.eventitta.gamification.domain.UserActivity;
import com.eventitta.gamification.dto.projection.ActivitySummaryProjection;
import com.eventitta.gamification.repository.UserActivityRepository;
import com.eventitta.user.domain.User;
import com.eventitta.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static com.eventitta.user.exception.UserErrorCode.NOT_FOUND_USER_ID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserActivityService {

    private final UserActivityRepository userActivityRepository;
    private final UserRepository userRepository;
    private final BadgeService badgeService;

    @Transactional
    public void recordActivity(Long userId, ActivityType activityType, Long targetId) {
        if (activityType == ActivityType.USER_LOGIN
            && isAlreadyRecordedToday(userId, activityType)) {
            log.debug("[UserActivity] Skipping login activity: " +
                    "already recorded today. userId={}, activityType={}",
                userId, activityType);
            return;
        }

        UserActivity userActivity = createUserActivity(userId, activityType, targetId);
        userActivityRepository.save(userActivity);

        int points = activityType.getDefaultPoint();
        if (points > 0) {
            int updated = userRepository.incrementPoints(userId, points);
            if (updated == 0) {
                log.error("[UserActivity] Failed to increment points. " +
                        "reason=user_not_found, userId={}, deltaPoint={}",
                    userId, points);
                throw NOT_FOUND_USER_ID.defaultException();
            }
        }

        User user = findUserById(userId);
        badgeService.checkAndAwardBadges(user);
    }

    @Transactional
    public void revokeActivity(Long userId, ActivityType activityType, Long targetId) {
        long deletedCount = userActivityRepository
            .deleteByUserIdAndActivityTypeAndTargetId(userId, activityType, targetId);

        if (deletedCount > 0) {
            int points = activityType.getDefaultPoint();
            if (points > 0) {
                int updated = userRepository.decrementPoints(userId, points);
                if (updated == 0) {
                    log.warn("[UserActivity] Failed to decrement points. reason" +
                            "=user_not_found_or_insufficient_points, userId={}, deltaPoint={}",
                        userId, points);
                }
            }
        }
    }

    @Transactional(readOnly = true)
    public List<ActivitySummaryProjection> getActivitySummaryProjection(Long userId) {
        return userActivityRepository.countActivitiesByUser(userId);
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(NOT_FOUND_USER_ID::defaultException);
    }

    private UserActivity createUserActivity(Long userId, ActivityType activityType, Long targetId) {
        return activityType.createActivity(userId, targetId);
    }

    private boolean isAlreadyRecordedToday(Long userId, ActivityType activityType) {
        LocalDateTime startOfDay = LocalDateTime.now().with(LocalTime.MIN);
        LocalDateTime endOfDay = startOfDay.plusDays(1);

        return userActivityRepository.existsTodayActivity(userId, activityType, startOfDay, endOfDay);
    }
}
