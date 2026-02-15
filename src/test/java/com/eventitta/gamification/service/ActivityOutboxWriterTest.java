package com.eventitta.gamification.service;

import com.eventitta.gamification.domain.ActivityOutbox;
import com.eventitta.gamification.domain.ActivityOutbox.OutboxStatus;
import com.eventitta.gamification.domain.ActivityType;
import com.eventitta.gamification.domain.OperationType;
import com.eventitta.gamification.repository.ActivityOutboxRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("ActivityOutboxWriter 테스트")
class ActivityOutboxWriterTest {

    @Mock
    private ActivityOutboxRepository activityOutboxRepository;

    @InjectMocks
    private ActivityOutboxWriter activityOutboxWriter;

    @Test
    @DisplayName("RECORD 이벤트를 아웃박스에 정상적으로 기록한다")
    void write_Record_ShouldSaveOutbox() {
        // when
        activityOutboxWriter.write(ActivityType.CREATE_POST, 1L, 100L, OperationType.RECORD);

        // then
        ArgumentCaptor<ActivityOutbox> captor = ArgumentCaptor.forClass(ActivityOutbox.class);
        verify(activityOutboxRepository).save(captor.capture());

        ActivityOutbox saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(1L);
        assertThat(saved.getActivityType()).isEqualTo(ActivityType.CREATE_POST);
        assertThat(saved.getOperationType()).isEqualTo(OperationType.RECORD);
        assertThat(saved.getTargetId()).isEqualTo(100L);
        assertThat(saved.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(saved.getRetryCount()).isZero();
        assertThat(saved.getIdempotencyKey()).isNotBlank();
    }

    @Test
    @DisplayName("REVOKE 이벤트를 아웃박스에 정상적으로 기록한다")
    void write_Revoke_ShouldSaveOutbox() {
        // when
        activityOutboxWriter.write(ActivityType.DELETE_POST, 2L, 200L, OperationType.REVOKE);

        // then
        ArgumentCaptor<ActivityOutbox> captor = ArgumentCaptor.forClass(ActivityOutbox.class);
        verify(activityOutboxRepository).save(captor.capture());

        ActivityOutbox saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(2L);
        assertThat(saved.getActivityType()).isEqualTo(ActivityType.DELETE_POST);
        assertThat(saved.getOperationType()).isEqualTo(OperationType.REVOKE);
        assertThat(saved.getTargetId()).isEqualTo(200L);
        assertThat(saved.getStatus()).isEqualTo(OutboxStatus.PENDING);
    }

    @Test
    @DisplayName("멱등키가 고유하게 생성된다")
    void write_ShouldGenerateUniqueIdempotencyKeys() {
        // when
        activityOutboxWriter.write(ActivityType.CREATE_POST, 1L, 100L, OperationType.RECORD);
        activityOutboxWriter.write(ActivityType.CREATE_POST, 1L, 100L, OperationType.RECORD);

        // then
        ArgumentCaptor<ActivityOutbox> captor = ArgumentCaptor.forClass(ActivityOutbox.class);
        verify(activityOutboxRepository, org.mockito.Mockito.times(2)).save(captor.capture());

        String key1 = captor.getAllValues().get(0).getIdempotencyKey();
        String key2 = captor.getAllValues().get(1).getIdempotencyKey();
        assertThat(key1).isNotEqualTo(key2);
    }
}
