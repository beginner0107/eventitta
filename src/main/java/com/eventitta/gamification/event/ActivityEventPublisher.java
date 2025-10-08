package com.eventitta.gamification.event;

import com.eventitta.gamification.domain.ActivityType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ActivityEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    public void publish(ActivityType activityType, Long userId, Long targetId) {
        eventPublisher.publishEvent(new UserActivityLogRequestedEvent(userId, activityType, targetId));
    }

    public void publishRevoke(ActivityType activityType, Long userId, Long targetId) {
        eventPublisher.publishEvent(new UserActivityRevokeRequestedEvent(userId, activityType, targetId));
    }
}
