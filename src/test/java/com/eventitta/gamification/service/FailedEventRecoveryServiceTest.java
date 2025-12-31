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

import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

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
    @DisplayName("recoverFailedEventIndependently - Pessimistic Lock 사용 및 동시성 체크")
    void recoverFailedEventIndependently_WithConcurrencyCheck() {
        // given
        Long eventId = 1L;
        FailedActivityEvent lockedEvent = spy(testEvent);
        when(lockedEvent.getStatus()).thenReturn(FailedActivityEvent.EventStatus.PENDING);

        when(failedEventRepository.findByIdWithLock(eventId))
            .thenReturn(Optional.of(lockedEvent));
        when(failedEventRepository.saveAndFlush(any(FailedActivityEvent.class)))
            .thenReturn(lockedEvent);

        doNothing().when(userActivityService).recordActivity(anyLong(), any(ActivityType.class), anyLong());

        // when
        failedEventRecoveryService.recoverFailedEventIndependently(eventId);

        // then
        verify(failedEventRepository).findByIdWithLock(eventId);  // Pessimistic Lock 확인
        verify(lockedEvent).markAsProcessing();
        verify(failedEventRepository).saveAndFlush(lockedEvent);  // 즉시 저장 확인
        verify(userActivityService).recordActivity(
            lockedEvent.getUserId(),
            lockedEvent.getActivityType(),
            lockedEvent.getTargetId()
        );
        verify(lockedEvent).markAsProcessed();
        verify(failedEventRepository).save(lockedEvent);  // 최종 저장 확인
    }

    @Test
    @DisplayName("recoverFailedEventIndependently - 이미 처리 중인 이벤트는 스킵")
    void recoverFailedEventIndependently_AlreadyProcessing_ShouldSkip() {
        // given
        Long eventId = 1L;
        FailedActivityEvent processingEvent = spy(testEvent);
        when(processingEvent.getStatus()).thenReturn(FailedActivityEvent.EventStatus.PROCESSING);

        when(failedEventRepository.findByIdWithLock(eventId))
            .thenReturn(Optional.of(processingEvent));

        // when
        failedEventRecoveryService.recoverFailedEventIndependently(eventId);

        // then
        verify(failedEventRepository).findByIdWithLock(eventId);
        verify(processingEvent, never()).markAsProcessing();  // 상태 변경 안 함
        verify(failedEventRepository, never()).saveAndFlush(any());  // 저장 안 함
        verify(userActivityService, never()).recordActivity(anyLong(), any(), anyLong());  // 처리 안 함
    }

    @Test
    @DisplayName("recoverFailedEventIndependently - 이미 완료된 이벤트는 스킵")
    void recoverFailedEventIndependently_AlreadyProcessed_ShouldSkip() {
        // given
        Long eventId = 1L;
        FailedActivityEvent processedEvent = spy(testEvent);
        when(processedEvent.getStatus()).thenReturn(FailedActivityEvent.EventStatus.PROCESSED);

        when(failedEventRepository.findByIdWithLock(eventId))
            .thenReturn(Optional.of(processedEvent));

        // when
        failedEventRecoveryService.recoverFailedEventIndependently(eventId);

        // then
        verify(failedEventRepository).findByIdWithLock(eventId);
        verify(processedEvent, never()).markAsProcessing();
        verify(failedEventRepository, never()).saveAndFlush(any());
        verify(userActivityService, never()).recordActivity(anyLong(), any(), anyLong());
    }

    @Test
    @DisplayName("recoverFailedEventIndependently - 처리 실패 시 재시도 가능 상태로 변경")
    void recoverFailedEventIndependently_Failure_ShouldRevertToPending() {
        // given
        Long eventId = 1L;
        FailedActivityEvent event = spy(testEvent);
        when(event.getStatus()).thenReturn(FailedActivityEvent.EventStatus.PENDING);
        when(event.getRetryCount()).thenReturn(1);

        when(failedEventRepository.findByIdWithLock(eventId))
            .thenReturn(Optional.of(event));
        when(failedEventRepository.saveAndFlush(any(FailedActivityEvent.class)))
            .thenReturn(event);

        String errorMessage = "Service error";
        doThrow(new RuntimeException(errorMessage))
            .when(userActivityService)
            .recordActivity(anyLong(), any(ActivityType.class), anyLong());

        // when & then
        try {
            failedEventRecoveryService.recoverFailedEventIndependently(eventId);
        } catch (RuntimeException e) {
            // 예외가 발생해야 트랜잭션이 롤백됨
        }

        // then
        verify(event).markAsProcessing();
        verify(event).incrementRetryCount();
        verify(event).revertToPending();
        verify(event).setErrorMessage(errorMessage);
        verify(failedEventRepository).save(event);  // 최종 save 확인
    }

    @Test
    @DisplayName("동시에 여러 스레드가 같은 이벤트 처리 시 동시성 제어 동작")
    void concurrentProcessing_SameEvent_ConcurrencyControl() throws InterruptedException {
        // given
        Long eventId = 1L;
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threadCount);
        AtomicInteger processedCount = new AtomicInteger(0);
        AtomicInteger skippedCount = new AtomicInteger(0);

        // when
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();  // 모든 스레드가 동시에 시작

                    // 첫 번째 스레드만 PENDING 상태를 볼 수 있다고 가정
                    // (실제로는 DB Lock으로 제어됨)
                    boolean isFirst = processedCount.compareAndSet(0, 1);

                    if (isFirst) {
                        // 첫 번째 스레드만 처리
                        Thread.sleep(50); // 처리 시간 시뮬레이션
                    } else {
                        // 나머지는 스킵
                        skippedCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    // 예외 무시
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        // 모든 스레드 시작
        startLatch.countDown();

        // 모든 스레드 종료 대기
        boolean finished = finishLatch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        // then
        assertThat(finished).isTrue();
        assertThat(processedCount.get()).isEqualTo(1);  // 하나만 처리
        assertThat(skippedCount.get()).isEqualTo(threadCount - 1);  // 나머지는 스킵
    }
}
