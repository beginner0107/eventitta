package com.eventitta.domain.gamification.api.internal.view;

public record ActivitySummaryView(
    String activityType,
    long count,
    long totalPoints
) {
}
