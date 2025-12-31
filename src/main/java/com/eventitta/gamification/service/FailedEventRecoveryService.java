package com.eventitta.gamification.service;

import static com.eventitta.gamification.constants.GamificationRetryConstants.FAILED_EVENT_MAX_ERROR_MESSAGE_LENGTH;
import static com.eventitta.gamification.constants.GamificationRetryConstants.FAILED_EVENT_MAX_RETRY_COUNT;

import com.eventitta.gamification.domain.ActivityType;
import com.eventitta.gamification.domain.FailedActivityEvent;
import com.eventitta.gamification.domain.OperationType;
import com.eventitta.gamification.repository.FailedActivityEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FailedEventRecoveryService {

    private final FailedActivityEventRepository failedEventRepository;
    private final UserActivityService userActivityService;

    private String truncateErrorMessage(String errorMessage) {
        if (errorMessage == null) {
            return null;
        }
        return errorMessage.length() > FAILED_EVENT_MAX_ERROR_MESSAGE_LENGTH
            ? errorMessage.substring(0, FAILED_EVENT_MAX_ERROR_MESSAGE_LENGTH)
            : errorMessage;
    }

    @Transactional
    public void saveFailedEvent(Long userId, ActivityType activityType, OperationType operationType,
                                Long targetId, String errorMessage) {
        FailedActivityEvent failedEvent = FailedActivityEvent.builder()
            .userId(userId)
            .activityType(activityType)
            .operationType(operationType)
            .targetId(targetId)
            .errorMessage(truncateErrorMessage(errorMessage))
            .build();

        failedEventRepository.save(failedEvent);
        log.debug("[실패 이벤트 저장] id={}, userId={}, activityType={}, operationType={}",
            failedEvent.getId(), userId, activityType, operationType);
    }

    /**
     * 실패 이벤트 복구 - 독립 트랜잭션으로 처리
     * 스케줄러에서 호출되며, 각 이벤트가 별도 트랜잭션에서 처리됨
     * Pessimistic Lock을 사용하여 동일 이벤트에 대한 동시 처리 방지
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recoverFailedEventIndependently(Long eventId) {
        FailedActivityEvent event = failedEventRepository.findByIdWithLock(eventId)
            .orElseThrow(() -> new IllegalArgumentException("Failed event not found: " + eventId));

        if (event.getStatus() != FailedActivityEvent.EventStatus.PENDING) {
            log.info("[동시성 체크] 이벤트 {}는 이미 처리 중이거나 완료됨. 현재 상태: {}",
                eventId, event.getStatus());
            return;
        }

        event.markAsProcessing();
        failedEventRepository.saveAndFlush(event);

        try {
            // 실제 복구 처리
            executeRecovery(event);

            // 성공 시 상태 변경
            event.markAsProcessed();
            failedEventRepository.save(event);
            log.debug("[실패 이벤트 복구 성공] id={}, userId={}, activityType={}, operationType={}",
                event.getId(), event.getUserId(), event.getActivityType(), event.getOperationType());

        } catch (Exception e) {
            log.error("[실패 이벤트 복구 실패] id={}, retryCount={}/{}, operationType={}",
                event.getId(), event.getRetryCount() + 1, FAILED_EVENT_MAX_RETRY_COUNT,
                event.getOperationType(), e);

            // 재시도 카운트 증가
            event.incrementRetryCount();

            if (event.getRetryCount() >= FAILED_EVENT_MAX_RETRY_COUNT) {
                event.markAsFailed(truncateErrorMessage(e.getMessage()));
            } else {
                event.revertToPending();
                event.setErrorMessage(truncateErrorMessage(e.getMessage()));
            }
            failedEventRepository.save(event);
            throw e;
        }
    }

    /**
     * 실제 복구 로직 - 순수하게 활동 기록/취소만 처리
     * 상태 변경은 호출하는 메서드에서 처리
     */
    private void executeRecovery(FailedActivityEvent event) {
        if (event.getOperationType() == OperationType.RECORD) {
            userActivityService.recordActivity(
                event.getUserId(),
                event.getActivityType(),
                event.getTargetId()
            );
        } else if (event.getOperationType() == OperationType.REVOKE) {
            userActivityService.revokeActivity(
                event.getUserId(),
                event.getActivityType(),
                event.getTargetId()
            );
        }
    }
}
