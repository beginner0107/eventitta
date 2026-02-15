package com.eventitta.gamification.scheduler;

import com.eventitta.gamification.domain.ActivityOutbox;
import com.eventitta.gamification.domain.ActivityOutbox.OutboxStatus;
import com.eventitta.gamification.domain.ActivityType;
import com.eventitta.gamification.domain.OperationType;
import com.eventitta.gamification.repository.ActivityOutboxRepository;
import com.eventitta.gamification.service.OutboxRelayService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OutboxRelayScheduler 테스트")
class OutboxRelaySchedulerTest {

    @Mock
    private ActivityOutboxRepository outboxRepository;

    @Mock
    private OutboxRelayService outboxRelayService;

    @InjectMocks
    private OutboxRelayScheduler scheduler;

    @Nested
    @DisplayName("relay - 아웃박스 릴레이")
    class Relay {

        @Test
        @DisplayName("PENDING 이벤트가 있으면 처리를 시도한다")
        void relay_WithPendingEvents_ShouldProcess() {
            // given
            List<ActivityOutbox> events = List.of(
                    createMockOutbox(1L),
                    createMockOutbox(2L));
            when(outboxRepository.findPendingEvents(eq(OutboxStatus.PENDING), anyInt(), any(Pageable.class)))
                    .thenReturn(events);

            // when
            scheduler.relay();

            // then
            verify(outboxRelayService, times(2)).processIndependently(anyLong());
        }

        @Test
        @DisplayName("PENDING 이벤트가 없으면 처리를 건너뛴다")
        void relay_NoPendingEvents_ShouldSkip() {
            // given
            when(outboxRepository.findPendingEvents(eq(OutboxStatus.PENDING), anyInt(), any(Pageable.class)))
                .thenReturn(Collections.emptyList());

            // when
            scheduler.relay();

            // then
            verify(outboxRelayService, never()).processIndependently(anyLong());
        }

        @Test
        @DisplayName("개별 이벤트 처리 실패해도 나머지는 계속 처리한다")
        void relay_WithFailure_ShouldContinue() {
            // given
            List<ActivityOutbox> events = List.of(
                    createMockOutbox(1L),
                    createMockOutbox(2L),
                    createMockOutbox(3L));
            when(outboxRepository.findPendingEvents(eq(OutboxStatus.PENDING), anyInt(), any(Pageable.class)))
                    .thenReturn(events);
            doThrow(new RuntimeException("오류")).when(outboxRelayService).processIndependently(1L);

            // when
            scheduler.relay();

            // then
            verify(outboxRelayService, times(3)).processIndependently(anyLong());
        }
    }

    @Nested
    @DisplayName("recoverStuckProcessing - stuck 복구")
    class RecoverStuckProcessing {

        @Test
        @DisplayName("stuck된 PROCESSING 이벤트를 PENDING으로 되돌린다")
        void recoverStuckProcessing_ShouldRevert() {
            // given
            when(outboxRepository.revertStuckProcessing(any(LocalDateTime.class))).thenReturn(2);

            // when
            scheduler.recoverStuckProcessing();

            // then
            verify(outboxRepository).revertStuckProcessing(any(LocalDateTime.class));
        }
    }

    @Nested
    @DisplayName("cleanupProcessedRecords - housekeeping")
    class CleanupProcessedRecords {

        @Test
        @DisplayName("처리 완료된 레코드를 정리한다")
        void cleanupProcessedRecords_ShouldDelete() {
            // given
            when(outboxRepository.deleteProcessedBefore(any(LocalDateTime.class))).thenReturn(5);

            // when
            scheduler.cleanupProcessedRecords();

            // then
            verify(outboxRepository).deleteProcessedBefore(any(LocalDateTime.class));
        }
    }

    private ActivityOutbox createMockOutbox(Long id) {
        ActivityOutbox outbox = mock(ActivityOutbox.class);
        lenient().when(outbox.getId()).thenReturn(id);
        lenient().when(outbox.getUserId()).thenReturn(100L + id);
        lenient().when(outbox.getActivityType()).thenReturn(ActivityType.CREATE_POST);
        lenient().when(outbox.getOperationType()).thenReturn(OperationType.RECORD);
        lenient().when(outbox.getTargetId()).thenReturn(200L + id);
        lenient().when(outbox.getStatus()).thenReturn(OutboxStatus.PENDING);
        return outbox;
    }
}
