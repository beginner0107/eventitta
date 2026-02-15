package com.eventitta.gamification.service;

import com.eventitta.gamification.domain.ActivityOutbox;
import com.eventitta.gamification.domain.ActivityOutbox.OutboxStatus;
import com.eventitta.gamification.domain.OperationType;
import com.eventitta.gamification.repository.ActivityOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static com.eventitta.gamification.constants.GamificationRetryConstants.OUTBOX_MAX_RETRY_COUNT;

/**
 * 아웃박스 레코드를 개별 독립 트랜잭션으로 처리합니다.
 * Pessimistic Lock으로 동일 레코드의 동시 처리를 방지합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxRelayService {

    private final ActivityOutboxRepository outboxRepository;
    private final UserActivityService userActivityService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processIndependently(Long outboxId) {
        ActivityOutbox outbox = outboxRepository.findByIdWithLock(outboxId)
                .orElseThrow(() -> new IllegalArgumentException("Outbox record not found: " + outboxId));

        if (outbox.getStatus() != OutboxStatus.PENDING) {
            log.info("[Outbox 동시성 체크] id={}는 이미 처리 중이거나 완료됨. 상태: {}",
                    outboxId, outbox.getStatus());
            return;
        }

        outbox.markAsProcessing();
        outboxRepository.saveAndFlush(outbox);

        try {
            executeActivity(outbox);
            outbox.markAsDone();
            log.debug("[Outbox 처리 성공] id={}, userId={}, activityType={}, operationType={}",
                    outbox.getId(), outbox.getUserId(), outbox.getActivityType(), outbox.getOperationType());
        } catch (Exception e) {
            handleFailure(outbox, e);
        }

        outboxRepository.save(outbox);
    }

    private void executeActivity(ActivityOutbox outbox) {
        if (outbox.getOperationType() == OperationType.RECORD) {
            userActivityService.recordActivity(
                    outbox.getUserId(),
                    outbox.getActivityType(),
                    outbox.getTargetId());
        } else if (outbox.getOperationType() == OperationType.REVOKE) {
            userActivityService.revokeActivity(
                    outbox.getUserId(),
                    outbox.getActivityType(),
                    outbox.getTargetId());
        }
    }

    private void handleFailure(ActivityOutbox outbox, Exception e) {
        log.error("[Outbox 처리 실패] id={}, retryCount={}/{}, operationType={}",
                outbox.getId(), outbox.getRetryCount() + 1, OUTBOX_MAX_RETRY_COUNT,
                outbox.getOperationType(), e);

        if (outbox.getRetryCount() + 1 >= OUTBOX_MAX_RETRY_COUNT) {
            outbox.markAsFailed(e.getMessage());
            log.error("[Outbox 최종 실패] id={}, userId={}, activityType={}",
                    outbox.getId(), outbox.getUserId(), outbox.getActivityType());
        } else {
            outbox.incrementRetryAndRevertToPending(e.getMessage());
        }
    }
}
