package com.eventitta.gamification.event;

import com.eventitta.gamification.domain.ActivityType;

public record UserActivityLogRequestedEvent(
    Long userId,
    ActivityType activityType,
    Long targetId
) {
}
