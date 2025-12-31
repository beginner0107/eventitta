package com.eventitta.gamification.service;

import com.eventitta.gamification.domain.ActivityType;
import com.eventitta.gamification.domain.UserActivity;
import com.eventitta.gamification.dto.projection.ActivitySummaryProjection;
import com.eventitta.gamification.event.ActivityRecordedEvent;
import com.eventitta.gamification.repository.UserActivityRepository;
import com.eventitta.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
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
    private final ApplicationEventPublisher eventPublisher;
    // BadgeService와 RankingService는 이제 직접 사용하지 않음

    /**
     * 활동을 기록하고 이벤트를 발행
     * 트랜잭션 범위를 최소화하여 락 보유 시간 단축
     */
    @Transactional
    public void recordActivity(Long userId, ActivityType activityType, Long targetId) {
        if (activityType == ActivityType.USER_LOGIN
            && isAlreadyRecordedToday(userId, activityType)) {
            log.debug("[UserActivity] Skipping login activity: " +
                    "already recorded today. userId={}, activityType={}",
                userId, activityType);
            return;
        }

        // 1. 핵심 데이터만 빠르게 저장 (활동 기록)
        UserActivity userActivity = createUserActivity(userId, activityType, targetId);
        userActivity = userActivityRepository.save(userActivity);

        // 2. 포인트 업데이트 (여전히 핵심 작업)
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

        // 3. 이벤트 발행 (트랜잭션 커밋 후 비동기로 처리됨)
        //    뱃지 체크와 랭킹 업데이트는 ActivityPostProcessor에서 처리
        eventPublisher.publishEvent(
            new ActivityRecordedEvent(
                userId,
                userActivity.getId(),
                activityType,
                points,
                targetId
            )
        );

        log.info("[UserActivity] Activity recorded successfully. " +
            "userId={}, activityType={}, points={}",
            userId, activityType, points);
    }

    /**
     * 활동 취소 (포인트 차감)
     * 랭킹 업데이트는 이벤트로 처리
     */
    @Transactional
    public void revokeActivity(Long userId, ActivityType activityType, Long targetId) {
        long deletedCount = userActivityRepository
            .deleteByUserIdAndActivityTypeAndTargetId(userId, activityType, targetId);

        if (deletedCount > 0) {
            // 1. 포인트 차감
            int points = activityType.getDefaultPoint();
            if (points > 0) {
                int updated = userRepository.decrementPoints(userId, points);
                if (updated == 0) {
                    log.warn("[UserActivity] Failed to decrement points. reason" +
                            "=user_not_found_or_insufficient_points, userId={}, deltaPoint={}",
                        userId, points);
                }
            }

            // 2. 이벤트 발행 (랭킹 업데이트를 위해)
            //    뱃지는 취소 시 회수하지 않으므로 별도 처리 불필요
            eventPublisher.publishEvent(
                new ActivityRecordedEvent(
                    userId,
                    null,  // 취소된 활동이므로 ID 없음
                    activityType,
                    -points,  // 음수 포인트로 차감 표시
                    targetId
                )
            );

            log.info("[UserActivity] Activity revoked successfully. " +
                "userId={}, activityType={}, points={}",
                userId, activityType, -points);
        }
    }

    @Transactional(readOnly = true)
    public List<ActivitySummaryProjection> getActivitySummaryProjection(Long userId) {
        return userActivityRepository.countActivitiesByUser(userId);
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
