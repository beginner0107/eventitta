package com.eventitta.gamification.event;

import com.eventitta.gamification.repository.UserActivityRepository;
import com.eventitta.gamification.service.BadgeService;
import com.eventitta.gamification.service.RankingService;
import com.eventitta.notification.domain.AlertLevel;
import com.eventitta.notification.service.SlackNotificationService;
import com.eventitta.user.domain.User;
import com.eventitta.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.eventitta.gamification.constants.GamificationErrorCodes.BADGE_CHECK_FAILED;
import static com.eventitta.gamification.constants.GamificationErrorCodes.RANKING_UPDATE_FAILED;
import static org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT;

/**
 * 활동 기록 후 부가 작업을 비동기로 처리
 * - 뱃지 체크 및 부여
 * - 랭킹 업데이트
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ActivityPostProcessor {

    private final BadgeService badgeService;
    private final RankingService rankingService;
    private final UserRepository userRepository;
    private final UserActivityRepository userActivityRepository;
    private final SlackNotificationService slackNotificationService;

    /**
     * 활동 기록 후 뱃지와 랭킹을 비동기로 처리
     * 각각 독립적으로 실행되어 하나가 실패해도 다른 작업에 영향 없음
     */
    @Async("gamificationExecutor")
    @TransactionalEventListener(phase = AFTER_COMMIT)
    public void handleActivityRecorded(ActivityRecordedEvent event) {
        log.info("[ActivityPostProcessor] 활동 후처리 시작 - userId={}, activityType={}",
            event.userId(), event.activityType());

        CompletableFuture<Void> badgeFuture = processBadgesAsync(event);
        CompletableFuture<Void> rankingFuture = processRankingsAsync(event);

        CompletableFuture.allOf(badgeFuture, rankingFuture)
            .thenRun(() ->
                log.info("[ActivityPostProcessor] 활동 후처리 완료 - userId={}, activityType={}",
                    event.userId(), event.activityType())
            )
            .exceptionally(ex -> {
                log.error("[ActivityPostProcessor] 활동 후처리 중 일부 실패 - userId={}",
                    event.userId(), ex);
                return null;
            });
    }

    /**
     * 뱃지 체크 및 부여
     */
    private CompletableFuture<Void> processBadgesAsync(ActivityRecordedEvent event) {
        return CompletableFuture.runAsync(() -> {
            try {
                checkAndAwardBadges(event.userId());
            } catch (Exception e) {
                handleBadgeCheckFailure(event, e);
            }
        });
    }

    /**
     * 뱃지 체크 실행
     */
    @Retryable(
        retryFor = {Exception.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    private void checkAndAwardBadges(Long userId) {
        log.debug("[Badge] 뱃지 체크 시작 - userId={}", userId);

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        List<String> awardedBadges = badgeService.checkAndAwardBadges(user);

        if (!awardedBadges.isEmpty()) {
            log.info("[Badge] 뱃지 부여 완료 - userId={}, badges={}",
                userId, String.join(", ", awardedBadges));
        }
    }

    /**
     * 뱃지 체크 실패 처리
     */
    private void handleBadgeCheckFailure(ActivityRecordedEvent event, Exception e) {
        log.error("[Badge] 뱃지 체크 실패 - userId={}, activityType={}",
            event.userId(), event.activityType(), e);

        // 뱃지 체크 실패는 핵심 기능이 아니므로 경고 레벨로 알림
        sendSlackAlertSafely(
            AlertLevel.MEDIUM,
            BADGE_CHECK_FAILED,
            String.format("뱃지 체크 실패: userId=%d, activityType=%s",
                event.userId(), event.activityType()),
            event.userId(),
            e
        );
    }

    /**
     * 랭킹 업데이트
     */
    private CompletableFuture<Void> processRankingsAsync(ActivityRecordedEvent event) {
        return CompletableFuture.runAsync(() -> {
            try {
                updateRankings(event.userId(), event.points());
            } catch (Exception e) {
                handleRankingUpdateFailure(event, e);
            }
        });
    }

    /**
     * 랭킹 업데이트 실행
     */
    @Retryable(
        retryFor = {Exception.class},
        maxAttempts = 2,
        backoff = @Backoff(delay = 500)
    )
    private void updateRankings(Long userId, Integer currentPoints) {
        log.debug("[Ranking] 랭킹 업데이트 시작 - userId={}, points={}", userId, currentPoints);

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            log.warn("[Ranking] 사용자 조회 실패, 랭킹 업데이트 건너뜀 - userId={}", userId);
            return;
        }

        int totalPoints = user.getPoints();
        rankingService.updatePointsRanking(userId, totalPoints);

        long activityCount = userActivityRepository.countByUserId(userId);
        rankingService.updateActivityCountRanking(userId, activityCount);

        log.info("[Ranking] 랭킹 업데이트 완료 - userId={}, points={}, activities={}",
            userId, totalPoints, activityCount);
    }

    /**
     * 랭킹 업데이트 실패 처리
     */
    private void handleRankingUpdateFailure(ActivityRecordedEvent event, Exception e) {
        log.error("[Ranking] 랭킹 업데이트 실패 - userId={}", event.userId(), e);

        sendSlackAlertSafely(
            AlertLevel.INFO,
            RANKING_UPDATE_FAILED,
            String.format("랭킹 업데이트 실패: userId=%d", event.userId()),
            event.userId(),
            e
        );
    }

    /**
     * Slack 알림 발송
     */
    private void sendSlackAlertSafely(AlertLevel level, String errorCode, String message,
                                      Long userId, Exception e) {
        try {
            slackNotificationService.sendAlert(
                level,
                errorCode,
                message,
                "/async/activity-post-processor",
                String.format("userId=%d", userId),
                e
            );
        } catch (Exception slackException) {
            log.error("[Slack] 알림 발송 실패", slackException);
        }
    }
}
