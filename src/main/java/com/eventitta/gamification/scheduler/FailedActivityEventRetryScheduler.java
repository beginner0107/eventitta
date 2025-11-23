package com.eventitta.gamification.scheduler;

import com.eventitta.gamification.domain.FailedActivityEvent;
import com.eventitta.gamification.domain.FailedActivityEvent.EventStatus;
import com.eventitta.gamification.repository.FailedActivityEventRepository;
import com.eventitta.gamification.service.FailedEventRecoveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.eventitta.gamification.constants.GamificationRetryConstants.*;
import static org.springframework.transaction.annotation.Propagation.NEVER;

@Slf4j
@Component
@RequiredArgsConstructor
public class FailedActivityEventRetryScheduler {

    private final FailedActivityEventRepository failedActivityEventRepository;
    private final FailedEventRecoveryService failedEventRecoveryService;

    @Scheduled(fixedDelay = FAILED_EVENT_RETRY_FIXED_DELAY_MS)
    @SchedulerLock(name = "retryFailedActivityEvents", lockAtMostFor = "55s", lockAtLeastFor = "5s")
    @Transactional(propagation = NEVER)
    public void retryFailedEvents() {
        List<FailedActivityEvent> pendingEvents = failedActivityEventRepository
            .findByStatusAndRetryCountLessThan(EventStatus.PENDING, FAILED_EVENT_MAX_RETRY_COUNT);

        if (pendingEvents.isEmpty()) {
            return;
        }

        log.debug("[실패 이벤트 재처리 시작] 대상 건수={}", pendingEvents.size());

        pendingEvents.stream()
            .limit(FAILED_EVENT_RETRY_BATCH_SIZE)
            .forEach(failedEventRecoveryService::recoverFailedEvent);
    }
}
