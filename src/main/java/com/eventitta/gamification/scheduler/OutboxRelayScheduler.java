package com.eventitta.gamification.scheduler;

import com.eventitta.gamification.domain.ActivityOutbox;
import com.eventitta.gamification.domain.ActivityOutbox.OutboxStatus;
import com.eventitta.gamification.repository.ActivityOutboxRepository;
import com.eventitta.gamification.service.OutboxRelayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static com.eventitta.gamification.constants.GamificationRetryConstants.*;
import static org.springframework.transaction.annotation.Propagation.NEVER;

/**
 * activity_outbox 테이블에서 PENDING 상태의 레코드를 폴링하여 처리하는 릴레이 스케줄러.
 * 각 레코드는 OutboxRelayService에서 독립 트랜잭션으로 처리됩니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "scheduler.outbox-relay.enabled", havingValue = "true", matchIfMissing = true)
public class OutboxRelayScheduler {

    private final ActivityOutboxRepository outboxRepository;
    private final OutboxRelayService outboxRelayService;

    /**
     * 아웃박스 릴레이 - PENDING 레코드를 폴링하여 활동 기록/취소 처리
     */
    @Scheduled(fixedDelay = OUTBOX_RELAY_FIXED_DELAY_MS)
    @SchedulerLock(name = "outboxRelay", lockAtMostFor = "PT4S", lockAtLeastFor = "PT1S")
    @Transactional(propagation = NEVER)
    public void relay() {
        List<ActivityOutbox> pendingEvents = outboxRepository.findPendingEvents(
                OutboxStatus.PENDING, OUTBOX_MAX_RETRY_COUNT,
                PageRequest.of(0, OUTBOX_RELAY_BATCH_SIZE));

        if (pendingEvents.isEmpty()) {
            return;
        }

        log.info("[OutboxRelay] 처리 시작 - 대상 건수: {}", pendingEvents.size());

        int successCount = 0;
        int failureCount = 0;

        for (ActivityOutbox outbox : pendingEvents) {
            try {
                outboxRelayService.processIndependently(outbox.getId());
                successCount++;
            } catch (Exception e) {
                log.warn("[OutboxRelay] 개별 처리 실패 - outboxId={}, error={}",
                        outbox.getId(), e.getMessage());
                failureCount++;
            }
        }

        log.info("[OutboxRelay] 처리 완료 - 성공: {}, 실패: {}, 전체: {}",
                successCount, failureCount, pendingEvents.size());
    }

    /**
     * PROCESSING 상태에서 stuck된 아웃박스 레코드를 PENDING으로 되돌림
     */
    @Scheduled(fixedDelay = STUCK_PROCESSING_RECOVERY_FIXED_DELAY_MS)
    @SchedulerLock(name = "outboxStuckRecovery", lockAtMostFor = "PT4M", lockAtLeastFor = "PT30S")
    @Transactional
    public void recoverStuckProcessing() {
        LocalDateTime threshold = LocalDateTime.now()
                .minusMinutes(STUCK_PROCESSING_TIMEOUT_MINUTES);

        int recovered = outboxRepository.revertStuckProcessing(threshold);

        if (recovered > 0) {
            log.warn("[OutboxStuckRecovery] PROCESSING → PENDING 전환: {}건", recovered);
        }
    }

    /**
     * 처리 완료된 아웃박스 레코드 정리 (housekeeping)
     */
    @Scheduled(cron = "0 0 4 * * *")
    @SchedulerLock(name = "outboxCleanup", lockAtMostFor = "PT10M")
    @Transactional
    public void cleanupProcessedRecords() {
        LocalDateTime threshold = LocalDateTime.now()
                .minusDays(OUTBOX_CLEANUP_RETENTION_DAYS);

        int deleted = outboxRepository.deleteProcessedBefore(threshold);

        if (deleted > 0) {
            log.info("[OutboxCleanup] 처리 완료 레코드 정리: {}건 ({}일 이전)",
                    deleted, OUTBOX_CLEANUP_RETENTION_DAYS);
        }
    }
}
