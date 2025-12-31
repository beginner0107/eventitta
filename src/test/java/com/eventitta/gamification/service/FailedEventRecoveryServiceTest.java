package com.eventitta.gamification.service;

import com.eventitta.gamification.domain.ActivityType;
import com.eventitta.gamification.domain.FailedActivityEvent;
import com.eventitta.gamification.domain.OperationType;
import com.eventitta.gamification.repository.FailedActivityEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.eventitta.gamification.constants.GamificationRetryConstants.FAILED_EVENT_MAX_ERROR_MESSAGE_LENGTH;
import static com.eventitta.gamification.constants.GamificationRetryConstants.FAILED_EVENT_MAX_RETRY_COUNT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FailedEventRecoveryServiceTest {

    @Mock
    private FailedActivityEventRepository failedEventRepository;

    @Mock
    private UserActivityService userActivityService;

    @InjectMocks
    private FailedEventRecoveryService failedEventRecoveryService;

    private FailedActivityEvent testEvent;

    @BeforeEach
    void setUp() {
        testEvent = FailedActivityEvent.builder()
                .userId(1L)
                .activityType(ActivityType.CREATE_POST)
                .operationType(OperationType.RECORD)
                .targetId(100L)
                .errorMessage("Test error")
                .build();
    }

    @Test
    @DisplayName("실패 이벤트를 정상적으로 저장한다")
    void saveFailedEvent_ShouldSaveEventSuccessfully() {
        // given
        Long userId = 1L;
        ActivityType activityType = ActivityType.CREATE_POST;
        OperationType operationType = OperationType.RECORD;
        Long targetId = 100L;
        String errorMessage = "Test error message";

        // when
        failedEventRecoveryService.saveFailedEvent(userId, activityType, operationType, targetId, errorMessage);

        // then
        ArgumentCaptor<FailedActivityEvent> eventCaptor = ArgumentCaptor.forClass(FailedActivityEvent.class);
        verify(failedEventRepository).save(eventCaptor.capture());

        FailedActivityEvent savedEvent = eventCaptor.getValue();
        assertThat(savedEvent.getUserId()).isEqualTo(userId);
        assertThat(savedEvent.getActivityType()).isEqualTo(activityType);
        assertThat(savedEvent.getOperationType()).isEqualTo(operationType);
        assertThat(savedEvent.getTargetId()).isEqualTo(targetId);
        assertThat(savedEvent.getErrorMessage()).isEqualTo(errorMessage);
    }

    @Test
    @DisplayName("긴 에러 메시지는 최대 길이로 잘린다")
    void saveFailedEvent_WithLongErrorMessage_ShouldTruncate() {
        // given
        String longErrorMessage = "a".repeat(FAILED_EVENT_MAX_ERROR_MESSAGE_LENGTH + 100);

        // when
        failedEventRecoveryService.saveFailedEvent(
                1L,
                ActivityType.CREATE_POST,
                OperationType.RECORD,
                100L,
                longErrorMessage
        );

        // then
        ArgumentCaptor<FailedActivityEvent> eventCaptor = ArgumentCaptor.forClass(FailedActivityEvent.class);
        verify(failedEventRepository).save(eventCaptor.capture());

        FailedActivityEvent savedEvent = eventCaptor.getValue();
        assertThat(savedEvent.getErrorMessage()).hasSize(FAILED_EVENT_MAX_ERROR_MESSAGE_LENGTH);
    }

    @Test
    @DisplayName("null 에러 메시지를 처리할 수 있다")
    void saveFailedEvent_WithNullErrorMessage_ShouldHandleGracefully() {
        // when
        failedEventRecoveryService.saveFailedEvent(
                1L,
                ActivityType.CREATE_POST,
                OperationType.RECORD,
                100L,
                null
        );

        // then
        ArgumentCaptor<FailedActivityEvent> eventCaptor = ArgumentCaptor.forClass(FailedActivityEvent.class);
        verify(failedEventRepository).save(eventCaptor.capture());

        FailedActivityEvent savedEvent = eventCaptor.getValue();
        assertThat(savedEvent.getErrorMessage()).isNull();
    }

    @Test
    @DisplayName("RECORD 타입 이벤트 복구 성공 시나리오")
    void recoverFailedEvent_RecordType_Success() {
        // given
        testEvent = spy(testEvent);
        doNothing().when(userActivityService).recordActivity(anyLong(), any(ActivityType.class), anyLong());

        // when
        failedEventRecoveryService.recoverFailedEvent(testEvent);

        // then
        verify(testEvent).markAsProcessing();
        verify(testEvent).incrementRetryCount();
        verify(userActivityService).recordActivity(
                testEvent.getUserId(),
                testEvent.getActivityType(),
                testEvent.getTargetId()
        );
        verify(testEvent).markAsProcessed();
        verify(testEvent, never()).markAsFailed(any());
        verify(testEvent, never()).revertToPending();
    }

    @Test
    @DisplayName("REVOKE 타입 이벤트 복구 성공 시나리오")
    void recoverFailedEvent_RevokeType_Success() {
        // given
        testEvent = FailedActivityEvent.builder()
                .userId(1L)
                .activityType(ActivityType.CREATE_POST)
                .operationType(OperationType.REVOKE)
                .targetId(100L)
                .errorMessage("Test error")
                .build();
        testEvent = spy(testEvent);

        doNothing().when(userActivityService).revokeActivity(anyLong(), any(ActivityType.class), anyLong());

        // when
        failedEventRecoveryService.recoverFailedEvent(testEvent);

        // then
        verify(testEvent).markAsProcessing();
        verify(testEvent).incrementRetryCount();
        verify(userActivityService).revokeActivity(
                testEvent.getUserId(),
                testEvent.getActivityType(),
                testEvent.getTargetId()
        );
        verify(testEvent).markAsProcessed();
        verify(testEvent, never()).markAsFailed(any());
        verify(testEvent, never()).revertToPending();
    }

    @Test
    @DisplayName("이벤트 복구 실패 시 재시도 가능한 경우 PENDING으로 되돌린다")
    void recoverFailedEvent_Failure_WithRetryRemaining() {
        // given
        testEvent = spy(testEvent);
        when(testEvent.getRetryCount()).thenReturn(1);

        doThrow(new RuntimeException("Service error"))
                .when(userActivityService)
                .recordActivity(anyLong(), any(ActivityType.class), anyLong());

        // when
        failedEventRecoveryService.recoverFailedEvent(testEvent);

        // then
        verify(testEvent).markAsProcessing();
        verify(testEvent).incrementRetryCount();
        verify(testEvent, never()).markAsProcessed();
        verify(testEvent, never()).markAsFailed(any());
        verify(testEvent).revertToPending();
    }

    @Test
    @DisplayName("최대 재시도 횟수 도달 시 FAILED로 마킹한다")
    void recoverFailedEvent_MaxRetryReached_MarkAsFailed() {
        // given
        testEvent = spy(testEvent);
        when(testEvent.getRetryCount()).thenReturn(FAILED_EVENT_MAX_RETRY_COUNT);

        String errorMessage = "Service error";
        doThrow(new RuntimeException(errorMessage))
                .when(userActivityService)
                .recordActivity(anyLong(), any(ActivityType.class), anyLong());

        // when
        failedEventRecoveryService.recoverFailedEvent(testEvent);

        // then
        verify(testEvent).markAsProcessing();
        verify(testEvent).incrementRetryCount();
        verify(testEvent, never()).markAsProcessed();
        verify(testEvent).markAsFailed(errorMessage);
        verify(testEvent, never()).revertToPending();
    }

    @Test
    @DisplayName("복구 실패 시 긴 에러 메시지는 잘린다")
    void recoverFailedEvent_WithLongErrorMessage_ShouldTruncate() {
        // given
        testEvent = spy(testEvent);
        when(testEvent.getRetryCount()).thenReturn(FAILED_EVENT_MAX_RETRY_COUNT);

        String longErrorMessage = "a".repeat(FAILED_EVENT_MAX_ERROR_MESSAGE_LENGTH + 100);
        doThrow(new RuntimeException(longErrorMessage))
                .when(userActivityService)
                .recordActivity(anyLong(), any(ActivityType.class), anyLong());

        // when
        failedEventRecoveryService.recoverFailedEvent(testEvent);

        // then
        ArgumentCaptor<String> errorCaptor = ArgumentCaptor.forClass(String.class);
        verify(testEvent).markAsFailed(errorCaptor.capture());

        String capturedError = errorCaptor.getValue();
        assertThat(capturedError).hasSize(FAILED_EVENT_MAX_ERROR_MESSAGE_LENGTH);
    }

    @Test
    @DisplayName("복구 중 null 에러 메시지 처리")
    void recoverFailedEvent_WithNullErrorMessage_ShouldHandleGracefully() {
        // given
        testEvent = spy(testEvent);
        when(testEvent.getRetryCount()).thenReturn(FAILED_EVENT_MAX_RETRY_COUNT);

        doThrow(new RuntimeException())  // null message exception
                .when(userActivityService)
                .recordActivity(anyLong(), any(ActivityType.class), anyLong());

        // when
        failedEventRecoveryService.recoverFailedEvent(testEvent);

        // then
        verify(testEvent).markAsFailed(null);
    }
}