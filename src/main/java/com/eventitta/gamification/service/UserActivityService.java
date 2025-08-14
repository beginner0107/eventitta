package com.eventitta.gamification.service;

import com.eventitta.gamification.domain.ActivityType;
import com.eventitta.gamification.domain.UserActivity;
import com.eventitta.gamification.domain.UserPoints;
import com.eventitta.gamification.dto.query.ActivitySummary;
import com.eventitta.gamification.exception.UserActivityException;
import com.eventitta.gamification.repository.ActivityTypeRepository;
import com.eventitta.gamification.repository.UserActivityRepository;
import com.eventitta.gamification.repository.UserPointsRepository;
import com.eventitta.user.domain.User;
import com.eventitta.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.eventitta.gamification.exception.UserActivityErrorCode.CONCURRENT_MODIFICATION_RETRY_EXHAUSTED;
import static com.eventitta.gamification.exception.UserActivityErrorCode.DUPLICATED_USER_ACTIVITY;
import static com.eventitta.gamification.exception.UserActivityErrorCode.INVALID_ACTIVITY_TYPE;
import static com.eventitta.user.exception.UserErrorCode.NOT_FOUND_USER_ID;
import static com.eventitta.gamification.exception.UserPointsErrorCode.NOT_FOUND_USER_POINTS;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserActivityService {

    private final UserActivityRepository userActivityRepository;
    private final ActivityTypeRepository activityTypeRepository;
    private final UserRepository userRepository;
    private final BadgeService badgeService;
    private final UserPointsRepository userPointsRepository;


    @Retryable(
        retryFor = {ObjectOptimisticLockingFailureException.class},
        noRetryFor = {UserActivityException.class},
        maxAttempts = 3,
        backoff = @Backoff(
            delay = 50,
            multiplier = 2.0,
            random = true
        )
    )
    @Transactional
    public void recordActivity(Long userId, String activityCode, Long targetId) {
        User user = userRepository.findById(userId)
            .orElseThrow(NOT_FOUND_USER_ID::defaultException);

        ActivityType type = activityTypeRepository.findByCode(activityCode)
            .orElseThrow(INVALID_ACTIVITY_TYPE::defaultException);

        try {
            userActivityRepository.saveAndFlush(new UserActivity(user, type, targetId));
        } catch (DataIntegrityViolationException dup) {
            throw DUPLICATED_USER_ACTIVITY.defaultException(dup);
        }

        int delta = type.getDefaultPoint();

        userPointsRepository.upsertAndAddPoints(userId, delta);

        UserPoints currentPoints = userPointsRepository.findByUserId(userId)
            .orElseThrow(NOT_FOUND_USER_POINTS::defaultException);
        badgeService.checkAndAwardBadges(user, currentPoints);
    }

    @Recover
    public void recoverRecordActivity(ObjectOptimisticLockingFailureException ex, Long userId, String activityCode, Long targetId) {
        log.error("모든 재시도 후 활동 기록에 실패했습니다. 사용자ID={}, 활동코드={}, 대상ID={}",
            userId, activityCode, targetId, ex);

        throw CONCURRENT_MODIFICATION_RETRY_EXHAUSTED.defaultException(ex);
    }

    @Transactional
    public void revokeActivity(Long userId, String activityCode, Long targetId) {
        userRepository.findById(userId)
            .orElseThrow(NOT_FOUND_USER_ID::defaultException);

        ActivityType activityType = activityTypeRepository.findByCode(activityCode)
            .orElseThrow(INVALID_ACTIVITY_TYPE::defaultException);

        userActivityRepository.findByUserIdAndActivityType_IdAndTargetId(userId, activityType.getId(), targetId)
            .ifPresent(activity -> {
                UserPoints userPoints = userPointsRepository.findByUserId(userId)
                    .orElseThrow(NOT_FOUND_USER_POINTS::defaultException);

                userPoints.subtractPoints(activityType.getDefaultPoint());
                userActivityRepository.delete(activity);
            });
    }

    @Transactional(readOnly = true)
    public List<ActivitySummary> getActivitySummary(Long userId) {
        return userActivityRepository.countActivitiesByUser(userId);
    }
}
