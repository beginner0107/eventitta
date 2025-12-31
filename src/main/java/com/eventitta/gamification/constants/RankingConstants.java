package com.eventitta.gamification.constants;

/**
 * 순위 시스템 관련 상수 정의
 * Redis 키 네이밍, 캐시 설정, 동기화 설정 등
 */
public class RankingConstants {

    // Redis Key Patterns
    public static final String RANKING_POINTS_KEY = "ranking:points";
    public static final String RANKING_ACTIVITY_COUNT_KEY = "ranking:activity:count";
    public static final String RANKING_CACHE_KEY_PREFIX = "ranking:cache:";

    // Ranking Settings
    public static final int DEFAULT_TOP_RANK_SIZE = 100;
    public static final long USER_RANK_CACHE_TTL_SECONDS = 300; // 5분

    // Redis Timeout Settings
    public static final int REDIS_TIMEOUT_MS = 3000;

    // Sync Settings
    public static final int SYNC_BATCH_SIZE = 1000; // 배치 처리 크기

    private RankingConstants() {
        // 인스턴스화 방지
    }
}
