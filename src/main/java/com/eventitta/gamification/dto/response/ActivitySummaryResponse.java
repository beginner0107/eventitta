package com.eventitta.gamification.dto.response;

public record ActivitySummaryResponse(
    String activityType,
    long count
) {
    public static ActivitySummaryResponse from(String activityType, long count) {
        return new ActivitySummaryResponse(activityType, count);
    }
}
