package com.eventitta.common.config.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Properties;

import static com.eventitta.common.constants.RedisConstants.*;

/**
 * Redis 헬스체크 설정
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class RedisHealthConfig {

    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisConnectionFactory redisConnectionFactory;

    @Bean
    public HealthIndicator redisHealthIndicator() {
        return new CustomRedisHealthIndicator();
    }

    private class CustomRedisHealthIndicator implements HealthIndicator {

        @Override
        public Health health() {
            try {
                String pong = redisTemplate.execute(RedisConnection::ping);

                if (!REDIS_PONG.equals(pong)) {
                    return Health.down()
                        .withDetail(DETAIL_STATUS, "Redis ping failed")
                        .withDetail(DETAIL_RESPONSE, pong)
                        .build();
                }

                Properties info = getRedisInfo();
                if (info != null && !info.isEmpty()) {
                    return Health.up()
                        .withDetail(DETAIL_STATUS, "Redis is running")
                        .withDetail(DETAIL_VERSION, info.getProperty(REDIS_VERSION, DEFAULT_UNKNOWN))
                        .withDetail(DETAIL_MODE, info.getProperty(REDIS_MODE, DEFAULT_UNKNOWN))
                        .withDetail(DETAIL_CONNECTED_CLIENTS, info.getProperty(CONNECTED_CLIENTS, DEFAULT_ZERO))
                        .withDetail(DETAIL_USED_MEMORY_HUMAN, info.getProperty(USED_MEMORY_HUMAN, DEFAULT_ZERO))
                        .withDetail(DETAIL_USED_MEMORY_PEAK_HUMAN, info.getProperty(USED_MEMORY_PEAK_HUMAN, DEFAULT_ZERO))
                        .withDetail(DETAIL_MAXMEMORY_HUMAN, getMaxMemory())
                        .withDetail(DETAIL_MAXMEMORY_POLICY, info.getProperty(MAXMEMORY_POLICY, DEFAULT_NOEVICTION))
                        .build();
                } else {
                    return Health.up()
                        .withDetail(DETAIL_STATUS, "Redis is running (limited info)")
                        .build();
                }

            } catch (Exception ex) {
                log.error("Redis health check failed", ex);
                return Health.down(ex)
                    .withDetail(DETAIL_STATUS, "Redis connection failed")
                    .withDetail(DETAIL_ERROR, ex.getMessage())
                    .build();
            }
        }

        private Properties getRedisInfo() {
            try {
                return redisTemplate.execute((RedisCallback<Properties>) connection -> {
                    Properties props = connection.serverCommands().info();
                    if (props == null) {
                        props = new Properties();
                    }

                    Properties info = new Properties();
                    for (String line : props.toString().split("\n")) {
                        if (!line.startsWith("#") && line.contains(":")) {
                            String[] parts = line.split(":", 2);
                            if (parts.length == 2) {
                                info.setProperty(parts[0].trim(), parts[1].trim());
                            }
                        }
                    }
                    return info;
                });
            } catch (Exception e) {
                log.warn("Failed to get Redis info", e);
                return new Properties();
            }
        }

        private String getMaxMemory() {
            try {
                return redisTemplate.execute((RedisCallback<String>) connection -> {
                    Properties config = connection.serverCommands().getConfig(MAXMEMORY);
                    if (config != null && config.containsKey(MAXMEMORY)) {
                        long maxMemoryBytes = Long.parseLong(config.getProperty(MAXMEMORY, DEFAULT_ZERO));
                        return humanReadableByteCount(maxMemoryBytes);
                    }
                    return DEFAULT_ZERO;
                });
            } catch (Exception e) {
                return DEFAULT_UNKNOWN;
            }
        }

        private String humanReadableByteCount(long bytes) {
            if (bytes == 0) return DEFAULT_ZERO;
            String[] units = {"B", "KB", "MB", "GB", "TB"};
            int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));
            return String.format("%.1f%s", bytes / Math.pow(1024, digitGroups), units[digitGroups]);
        }
    }
}
