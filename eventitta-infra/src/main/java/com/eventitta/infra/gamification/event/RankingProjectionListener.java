package com.eventitta.infra.gamification.event;

import com.eventitta.domain.gamification.event.GamificationStateChangedEvent;
import com.eventitta.domain.gamification.service.RankingService;
import com.eventitta.domain.notification.domain.AlertLevel;
import com.eventitta.domain.notification.service.AlertNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import static com.eventitta.domain.gamification.constants.GamificationErrorCodes.RANKING_UPDATE_FAILED;
import static org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT;

@Slf4j
@Component
@RequiredArgsConstructor
public class RankingProjectionListener {

    private final RankingService rankingService;
    private final AlertNotificationService alertNotificationService;

    @Async("gamificationExecutor")
    @TransactionalEventListener(phase = AFTER_COMMIT)
    public void handle(GamificationStateChangedEvent event) {
        try {
            rankingService.updatePointsRanking(event.userId(), event.totalPoints());
            rankingService.updateActivityCountRanking(event.userId(), event.totalActivityCount());
        } catch (Exception e) {
            log.error("[RankingProjection] Failed to project rankings. userId={}", event.userId(), e);
            sendDiscordAlertSafely(event, e);
        }
    }

    private void sendDiscordAlertSafely(GamificationStateChangedEvent event, Exception e) {
        try {
            alertNotificationService.sendAlert(
                AlertLevel.INFO,
                RANKING_UPDATE_FAILED,
                "랭킹 업데이트 실패: userId=" + event.userId(),
                "/async/gamification/ranking-projection",
                "userId=" + event.userId(),
                e
            );
        } catch (Exception discordException) {
            log.error("[RankingProjection] Failed to send Discord alert", discordException);
        }
    }
}
