package com.eventitta.gamification.service;

import com.eventitta.gamification.domain.ActivityType;
import com.eventitta.gamification.domain.UserActivity;
import com.eventitta.gamification.domain.UserPoints;
import com.eventitta.gamification.dto.query.ActivitySummary;
import com.eventitta.gamification.repository.ActivityTypeRepository;
import com.eventitta.gamification.repository.UserActivityRepository;
import com.eventitta.gamification.repository.UserPointsRepository;
import com.eventitta.user.domain.User;
import com.eventitta.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.eventitta.user.exception.UserErrorCode.NOT_FOUND_USER_ID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserActivityService {

    private final UserActivityRepository userActivityRepository;
    private final ActivityTypeRepository activityTypeRepository;
    private final UserRepository userRepository;
    private final BadgeService badgeService;
    private final UserPointsRepository userPointsRepository;

    private static final int MAX_RETRIES = 3;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordActivity(Long userId, String activityCode, Long targetId) {
        int attempt = 0;
        while (true) {
            try {
                performRecordActivity(userId, activityCode, targetId);
                return;
            } catch (ObjectOptimisticLockingFailureException e) {
                if (++attempt >= MAX_RETRIES) {
                    throw e;
                }
            }
        }
    }

    public void performRecordActivity(Long userId, String activityCode, Long targetId) {        // 🐌 의도적으로 느린 작업 시뮬레이션 (실제 환경에서는 제거)
        User user = userRepository.findById(userId)
            .orElseThrow(NOT_FOUND_USER_ID::defaultException);

        ActivityType activityType = activityTypeRepository.findByCode(activityCode)
            .orElseThrow(() -> new IllegalArgumentException("Invalid activityCode: " + activityCode));

        if (userActivityRepository.existsByUserIdAndActivityType_IdAndTargetId(userId, activityType.getId(), targetId)) {
            return;
        }

        UserPoints userPoints = getOrCreateUserPoints(user);

        // 포인트 적립
        log.info("포인트 적립: +{}", activityType.getDefaultPoint());
        userPoints.addPoints(activityType.getDefaultPoint());
        userPointsRepository.save(userPoints);
        userPointsRepository.flush();

        // 활동 기록
        userActivityRepository.save(new UserActivity(user, activityType, targetId));
        badgeService.checkAndAwardBadges(user, userPoints);
    }

    @Transactional
    public void revokeActivity(Long userId, String activityCode, Long targetId) {
        User user = userRepository.findById(userId)
            .orElseThrow(NOT_FOUND_USER_ID::defaultException);

        ActivityType activityType = activityTypeRepository.findByCode(activityCode)
            .orElseThrow(() -> new IllegalArgumentException("Invalid activityCode: " + activityCode));

        userActivityRepository.findByUserIdAndActivityType_IdAndTargetId(userId, activityType.getId(), targetId)
            .ifPresent(activity -> {
                UserPoints userPoints = userPointsRepository.findById(userId)
                    .orElseThrow(() -> new IllegalStateException("UserPoints not found for userId: " + userId));

                userPoints.subtractPoints(activityType.getDefaultPoint());
                userActivityRepository.delete(activity);
            });
    }

    @Transactional(readOnly = true)
    public List<ActivitySummary> getActivitySummary(Long userId) {
        return userActivityRepository.countActivitiesByUser(userId);
    }

    private UserPoints getOrCreateUserPoints(User user) {
        return userPointsRepository.findById(user.getId())
            .orElseGet(() -> {
                userRepository.findWithPessimisticLockById(user.getId());

                return userPointsRepository.findById(user.getId())
                    .orElseGet(() -> userPointsRepository.save(new UserPoints(user)));
            });
    }
}


