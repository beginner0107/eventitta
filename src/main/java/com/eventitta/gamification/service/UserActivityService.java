package com.eventitta.gamification.service;

import com.eventitta.gamification.domain.ActivityType;
import com.eventitta.gamification.domain.RankingType;
import com.eventitta.gamification.domain.UserActivity;
import com.eventitta.gamification.dto.projection.ActivitySummaryProjection;
import com.eventitta.gamification.repository.UserActivityRepository;
import com.eventitta.user.domain.User;
import com.eventitta.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
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
    private final RankingService rankingService;

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

        updateRankingsAsync(userId, user.getPoints());
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

            User user = userRepository.findById(userId).orElse(null);
            if (user != null) {
                updateRankingsAsync(userId, user.getPoints());
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

    /**
     * 순위 업데이트 (비동기)
     * 포인트 및 활동량 순위를 별도 스레드에서 업데이트
     */
    @Async("rankingExecutor")
    public void updateRankingsAsync(Long userId, int currentPoints) {
        try {
            // 포인트 순위 업데이트
            rankingService.updatePointsRanking(userId, currentPoints);

            // 활동량 순위 업데이트
            long activityCount = userActivityRepository.countByUserId(userId);
            rankingService.updateActivityCountRanking(userId, activityCount);

            log.debug("[UserActivity] Rankings updated asynchronously. " +
                "userId={}, points={}, activities={}", userId, currentPoints, activityCount);
        } catch (Exception e) {
            log.error("[UserActivity] Failed to update rankings asynchronously. userId={}",
                userId, e);
            // 순위 업데이트 실패는 주요 비즈니스 로직에 영향을 주지 않음
        }
    }
}
