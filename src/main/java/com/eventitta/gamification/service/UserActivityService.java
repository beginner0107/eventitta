package com.eventitta.gamification.service;

import com.eventitta.gamification.domain.ActivityType;
import com.eventitta.gamification.domain.UserActivity;
import com.eventitta.gamification.dto.query.ActivitySummary;
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
        User user = findUserById(userId);

        // USER_LOGIN의 경우 일일 중복 방지
        if (activityType == ActivityType.USER_LOGIN && isAlreadyRecordedToday(userId, activityType)) {
            log.debug("User {} already logged in today, skipping duplicate login activity", userId);
            return;
        }

        UserActivity userActivity = createUserActivity(userId, activityType, targetId);
        userActivityRepository.save(userActivity);

        user.earnPoints(activityType.getDefaultPoint());
        badgeService.checkAndAwardBadges(user);
    }

    @Transactional
    public void revokeActivity(Long userId, ActivityType activityType, Long targetId) {
        User user = findUserById(userId);

        long deletedCount = userActivityRepository
            .deleteByUserIdAndActivityTypeAndTargetId(userId, activityType, targetId);

        if (deletedCount > 0) {
            user.deductPoints(activityType.getDefaultPoint());
        }
    }

    @Transactional(readOnly = true)
    public List<ActivitySummary> getActivitySummary(Long userId) {
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
