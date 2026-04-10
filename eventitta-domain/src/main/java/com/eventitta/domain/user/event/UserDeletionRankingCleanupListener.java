package com.eventitta.domain.user.event;

import com.eventitta.domain.gamification.domain.RankingType;
import com.eventitta.domain.gamification.service.RankingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import static org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserDeletionRankingCleanupListener {

    private final RankingService rankingService;

    @TransactionalEventListener(phase = AFTER_COMMIT)
    public void handleUserDeleted(UserDeletedEvent event) {
        removeFromRanking(RankingType.POINTS, event.userId());
        removeFromRanking(RankingType.ACTIVITY_COUNT, event.userId());
    }

    private void removeFromRanking(RankingType type, Long userId) {
        try {
            rankingService.removeUser(type, userId);
        } catch (Exception e) {
            log.error("[UserDeletion] Failed to remove user from ranking. type={}, userId={}", type, userId, e);
        }
    }
}
