package com.eventitta.gamification.event;

import com.eventitta.gamification.domain.OperationType;
import com.eventitta.gamification.service.UserActivityService;
import com.eventitta.gamification.service.FailedEventRecoveryService;
import com.eventitta.notification.domain.AlertLevel;
import com.eventitta.notification.service.DiscordNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import static com.eventitta.gamification.constants.GamificationErrorCodes.ACTIVITY_RECORD_FAILED;
import static com.eventitta.gamification.constants.GamificationErrorCodes.ACTIVITY_REVOKE_FAILED;
import static com.eventitta.gamification.constants.GamificationErrorCodes.ACTIVITY_PERSIST_FAILED;
import static com.eventitta.gamification.constants.GamificationSlackConstants.*;
import static org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserActivityEventListener {

    private final UserActivityService userActivityService;
    private final DiscordNotificationService discordNotificationService;
    private final FailedEventRecoveryService failedEventRecoveryService;

    @Async
    @TransactionalEventListener(phase = AFTER_COMMIT)
    @Retryable(retryFor = { Exception.class }, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public void handleUserActivity(UserActivityLogRequestedEvent event) {
        log.info("[활동 기록 시작] userId={}, activityType={}, targetId={}",
                event.userId(), event.activityType(), event.targetId());

        userActivityService.recordActivity(
                event.userId(),
                event.activityType(),
                event.targetId());

        log.info("[활동 기록 성공] userId={}, activityType={}",
                event.userId(), event.activityType());
    }

    @Recover
    public void recoverUserActivity(Exception e, UserActivityLogRequestedEvent event) {
        log.error("[활동 기록 최종 실패] userId={}, activityType={}, targetId={}",
                event.userId(), event.activityType(), event.targetId(), e);

        try {
            failedEventRecoveryService.saveFailedEvent(
                    event.userId(),
                    event.activityType(),
                    OperationType.RECORD,
                    event.targetId(),
                    e.getMessage());
        } catch (Exception persistEx) {
            log.error("[실패 이벤트 저장 중 오류 - 이벤트 완전 유실 가능] userId={}, activityType={}, targetId={}",
                    event.userId(), event.activityType(), event.targetId(), persistEx);

            sendDiscordAlertSafely(
                    AlertLevel.CRITICAL,
                    ACTIVITY_PERSIST_FAILED,
                    String.format("실패 이벤트 DB 저장 실패 (이벤트 유실): userId=%d, type=%s, targetId=%d",
                            event.userId(), event.activityType(), event.targetId()),
                    event.userId(),
                    persistEx);
        }

        String message = String.format(
                ACTIVITY_RECORD_FAILED_MESSAGE_FORMAT,
                event.userId(), event.activityType(), event.targetId());

        sendDiscordAlertSafely(
                AlertLevel.HIGH,
                ACTIVITY_RECORD_FAILED,
                message,
                event.userId(),
                e);
    }

    @Async
    @TransactionalEventListener(phase = AFTER_COMMIT)
    @Retryable(retryFor = { Exception.class }, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public void handleUserActivityRevoke(UserActivityRevokeRequestedEvent event) {
        log.info("[활동 취소 시작] userId={}, activityType={}, targetId={}",
                event.userId(), event.activityType(), event.targetId());

        userActivityService.revokeActivity(
                event.userId(),
                event.activityType(),
                event.targetId());

        log.info("[활동 취소 성공] userId={}, activityType={}",
                event.userId(), event.activityType());
    }

    @Recover
    public void recoverUserActivityRevoke(Exception e, UserActivityRevokeRequestedEvent event) {
        log.error("[활동 취소 최종 실패] userId={}, activityType={}, targetId={}",
                event.userId(), event.activityType(), event.targetId(), e);

        try {
            failedEventRecoveryService.saveFailedEvent(
                    event.userId(),
                    event.activityType(),
                    OperationType.REVOKE,
                    event.targetId(),
                    e.getMessage());
        } catch (Exception persistEx) {
            log.error("[실패 이벤트 저장 중 오류 - 이벤트 완전 유실 가능] userId={}, activityType={}, targetId={}",
                    event.userId(), event.activityType(), event.targetId(), persistEx);

            sendDiscordAlertSafely(
                    AlertLevel.CRITICAL,
                    ACTIVITY_PERSIST_FAILED,
                    String.format("실패 이벤트 DB 저장 실패 (이벤트 유실): userId=%d, type=%s, targetId=%d",
                            event.userId(), event.activityType(), event.targetId()),
                    event.userId(),
                    persistEx);
        }

        String message = String.format(
                ACTIVITY_REVOKE_FAILED_MESSAGE_FORMAT,
                event.userId(), event.activityType(), event.targetId());

        sendDiscordAlertSafely(
                AlertLevel.HIGH,
                ACTIVITY_REVOKE_FAILED,
                message,
                event.userId(),
                e);
    }

    private void sendDiscordAlertSafely(AlertLevel level, String errorCode, String message,
            Long userId, Exception e) {
        String userInfo = String.format(USER_INFO_FORMAT, userId);

        try {
            discordNotificationService.sendAlert(
                    level,
                    errorCode,
                    message,
                    REQUEST_URI,
                    userInfo,
                    e);
        } catch (Exception discordException) {
            log.error("[Discord 알림 발송 실패]", discordException);
        }
    }
}
