package com.eventitta.gamification.event;

import com.eventitta.gamification.service.UserActivityService;
import com.eventitta.notification.domain.AlertLevel;
import com.eventitta.notification.service.SlackNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import static org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserActivityEventListener {

    private final UserActivityService userActivityService;
    private final SlackNotificationService slackNotificationService;

    @Async
    @TransactionalEventListener(phase = AFTER_COMMIT)
    @Retryable(
        retryFor = {Exception.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void handleUserActivity(UserActivityLogRequestedEvent event) {
        log.info("[활동 기록 시작] userId={}, activityType={}, targetId={}",
            event.userId(), event.activityType(), event.targetId());

        userActivityService.recordActivity(
            event.userId(),
            event.activityType(),
            event.targetId()
        );

        log.info("[활동 기록 성공] userId={}, activityType={}",
            event.userId(), event.activityType());
    }

    @Recover
    public void recoverUserActivity(Exception e, UserActivityLogRequestedEvent event) {
        log.error("[활동 기록 최종 실패] userId={}, activityType={}, targetId={}",
            event.userId(), event.activityType(), event.targetId(), e);

        // Slack 알림 발송
        String errorCode = "ACTIVITY_RECORD_FAILED";
        String message = String.format(
            "포인트 기록 실패 - userId: %d, activity: %s, targetId: %d",
            event.userId(), event.activityType(), event.targetId()
        );
        String requestUri = "EventListener";
        String userInfo = String.format("userId=%d", event.userId());

        try {
            slackNotificationService.sendAlert(
                AlertLevel.HIGH,
                errorCode,
                message,
                requestUri,
                userInfo,
                e
            );
        } catch (Exception slackException) {
            log.error("[Slack 알림 발송 실패]", slackException);
        }
    }

    @Async
    @TransactionalEventListener(phase = AFTER_COMMIT)
    @Retryable(
        retryFor = {Exception.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void handleUserActivityRevoke(UserActivityRevokeRequestedEvent event) {
        log.info("[활동 취소 시작] userId={}, activityType={}, targetId={}",
            event.userId(), event.activityType(), event.targetId());

        userActivityService.revokeActivity(
            event.userId(),
            event.activityType(),
            event.targetId()
        );

        log.info("[활동 취소 성공] userId={}, activityType={}",
            event.userId(), event.activityType());
    }

    @Recover
    public void recoverUserActivityRevoke(Exception e, UserActivityRevokeRequestedEvent event) {
        log.error("[활동 취소 최종 실패] userId={}, activityType={}, targetId={}",
            event.userId(), event.activityType(), event.targetId(), e);

        String errorCode = "ACTIVITY_REVOKE_FAILED";
        String message = String.format(
            "포인트 취소 실패 - userId: %d, activity: %s, targetId: %d",
            event.userId(), event.activityType(), event.targetId()
        );
        String requestUri = "EventListener";
        String userInfo = String.format("userId=%d", event.userId());

        try {
            slackNotificationService.sendAlert(
                AlertLevel.HIGH,
                errorCode,
                message,
                requestUri,
                userInfo,
                e
            );
        } catch (Exception slackException) {
            log.error("[Slack 알림 발송 실패]", slackException);
        }
    }
}
