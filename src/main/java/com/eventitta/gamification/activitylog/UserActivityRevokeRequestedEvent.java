package com.eventitta.gamification.activitylog;

public record UserActivityRevokeRequestedEvent(
    Long userId,
    String activityCode,
    Long targetId
) {
}
