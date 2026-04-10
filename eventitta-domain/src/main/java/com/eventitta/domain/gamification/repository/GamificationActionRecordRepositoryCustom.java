package com.eventitta.domain.gamification.repository;

import com.eventitta.domain.gamification.domain.ActivityType;
import com.eventitta.domain.gamification.domain.ResourceType;

public interface GamificationActionRecordRepositoryCustom {

    boolean insertGrantIfAbsent(
        Long userId,
        ActivityType activityType,
        ResourceType resourceType,
        Long targetId,
        int pointsEarned
    );
}
