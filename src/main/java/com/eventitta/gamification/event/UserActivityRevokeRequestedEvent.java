package com.eventitta.gamification.event;

import com.eventitta.gamification.domain.ActivityType;

public record UserActivityRevokeRequestedEvent(
    Long userId,
    ActivityType activityType,
    Long targetId
) {
}
