package com.eventitta.gamification.scheduler;

import com.eventitta.gamification.domain.FailedActivityEvent;
import com.eventitta.gamification.domain.FailedActivityEvent.EventStatus;
import com.eventitta.gamification.repository.FailedActivityEventRepository;
import com.eventitta.gamification.service.FailedEventRecoveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static com.eventitta.gamification.constants.GamificationRetryConstants.*;
import static org.springframework.transaction.annotation.Propagation.NEVER;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "scheduler.failed-event-retry.enabled", havingValue = "true", matchIfMissing = true)
public class FailedActivityEventRetryScheduler {

    private final FailedActivityEventRepository failedActivityEventRepository;
    private final FailedEventRecoveryService failedEventRecoveryService;

    /**
     * 실패 이벤트 재처리 스케줄러
     * 각 이벤트를 개별 트랜잭션으로 처리하여 하나의 실패가 다른 이벤트에 영향을 주지 않도록 함
     */
    @Scheduled(fixedDelay = FAILED_EVENT_RETRY_FIXED_DELAY_MS)
    @SchedulerLock(name = "retryFailedActivityEvents", lockAtMostFor = "PT55S", lockAtLeastFor = "PT5S")
    @Transactional(propagation = NEVER)
    public void retryFailedEvents() {
        logFailedEventStatistics();

        List<FailedActivityEvent> pendingEvents = failedActivityEventRepository
                .findByStatusAndRetryCountLessThan(EventStatus.PENDING, FAILED_EVENT_MAX_RETRY_COUNT);

        if (pendingEvents.isEmpty()) {
            return;
        }

        log.info("[Scheduler] 실패 이벤트 재처리 시작 - 대상 건수: {}, 배치 크기: {}",
                pendingEvents.size(), FAILED_EVENT_RETRY_BATCH_SIZE);

        List<FailedActivityEvent> eventsToProcess = pendingEvents.stream()
                .limit(FAILED_EVENT_RETRY_BATCH_SIZE)
                .toList();

        int successCount = 0;
        int failureCount = 0;

        for (FailedActivityEvent event : eventsToProcess) {
            try {
                failedEventRecoveryService.recoverFailedEventIndependently(event.getId());
                successCount++;
            } catch (Exception e) {
                log.warn("[Scheduler] 개별 이벤트 재처리 실패 - eventId={}, error={}",
                        event.getId(), e.getMessage());
                failureCount++;
            }
        }

        log.info("[Scheduler] 실패 이벤트 재처리 완료 - 성공: {}, 실패: {}, 전체: {}",
                successCount, failureCount, eventsToProcess.size());
    }

    /**
     * PROCESSING 상태에서 stuck된 이벤트를 PENDING으로 되돌리는 복구 스케줄러
     * JVM 크래시 등으로 PROCESSING 상태에서 멈춘 레코드를 감지하여 재처리 대상으로 전환
     */
    @Scheduled(fixedDelay = STUCK_PROCESSING_RECOVERY_FIXED_DELAY_MS)
    @SchedulerLock(name = "recoverStuckFailedEvents", lockAtMostFor = "PT4M", lockAtLeastFor = "PT30S")
    @Transactional
    public void recoverStuckProcessingEvents() {
        LocalDateTime threshold = LocalDateTime.now()
                .minusMinutes(STUCK_PROCESSING_TIMEOUT_MINUTES);

        int recovered = failedActivityEventRepository.revertStuckProcessing(threshold);

        if (recovered > 0) {
            log.warn("[StuckRecovery] PROCESSING → PENDING 전환: {}건 ({}분 이상 stuck)",
                    recovered, STUCK_PROCESSING_TIMEOUT_MINUTES);
        }
    }

    /**
     * 실패 이벤트 현황 통계 로깅
     * 운영 가시성 확보를 위해 PENDING/PROCESSING/FAILED 상태별 건수를 출력
     */
    private void logFailedEventStatistics() {
        long totalPending = failedActivityEventRepository.countByStatus(EventStatus.PENDING);
        long totalProcessing = failedActivityEventRepository.countByStatus(EventStatus.PROCESSING);
        long totalFailed = failedActivityEventRepository.countByStatus(EventStatus.FAILED);

        if (totalFailed > 0 || totalProcessing > 0) {
            log.warn("[FailedEvent 현황] PENDING={}, PROCESSING={}, FAILED={}",
                    totalPending, totalProcessing, totalFailed);
        } else if (totalPending > 0) {
            log.info("[FailedEvent 현황] PENDING={}", totalPending);
        }
    }
}
