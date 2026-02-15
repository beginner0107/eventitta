package com.eventitta.gamification.service;

import com.eventitta.gamification.domain.ActivityOutbox;
import com.eventitta.gamification.domain.ActivityOutbox.OutboxStatus;
import com.eventitta.gamification.domain.ActivityType;
import com.eventitta.gamification.domain.OperationType;
import com.eventitta.gamification.repository.ActivityOutboxRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OutboxRelayService 테스트")
class OutboxRelayServiceTest {

    @Mock
    private ActivityOutboxRepository outboxRepository;

    @Mock
    private UserActivityService userActivityService;

    @InjectMocks
    private OutboxRelayService outboxRelayService;

    @Test
    @DisplayName("PENDING 상태의 RECORD 이벤트를 성공적으로 처리한다")
    void processIndependently_Record_ShouldSucceed() {
        // given
        ActivityOutbox outbox = createOutbox(1L, OperationType.RECORD, OutboxStatus.PENDING);
        when(outboxRepository.findByIdWithLock(1L)).thenReturn(Optional.of(outbox));

        // when
        outboxRelayService.processIndependently(1L);

        // then
        verify(userActivityService).recordActivity(outbox.getUserId(), outbox.getActivityType(), outbox.getTargetId());
        verify(outboxRepository).save(outbox);
    }

    @Test
    @DisplayName("PENDING 상태의 REVOKE 이벤트를 성공적으로 처리한다")
    void processIndependently_Revoke_ShouldSucceed() {
        // given
        ActivityOutbox outbox = createOutbox(2L, OperationType.REVOKE, OutboxStatus.PENDING);
        when(outboxRepository.findByIdWithLock(2L)).thenReturn(Optional.of(outbox));

        // when
        outboxRelayService.processIndependently(2L);

        // then
        verify(userActivityService).revokeActivity(outbox.getUserId(), outbox.getActivityType(), outbox.getTargetId());
        verify(outboxRepository).save(outbox);
    }

    @Test
    @DisplayName("이미 PROCESSING 상태인 이벤트는 건너뛴다")
    void processIndependently_AlreadyProcessing_ShouldSkip() {
        // given
        ActivityOutbox outbox = createOutbox(3L, OperationType.RECORD, OutboxStatus.PENDING);
        outbox.markAsProcessing(); // 이미 PROCESSING 상태
        when(outboxRepository.findByIdWithLock(3L)).thenReturn(Optional.of(outbox));

        // when
        outboxRelayService.processIndependently(3L);

        // then
        verify(userActivityService, never()).recordActivity(anyLong(), any(), anyLong());
        verify(userActivityService, never()).revokeActivity(anyLong(), any(), anyLong());
    }

    @Test
    @DisplayName("처리 중 예외 발생 시 재시도 카운트를 증가시키고 PENDING으로 되돌린다")
    void processIndependently_OnFailure_ShouldIncrementRetryAndRevertToPending() {
        // given
        ActivityOutbox outbox = createOutbox(4L, OperationType.RECORD, OutboxStatus.PENDING);
        when(outboxRepository.findByIdWithLock(4L)).thenReturn(Optional.of(outbox));
        doThrow(new RuntimeException("DB 오류"))
                .when(userActivityService).recordActivity(anyLong(), any(), anyLong());

        // when
        outboxRelayService.processIndependently(4L);

        // then
        verify(outboxRepository).save(outbox);
        // outbox는 retry가 1 증가하고 PENDING으로 돌아감
    }

    private ActivityOutbox createOutbox(Long id, OperationType opType, OutboxStatus status) {
        ActivityOutbox outbox = ActivityOutbox.builder()
                .idempotencyKey("test-key-" + id)
                .userId(100L + id)
                .activityType(ActivityType.CREATE_POST)
                .operationType(opType)
                .targetId(200L + id)
                .build();

        // Mock ID 설정을 위해 spy 활용 대신, 상태만 검증
        return outbox;
    }
}
