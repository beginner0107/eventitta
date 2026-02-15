package com.eventitta.gamification.event;

import com.eventitta.gamification.domain.OperationType;
import com.eventitta.gamification.service.ActivityOutboxWriter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.eventitta.gamification.domain.ActivityType.CREATE_POST;
import static com.eventitta.gamification.domain.ActivityType.DELETE_POST;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("사용자 활동 이벤트 퍼블리셔의 동작을 검증하는 테스트")
class ActivityEventPublisherTest {

    @Mock
    private ActivityOutboxWriter activityOutboxWriter;

    @InjectMocks
    private ActivityEventPublisher eventPublisher;

    @Test
    @DisplayName("활동 코드, 사용자 ID, 대상 ID를 전달하면 아웃박스에 RECORD 이벤트가 기록된다")
    void givenCodeUserIdTargetId_whenPublish_thenOutboxRecordWritten() {
        // when
        eventPublisher.publish(CREATE_POST, 1L, 2L);

        // then
        verify(activityOutboxWriter).write(CREATE_POST, 1L, 2L, OperationType.RECORD);
    }

    @Test
    @DisplayName("활동 코드, 사용자 ID, 대상 ID를 전달하면 아웃박스에 REVOKE 이벤트가 기록된다")
    void givenCodeUserIdTargetId_whenPublishRevoke_thenOutboxRevokeWritten() {
        // when
        eventPublisher.publishRevoke(DELETE_POST, 1L, 2L);

        // then
        verify(activityOutboxWriter).write(DELETE_POST, 1L, 2L, OperationType.REVOKE);
    }
}
