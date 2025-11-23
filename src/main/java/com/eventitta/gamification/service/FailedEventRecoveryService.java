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

    @Transactional
    public void recoverFailedEvent(FailedActivityEvent event) {
        try {
            event.markAsProcessing();
            event.incrementRetryCount();

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

            event.markAsProcessed();
            log.debug("[실패 이벤트 복구 성공] id={}, userId={}, activityType={}, operationType={}",
                event.getId(), event.getUserId(), event.getActivityType(), event.getOperationType());

        } catch (Exception e) {
            log.error("[실패 이벤트 복구 실패] id={}, retryCount={}/{}, operationType={}",
                event.getId(), event.getRetryCount(), FAILED_EVENT_MAX_RETRY_COUNT,
                event.getOperationType(), e);

            if (event.getRetryCount() >= FAILED_EVENT_MAX_RETRY_COUNT) {
                event.markAsFailed(truncateErrorMessage(e.getMessage()));
            } else {
                event.revertToPending();
            }
        }
    }
}
