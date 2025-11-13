package com.eventitta.gamification.dto.projection;

import com.eventitta.gamification.domain.ActivityType;

public interface ActivitySummaryProjection {
    ActivityType getActivityType();

    long getCount();
}
