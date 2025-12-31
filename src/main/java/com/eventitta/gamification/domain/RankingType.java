package com.eventitta.gamification.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 순위 타입 정의
 * Redis zSet 키와 표시명을 관리
 */
@Getter
@RequiredArgsConstructor
public enum RankingType {
    POINTS("ranking:points", "포인트 순위"),
    ACTIVITY_COUNT("ranking:activity:count", "활동량 순위");

    private final String redisKey;
    private final String displayName;

    /**
     * Redis key 반환
     */
    public String getRedisKey() {
        return redisKey;
    }

    /**
     * 포인트 기반 랭킹인지 확인
     */
    public boolean isPointsBased() {
        return this == POINTS;
    }

    /**
     * 활동량 기반 랭킹인지 확인
     */
    public boolean isActivityBased() {
        return this == ACTIVITY_COUNT;
    }
}
