package com.eventitta.common.config.redis;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Import(RedisTestConfig.class)
class RedisHealthConfigTest {

    @Autowired
    private HealthIndicator redisHealthIndicator;

    @Test
    @DisplayName("Redis 헬스체크가 정상 동작한다")
    void testRedisHealthCheck() {
        // when
        Health health = redisHealthIndicator.health();

        // then
        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsKey("status");
        assertThat(health.getDetails().get("status")).isEqualTo("Redis is running");

        // Redis 정보가 Properties 객체에서 직접 조회되는지 확인
        assertThat(health.getDetails()).containsKeys(
            "version",
            "mode",
            "connected_clients",
            "used_memory_human"
        );
    }

    @Test
    @DisplayName("Redis 정보 조회 시 Properties 객체를 직접 사용한다")
    void testRedisInfoUsingPropertiesDirectly() {
        // when
        Health health = redisHealthIndicator.health();

        // then
        // Properties에서 직접 가져온 값들이 존재하는지 확인
        assertThat(health.getStatus()).isEqualTo(Status.UP);

        // Redis 버전 정보가 있는지 확인 (예: "7.x.x" 형태)
        Object version = health.getDetails().get("version");
        assertThat(version).isNotNull();
        assertThat(version.toString()).isNotEqualTo("unknown");

        // 연결된 클라이언트 수가 0 이상인지 확인
        Object clients = health.getDetails().get("connected_clients");
        assertThat(clients).isNotNull();
        assertThat(Integer.parseInt(clients.toString())).isGreaterThanOrEqualTo(0);
    }
}