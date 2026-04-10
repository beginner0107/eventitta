package com.eventitta.infra.gamification.event;

import com.eventitta.domain.gamification.event.GamificationStateChangedEvent;
import com.eventitta.domain.gamification.service.BadgeService;
import com.eventitta.domain.notification.domain.AlertLevel;
import com.eventitta.domain.notification.service.AlertNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import static com.eventitta.domain.gamification.constants.GamificationErrorCodes.BADGE_CHECK_FAILED;
import static org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT;

@Slf4j
@Component
@RequiredArgsConstructor
public class BadgeProjectionListener {

    private final BadgeService badgeService;
    private final AlertNotificationService alertNotificationService;

    @Async("gamificationExecutor")
    @TransactionalEventListener(phase = AFTER_COMMIT)
    public void handle(GamificationStateChangedEvent event) {
        try {
            badgeService.checkAndAwardBadges(
                event.userId(),
                event.actionType(),
                event.actionCount(),
                event.actionPointsTotal()
            );
        } catch (Exception e) {
            log.error("[BadgeProjection] Failed to project badges. userId={}, actionType={}",
                event.userId(), event.actionType(), e);
            sendDiscordAlertSafely(event, e);
        }
    }

    private void sendDiscordAlertSafely(GamificationStateChangedEvent event, Exception e) {
        try {
            alertNotificationService.sendAlert(
                AlertLevel.MEDIUM,
                BADGE_CHECK_FAILED,
                "뱃지 체크 실패: userId=" + event.userId() + ", actionType=" + event.actionType(),
                "/async/gamification/badge-projection",
                "userId=" + event.userId(),
                e
            );
        } catch (Exception discordException) {
            log.error("[BadgeProjection] Failed to send Discord alert", discordException);
        }
    }
}
