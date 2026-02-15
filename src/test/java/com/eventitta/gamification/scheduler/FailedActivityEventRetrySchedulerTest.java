package com.eventitta.gamification.scheduler;

import com.eventitta.gamification.domain.ActivityType;
import com.eventitta.gamification.domain.FailedActivityEvent;
import com.eventitta.gamification.domain.FailedActivityEvent.EventStatus;
import com.eventitta.gamification.domain.OperationType;
import com.eventitta.gamification.repository.FailedActivityEventRepository;
import com.eventitta.gamification.service.FailedEventRecoveryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static com.eventitta.gamification.constants.GamificationRetryConstants.FAILED_EVENT_MAX_RETRY_COUNT;
import static com.eventitta.gamification.constants.GamificationRetryConstants.FAILED_EVENT_RETRY_BATCH_SIZE;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FailedActivityEventRetrySchedulerTest {

        @Mock
        private FailedActivityEventRepository failedActivityEventRepository;

        @Mock
        private FailedEventRecoveryService failedEventRecoveryService;

        @InjectMocks
        private FailedActivityEventRetryScheduler scheduler;

        private List<FailedActivityEvent> testEvents;

        @BeforeEach
        void setUp() {
                testEvents = List.of(
                                createFailedEventWithId(1L, 1L, ActivityType.CREATE_POST, 0),
                                createFailedEventWithId(2L, 2L, ActivityType.CREATE_COMMENT, 1),
                                createFailedEventWithId(3L, 3L, ActivityType.JOIN_MEETING, 2));

                // 통계 로깅 메서드를 위한 기본 stub
                lenient().when(failedActivityEventRepository.countByStatus(any(EventStatus.class))).thenReturn(0L);
        }

        @Nested
        @DisplayName("retryFailedEvents - 실패 이벤트 재처리")
        class RetryFailedEvents {

        @Test
        @DisplayName("펜딩 상태의 실패 이벤트가 있으면 복구를 시도한다")
        void retryFailedEvents_WithPendingEvents_ShouldProcessEvents() {
            // given
            when(failedActivityEventRepository.findByStatusAndRetryCountLessThan(
                    eq(EventStatus.PENDING), eq(FAILED_EVENT_MAX_RETRY_COUNT)))
                    .thenReturn(testEvents);

            // when
            scheduler.retryFailedEvents();

            // then
            verify(failedActivityEventRepository).findByStatusAndRetryCountLessThan(
                    EventStatus.PENDING, FAILED_EVENT_MAX_RETRY_COUNT);

            // 배치 크기만큼만 처리되어야 함
            int expectedCallCount = Math.min(testEvents.size(), FAILED_EVENT_RETRY_BATCH_SIZE);
            verify(failedEventRecoveryService, times(expectedCallCount))
                    .recoverFailedEventIndependently(anyLong());
        }

        @Test
        @DisplayName("펜딩 상태의 실패 이벤트가 없으면 처리를 건너뛴다")
        void retryFailedEvents_NoPendingEvents_ShouldSkip() {
            // given
            when(failedActivityEventRepository.findByStatusAndRetryCountLessThan(
                    eq(EventStatus.PENDING), eq(FAILED_EVENT_MAX_RETRY_COUNT)))
                    .thenReturn(Collections.emptyList());

            // when
            scheduler.retryFailedEvents();

            // then
            verify(failedActivityEventRepository).findByStatusAndRetryCountLessThan(
                    EventStatus.PENDING, FAILED_EVENT_MAX_RETRY_COUNT);
            verify(failedEventRecoveryService, never()).recoverFailedEventIndependently(anyLong());
        }

                @Test
                @DisplayName("배치 크기보다 많은 이벤트가 있어도 배치 크기만큼만 처리한다")
                void retryFailedEvents_MoreThanBatchSize_ShouldProcessBatchSizeOnly() {
                        // given
                        List<FailedActivityEvent> manyEvents = createManyFailedEventsWithId(
                                        FAILED_EVENT_RETRY_BATCH_SIZE + 10);

                        when(failedActivityEventRepository.findByStatusAndRetryCountLessThan(
                                        eq(EventStatus.PENDING), eq(FAILED_EVENT_MAX_RETRY_COUNT)))
                                        .thenReturn(manyEvents);

                        // when
                        scheduler.retryFailedEvents();

                        // then
                        verify(failedEventRecoveryService, times(FAILED_EVENT_RETRY_BATCH_SIZE))
                                        .recoverFailedEventIndependently(anyLong());
                }

        @Test
        @DisplayName("이벤트 처리 중 예외가 발생해도 스케줄러는 계속 동작한다")
        void retryFailedEvents_WithException_ShouldContinue() {
            // given
            when(failedActivityEventRepository.findByStatusAndRetryCountLessThan(
                    eq(EventStatus.PENDING), eq(FAILED_EVENT_MAX_RETRY_COUNT)))
                    .thenReturn(testEvents);

            doThrow(new RuntimeException("Processing error"))
                    .when(failedEventRecoveryService)
                    .recoverFailedEventIndependently(1L);

            // when
            scheduler.retryFailedEvents();

            // then
            verify(failedActivityEventRepository).findByStatusAndRetryCountLessThan(
                    EventStatus.PENDING, FAILED_EVENT_MAX_RETRY_COUNT);

            verify(failedEventRecoveryService, atLeast(1))
                    .recoverFailedEventIndependently(anyLong());
        }

        @Test
        @DisplayName("재시도 횟수가 최대치를 초과한 이벤트는 조회되지 않는다")
        void retryFailedEvents_MaxRetryCountFilter() {
            // given
            when(failedActivityEventRepository.findByStatusAndRetryCountLessThan(
                    eq(EventStatus.PENDING), eq(FAILED_EVENT_MAX_RETRY_COUNT)))
                    .thenReturn(Collections.emptyList());

            // when
            scheduler.retryFailedEvents();

            // then
            verify(failedActivityEventRepository).findByStatusAndRetryCountLessThan(
                    EventStatus.PENDING, FAILED_EVENT_MAX_RETRY_COUNT);
            verify(failedEventRecoveryService, never()).recoverFailedEventIndependently(anyLong());
        }

                @Test
                @DisplayName("복구 서비스 호출 시 정확한 이벤트가 전달된다")
                void retryFailedEvents_VerifyExactEventsProcessed() {
                        // given
                        FailedActivityEvent specificEvent = createFailedEventWithId(99L, 99L, ActivityType.CREATE_POST,
                                        0);
                        when(failedActivityEventRepository.findByStatusAndRetryCountLessThan(
                                        eq(EventStatus.PENDING), eq(FAILED_EVENT_MAX_RETRY_COUNT)))
                                        .thenReturn(List.of(specificEvent));

                        // when
                        scheduler.retryFailedEvents();

                        // then
                        verify(failedEventRecoveryService, times(1)).recoverFailedEventIndependently(99L);
                }
        }

        @Nested
        @DisplayName("recoverStuckProcessingEvents - PROCESSING stuck 복구")
        class RecoverStuckProcessingEvents {

        @Test
        @DisplayName("stuck된 PROCESSING 이벤트가 있으면 PENDING으로 되돌린다")
        void recoverStuckProcessingEvents_WithStuckEvents_ShouldRevert() {
            // given
            when(failedActivityEventRepository.revertStuckProcessing(any(LocalDateTime.class)))
                    .thenReturn(3);

            // when
            scheduler.recoverStuckProcessingEvents();

            // then
            verify(failedActivityEventRepository).revertStuckProcessing(any(LocalDateTime.class));
        }

        @Test
        @DisplayName("stuck된 PROCESSING 이벤트가 없으면 아무 작업도 하지 않는다")
        void recoverStuckProcessingEvents_NoStuckEvents_ShouldDoNothing() {
            // given
            when(failedActivityEventRepository.revertStuckProcessing(any(LocalDateTime.class)))
                    .thenReturn(0);

            // when
            scheduler.recoverStuckProcessingEvents();

            // then
            verify(failedActivityEventRepository).revertStuckProcessing(any(LocalDateTime.class));
        }

        @Test
        @DisplayName("타임아웃 기준 시간이 올바르게 전달된다")
        void recoverStuckProcessingEvents_ShouldUseCorrectThreshold() {
            // given
            when(failedActivityEventRepository.revertStuckProcessing(any(LocalDateTime.class)))
                    .thenReturn(0);

            LocalDateTime before = LocalDateTime.now().minusMinutes(6);

            // when
            scheduler.recoverStuckProcessingEvents();

            // then
            verify(failedActivityEventRepository).revertStuckProcessing(argThat(threshold ->
                    threshold.isAfter(before) && threshold.isBefore(LocalDateTime.now())
            ));
        }
        }

        @Nested
        @DisplayName("logFailedEventStatistics - 통계 로깅")
        class LogFailedEventStatistics {

        @Test
        @DisplayName("FAILED 또는 PROCESSING 이벤트가 있으면 통계가 조회된다")
        void retryFailedEvents_WithFailedEvents_ShouldLogStatistics() {
            // given
            when(failedActivityEventRepository.countByStatus(EventStatus.PENDING)).thenReturn(2L);
            when(failedActivityEventRepository.countByStatus(EventStatus.PROCESSING)).thenReturn(1L);
            when(failedActivityEventRepository.countByStatus(EventStatus.FAILED)).thenReturn(3L);
            when(failedActivityEventRepository.findByStatusAndRetryCountLessThan(
                    eq(EventStatus.PENDING), eq(FAILED_EVENT_MAX_RETRY_COUNT)))
                    .thenReturn(Collections.emptyList());

            // when
            scheduler.retryFailedEvents();

            // then - 통계 쿼리가 호출되었는지 확인
            verify(failedActivityEventRepository).countByStatus(EventStatus.PENDING);
            verify(failedActivityEventRepository).countByStatus(EventStatus.PROCESSING);
            verify(failedActivityEventRepository).countByStatus(EventStatus.FAILED);
        }

        @Test
        @DisplayName("모든 상태가 0이면 통계 쿼리만 호출된다")
        void retryFailedEvents_AllZero_ShouldStillQueryStatistics() {
            // given
            when(failedActivityEventRepository.countByStatus(any(EventStatus.class))).thenReturn(0L);
            when(failedActivityEventRepository.findByStatusAndRetryCountLessThan(
                    eq(EventStatus.PENDING), eq(FAILED_EVENT_MAX_RETRY_COUNT)))
                    .thenReturn(Collections.emptyList());

            // when
            scheduler.retryFailedEvents();

            // then
            verify(failedActivityEventRepository, times(3)).countByStatus(any(EventStatus.class));
        }
        }

        private FailedActivityEvent createFailedEvent(Long id, Long userId, ActivityType activityType, int retryCount) {
                FailedActivityEvent event = FailedActivityEvent.builder()
                                .userId(userId)
                                .activityType(activityType)
                                .operationType(OperationType.RECORD)
                                .targetId(100L + id)
                                .errorMessage("Test error " + id)
                                .build();

                // 테스트용으로 retryCount 설정
                for (int i = 0; i < retryCount; i++) {
                        event.incrementRetryCount();
                }

                return event;
        }

        private List<FailedActivityEvent> createManyFailedEvents(int count) {
                return java.util.stream.IntStream.range(0, count)
                                .mapToObj(i -> createFailedEvent((long) i, (long) i, ActivityType.CREATE_POST, 0))
                                .collect(java.util.stream.Collectors.toList());
        }

        private FailedActivityEvent createFailedEventWithId(Long id, Long userId, ActivityType activityType,
                        int retryCount) {
                FailedActivityEvent event = mock(FailedActivityEvent.class);
                lenient().when(event.getId()).thenReturn(id);
                lenient().when(event.getUserId()).thenReturn(userId);
                lenient().when(event.getActivityType()).thenReturn(activityType);
                lenient().when(event.getOperationType()).thenReturn(OperationType.RECORD);
                lenient().when(event.getTargetId()).thenReturn(100L + id);
                lenient().when(event.getRetryCount()).thenReturn(retryCount);
                return event;
        }

        private List<FailedActivityEvent> createManyFailedEventsWithId(int count) {
                return java.util.stream.IntStream.range(0, count)
                                .mapToObj(i -> createFailedEventWithId((long) i, (long) i, ActivityType.CREATE_POST, 0))
                                .collect(java.util.stream.Collectors.toList());
        }
}
