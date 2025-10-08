package com.eventitta.gamification.event;

import com.eventitta.gamification.service.UserActivityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import static org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserActivityEventListener {

    private final UserActivityService userActivityService;

    @Async
    @TransactionalEventListener(phase = AFTER_COMMIT)
    public void handleUserActivity(UserActivityLogRequestedEvent event) {
        try {
            userActivityService.recordActivity(event.userId(), event.activityType(), event.targetId());
        } catch (Exception e) {
            log.error("[활동 기록 실패] userId={}, activityCode={}, targetId={}",
                event.userId(), event.activityType().getDisplayName(), event.targetId(), e);
        }
    }

    @Async
    @TransactionalEventListener(phase = AFTER_COMMIT)
    public void handleUserActivityRevoke(UserActivityRevokeRequestedEvent event) {
        try {
            userActivityService.revokeActivity(event.userId(), event.activityType(), event.targetId());
        } catch (Exception e) {
            log.error("[활동 취소 실패] userId={}, activityCode={}, targetId={}",
                event.userId(), event.activityType(), event.targetId(), e);
        }
    }
}
