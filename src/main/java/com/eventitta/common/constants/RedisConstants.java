package com.eventitta.common.constants;

/**
 * Redis 관련 상수
 */
public final class RedisConstants {

    private RedisConstants() {
    }

    /**
     * Redis 응답 상수
     */
    public static final String REDIS_PONG = "PONG";

    /**
     * Redis info 키 상수
     */
    public static final String REDIS_VERSION = "redis_version";
    public static final String REDIS_MODE = "redis_mode";
    public static final String CONNECTED_CLIENTS = "connected_clients";
    public static final String USED_MEMORY_HUMAN = "used_memory_human";
    public static final String USED_MEMORY_PEAK_HUMAN = "used_memory_peak_human";
    public static final String MAXMEMORY = "maxmemory";
    public static final String MAXMEMORY_POLICY = "maxmemory_policy";
    public static final String MAXMEMORY_HUMAN = "maxmemory_human";

    /**
     * Health detail 키 상수
     */
    public static final String DETAIL_STATUS = "status";
    public static final String DETAIL_RESPONSE = "response";
    public static final String DETAIL_VERSION = "version";
    public static final String DETAIL_MODE = "mode";
    public static final String DETAIL_ERROR = "error";
    public static final String DETAIL_CONNECTED_CLIENTS = "connected_clients";
    public static final String DETAIL_USED_MEMORY_HUMAN = "used_memory_human";
    public static final String DETAIL_USED_MEMORY_PEAK_HUMAN = "used_memory_peak_human";
    public static final String DETAIL_MAXMEMORY_HUMAN = "maxmemory_human";
    public static final String DETAIL_MAXMEMORY_POLICY = "maxmemory_policy";

    /**
     * 기본값 상수
     */
    public static final String DEFAULT_UNKNOWN = "unknown";
    public static final String DEFAULT_ZERO = "0";
    public static final String DEFAULT_NOEVICTION = "noeviction";
}
