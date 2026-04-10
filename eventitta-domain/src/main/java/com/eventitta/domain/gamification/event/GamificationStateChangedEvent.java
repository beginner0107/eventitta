package com.eventitta.domain.gamification.event;

import com.eventitta.domain.gamification.domain.RewardActionType;

public record GamificationStateChangedEvent(
    Long userId,
    RewardActionType actionType,
    int totalPoints,
    long totalActivityCount,
    long actionCount,
    int actionPointsTotal
) {
}
