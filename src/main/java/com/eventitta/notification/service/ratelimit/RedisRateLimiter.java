package com.eventitta.notification.service.ratelimit;

import com.eventitta.notification.constants.AlertConstants;
import com.eventitta.notification.domain.AlertLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Set;

/**
 * Redis 기반 분산 Rate Limiter
 *
 * <p>분산 환경에서 서버 간 Rate Limit를 공유하여 정확한 알림 제어를 제공합니다.</p>
 *
 * <p>Redis 장애 시 로컬 Caffeine 캐시로 자동 Fallback하여 알림 연속성을 보장합니다.</p>
 *
 * @see CacheBasedRateLimiter Redis 장애 시 Fallback 구현
 */
@Slf4j
@Component
@Primary
@Profile("!test")
@RequiredArgsConstructor
public class RedisRateLimiter implements RateLimiter {

    private final StringRedisTemplate stringRedisTemplate;
    private final CacheBasedRateLimiter fallbackLimiter;

    private static final String KEY_PREFIX = "ratelimit:alert:";

    @Override
    public boolean shouldSendAlert(String errorCode, AlertLevel level) {
        String key = KEY_PREFIX + createKey(errorCode, level);
        int maxAlerts = getMaxAlertsPerPeriod(level);

        try {
            Long count = stringRedisTemplate.opsForValue().increment(key);

            if (count == null) {
                log.warn("Redis increment returned null for key: {}", key);
                return fallbackToLocalCache(errorCode, level);
            }

            // 첫 번째 카운트일 때만 TTL 설정
            if (count == 1) {
                stringRedisTemplate.expire(key,
                    Duration.ofMinutes(AlertConstants.RATE_LIMIT_WINDOW_MINUTES));
            }

            boolean allowed = count <= maxAlerts;
            if (!allowed) {
                log.debug("Rate limit exceeded: key={}, count={}/{}", key, count, maxAlerts);
            }
            return allowed;

        } catch (RedisConnectionFailureException e) {
            log.error("Redis connection failed, using local fallback: {}", e.getMessage());
            return fallbackToLocalCache(errorCode, level);
        } catch (Exception e) {
            log.warn("Redis error, using local fallback: {}", e.getMessage());
            return fallbackToLocalCache(errorCode, level);
        }
    }

    @Override
    public void reset() {
        try {
            Set<String> keys = stringRedisTemplate.keys(KEY_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                stringRedisTemplate.delete(keys);
                log.info("Deleted {} rate limit keys from Redis", keys.size());
            }
        } catch (Exception e) {
            log.error("Failed to reset Redis rate limiter: {}", e.getMessage());
        }
        fallbackLimiter.reset();
    }

    /**
     * Redis 장애 시 로컬 Caffeine 캐시로 Fallback
     *
     * <p>Redis 연결 실패, 타임아웃 등의 상황에서 알림 누락을 방지합니다.</p>
     *
     * @param errorCode 에러 코드
     * @param level 알림 레벨
     * @return 알림 허용 여부
     */
    private boolean fallbackToLocalCache(String errorCode, AlertLevel level) {
        log.debug("Using local Caffeine cache fallback for: {}:{}", errorCode, level);
        return fallbackLimiter.shouldSendAlert(errorCode, level);
    }
}
