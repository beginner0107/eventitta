package com.eventitta.gamification.dto.query;

import com.eventitta.gamification.domain.ActivityType;

public interface ActivitySummary {
    ActivityType getActivityType();

    long getCount();
}
