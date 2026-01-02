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

import java.util.List;
import java.util.stream.Collectors;

import static com.eventitta.gamification.constants.GamificationRetryConstants.*;
import static org.springframework.transaction.annotation.Propagation.NEVER;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
    name = "scheduler.failed-event-retry.enabled",
    havingValue = "true",
    matchIfMissing = true
)
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
}
