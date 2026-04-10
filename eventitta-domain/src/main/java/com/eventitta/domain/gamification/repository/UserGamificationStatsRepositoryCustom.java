package com.eventitta.domain.gamification.repository;

public interface UserGamificationStatsRepositoryCustom {

    void increment(Long userId, int pointsDelta, long activityCountDelta);

    void decrement(Long userId, int pointsDelta, long activityCountDelta);
}
