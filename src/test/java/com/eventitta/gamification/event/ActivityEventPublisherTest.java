package com.eventitta.gamification.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import static com.eventitta.gamification.domain.ActivityType.CREATE_POST;
import static com.eventitta.gamification.domain.ActivityType.DELETE_POST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@DisplayName("사용자 활동 이벤트 퍼블리셔의 동작을 검증하는 테스트")
class ActivityEventPublisherTest {

    @Test
    @DisplayName("활동 코드, 사용자 ID, 대상 ID를 전달하면 사용자 활동 로그 이벤트가 정상적으로 발행된다")
    void givenCodeUserIdTargetId_whenPublish_thenUserActivityLogRequestedEventIsPublished() {
        ApplicationEventPublisher mockPublisher = mock(ApplicationEventPublisher.class);
        ActivityEventPublisher eventPublisher = new ActivityEventPublisher(mockPublisher);

        eventPublisher.publish(CREATE_POST, 1L, 2L);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(mockPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue())
            .isInstanceOf(UserActivityLogRequestedEvent.class);
    }

    @Test
    @DisplayName("활동 코드, 사용자 ID, 대상 ID를 전달하면 사용자 활동 취소 이벤트가 정상적으로 발행된다")
    void givenCodeUserIdTargetId_whenPublishRevoke_thenUserActivityRevokeRequestedEventIsPublished() {
        ApplicationEventPublisher mockPublisher = mock(ApplicationEventPublisher.class);
        ActivityEventPublisher eventPublisher = new ActivityEventPublisher(mockPublisher);

        eventPublisher.publishRevoke(DELETE_POST, 1L, 2L);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(mockPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue())
            .isInstanceOf(UserActivityRevokeRequestedEvent.class);
    }
}
