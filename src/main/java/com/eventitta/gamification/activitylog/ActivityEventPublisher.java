package com.eventitta.gamification.activitylog;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ActivityEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    public void publish(String activityCode, Long userId, Long targetId) {
        eventPublisher.publishEvent(new UserActivityLogRequestedEvent(userId, activityCode, targetId));
    }

    public void publishRevoke(String activityCode, Long userId, Long targetId) {
        eventPublisher.publishEvent(new UserActivityRevokeRequestedEvent(userId, activityCode, targetId));
    }
}
