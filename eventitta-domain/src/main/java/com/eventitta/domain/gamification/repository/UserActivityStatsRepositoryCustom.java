package com.eventitta.domain.gamification.repository;

import com.eventitta.domain.gamification.domain.RewardActionType;

public interface UserActivityStatsRepositoryCustom {

    void increment(Long userId, RewardActionType actionType, int pointsDelta);

    void decrement(Long userId, RewardActionType actionType, int pointsDelta);
}
