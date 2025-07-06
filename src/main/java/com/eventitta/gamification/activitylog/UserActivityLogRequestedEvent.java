package com.eventitta.gamification.activitylog;

public record UserActivityLogRequestedEvent(
    Long userId,
    String activityCode,
    Long targetId
) {
}
