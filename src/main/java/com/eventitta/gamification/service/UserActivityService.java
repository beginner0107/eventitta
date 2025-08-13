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
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

import static com.eventitta.gamification.exception.UserActivityErrorCode.DUPLICATED_USER_ACTIVITY;
import static com.eventitta.gamification.exception.UserPointsErrorCode.NOT_FOUND_USER_POINTS;
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

    private final PlatformTransactionManager transactionManager;

    private static final int MAX_RETRIES = 3;

    public void recordActivity(Long userId, String activityCode, Long targetId) {
        int attempt = 0;

        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        while (true) {
            try {
                template.executeWithoutResult(status -> performRecordActivity(userId, activityCode, targetId));
                return;
            } catch (UserActivityException ok) {
                return;
            } catch (ObjectOptimisticLockingFailureException e) {
                if (++attempt >= MAX_RETRIES) throw e;
            }
        }
    }

    public void performRecordActivity(Long userId, String activityCode, Long targetId) {
        User user = userRepository.findById(userId)
            .orElseThrow(NOT_FOUND_USER_ID::defaultException);

        ActivityType type = activityTypeRepository.findByCode(activityCode)
            .orElseThrow(() -> new IllegalArgumentException("Invalid activityCode: " + activityCode));

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

    @Transactional
    public void revokeActivity(Long userId, String activityCode, Long targetId) {
        User user = userRepository.findById(userId)
            .orElseThrow(NOT_FOUND_USER_ID::defaultException);

        ActivityType activityType = activityTypeRepository.findByCode(activityCode)
            .orElseThrow(() -> new IllegalArgumentException("Invalid activityCode: " + activityCode));

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
