package com.eventitta.domain.gamification.dto.projection;

import com.eventitta.domain.gamification.domain.ActivityType;

public interface ActivitySummaryProjection {
    ActivityType getActivityType();

    long getCount();

    long getTotalPoints();
}
