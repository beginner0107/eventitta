package com.eventitta.gamification.dto.response;

public record ActivitySummaryResponse(
    String activityType,
    long count,
    long totalPoints
) {
    public static ActivitySummaryResponse from(String activityType, long count, long totalPoints) {
        return new ActivitySummaryResponse(activityType, count, totalPoints);
    }
}
