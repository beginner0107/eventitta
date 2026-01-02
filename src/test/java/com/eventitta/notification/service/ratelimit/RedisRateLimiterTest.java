package com.eventitta.notification.service.ratelimit;

import com.eventitta.notification.domain.AlertLevel;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RedisRateLimiter 통합 테스트
 *
 * <p>Testcontainers를 사용하여 실제 Redis와 통신하며 분산 환경을 시뮬레이션합니다.</p>
 */
class RedisRateLimiterTest {

    private static GenericContainer<?> redisContainer;
    private static StringRedisTemplate stringRedisTemplate;
    private static CacheBasedRateLimiter fallbackLimiter;

    private RedisRateLimiter rateLimiter;

    @BeforeAll
    static void setUpAll() {
        // Testcontainers Redis 시작
        redisContainer = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);
        redisContainer.start();

        // StringRedisTemplate 설정
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(redisContainer.getHost());
        config.setPort(redisContainer.getFirstMappedPort());

        LettuceConnectionFactory connectionFactory = new LettuceConnectionFactory(config);
        connectionFactory.afterPropertiesSet();

        stringRedisTemplate = new StringRedisTemplate();
        stringRedisTemplate.setConnectionFactory(connectionFactory);
        stringRedisTemplate.afterPropertiesSet();

        // Fallback용 CacheBasedRateLimiter
        fallbackLimiter = new CacheBasedRateLimiter();
    }

    @AfterAll
    static void tearDownAll() {
        if (redisContainer != null) {
            redisContainer.stop();
        }
    }

    @BeforeEach
    void setUp() {
        rateLimiter = new RedisRateLimiter(stringRedisTemplate, fallbackLimiter);

        // Redis 데이터 초기화
        stringRedisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
        fallbackLimiter.reset();
    }

    @Test
    @DisplayName("Redis 정상 동작 시 제한치까지 허용")
    void shouldAllowUpToLimitWithRedis() {
        // given
        String errorCode = "TEST_ERROR";
        AlertLevel level = AlertLevel.CRITICAL; // 10회 제한

        // when & then: 10회까지 허용
        for (int i = 0; i < 10; i++) {
            boolean allowed = rateLimiter.shouldSendAlert(errorCode, level);
            assertThat(allowed).isTrue();
        }

        // when: 11번째 요청
        boolean eleventhRequest = rateLimiter.shouldSendAlert(errorCode, level);

        // then: 거부되어야 함
        assertThat(eleventhRequest).isFalse();
    }

    @Test
    @DisplayName("각 AlertLevel별 제한치가 올바르게 적용됨")
    void shouldApplyCorrectLimitForEachLevel() {
        // CRITICAL: 10회
        for (int i = 0; i < 10; i++) {
            assertThat(rateLimiter.shouldSendAlert("CRIT", AlertLevel.CRITICAL)).isTrue();
        }
        assertThat(rateLimiter.shouldSendAlert("CRIT", AlertLevel.CRITICAL)).isFalse();

        // HIGH: 5회
        for (int i = 0; i < 5; i++) {
            assertThat(rateLimiter.shouldSendAlert("HIGH", AlertLevel.HIGH)).isTrue();
        }
        assertThat(rateLimiter.shouldSendAlert("HIGH", AlertLevel.HIGH)).isFalse();

        // MEDIUM: 2회
        for (int i = 0; i < 2; i++) {
            assertThat(rateLimiter.shouldSendAlert("MED", AlertLevel.MEDIUM)).isTrue();
        }
        assertThat(rateLimiter.shouldSendAlert("MED", AlertLevel.MEDIUM)).isFalse();

        // INFO: 1회
        assertThat(rateLimiter.shouldSendAlert("INFO", AlertLevel.INFO)).isTrue();
        assertThat(rateLimiter.shouldSendAlert("INFO", AlertLevel.INFO)).isFalse();
    }

    @Test
    @DisplayName("다중 RedisRateLimiter 인스턴스가 동일한 제한치 공유 (분산 환경 시뮬레이션)")
    void shouldShareLimitAcrossMultipleInstances() {
        // given: 3개의 서버를 시뮬레이션
        RedisRateLimiter server1 = new RedisRateLimiter(stringRedisTemplate, fallbackLimiter);
        RedisRateLimiter server2 = new RedisRateLimiter(stringRedisTemplate, fallbackLimiter);
        RedisRateLimiter server3 = new RedisRateLimiter(stringRedisTemplate, fallbackLimiter);

        String errorCode = "DISTRIBUTED_TEST";
        AlertLevel level = AlertLevel.CRITICAL; // 총 10회 제한

        // when: 서버별로 요청
        // 서버1: 4회
        for (int i = 0; i < 4; i++) {
            assertThat(server1.shouldSendAlert(errorCode, level)).isTrue();
        }

        // 서버2: 3회
        for (int i = 0; i < 3; i++) {
            assertThat(server2.shouldSendAlert(errorCode, level)).isTrue();
        }

        // 서버3: 3회
        for (int i = 0; i < 3; i++) {
            assertThat(server3.shouldSendAlert(errorCode, level)).isTrue();
        }

        // then: 모든 서버에서 다음 요청은 거부되어야 함 (총 10회 도달)
        assertThat(server1.shouldSendAlert(errorCode, level)).isFalse();
        assertThat(server2.shouldSendAlert(errorCode, level)).isFalse();
        assertThat(server3.shouldSendAlert(errorCode, level)).isFalse();
    }

    @Test
    @DisplayName("Redis 키에 TTL이 올바르게 설정됨")
    void shouldSetCorrectTTL() {
        // given
        String errorCode = "TTL_TEST";
        AlertLevel level = AlertLevel.HIGH;

        // when
        rateLimiter.shouldSendAlert(errorCode, level);

        // then
        String redisKey = "ratelimit:alert:" + errorCode + ":" + level;
        Long ttl = stringRedisTemplate.getExpire(redisKey, TimeUnit.MINUTES);

        assertThat(ttl).isNotNull();
        assertThat(ttl).isBetween(4L, 5L); // 5분 설정, 약간의 오차 허용
    }

    @Test
    @DisplayName("다른 에러 코드는 독립적으로 카운트됨")
    void shouldCountDifferentErrorCodesIndependently() {
        // given
        AlertLevel level = AlertLevel.HIGH; // 5회 제한

        // when: ERROR_001로 5회 요청
        for (int i = 0; i < 5; i++) {
            assertThat(rateLimiter.shouldSendAlert("ERROR_001", level)).isTrue();
        }

        // then: ERROR_001은 제한 도달
        assertThat(rateLimiter.shouldSendAlert("ERROR_001", level)).isFalse();

        // but: ERROR_002는 여전히 허용됨
        assertThat(rateLimiter.shouldSendAlert("ERROR_002", level)).isTrue();
    }

    @Test
    @DisplayName("같은 에러 코드라도 다른 레벨은 독립적으로 카운트됨")
    void shouldCountDifferentLevelsIndependently() {
        // given
        String errorCode = "SAME_CODE";

        // when: CRITICAL 레벨로 10회 요청
        for (int i = 0; i < 10; i++) {
            assertThat(rateLimiter.shouldSendAlert(errorCode, AlertLevel.CRITICAL)).isTrue();
        }

        // then: CRITICAL은 제한 도달
        assertThat(rateLimiter.shouldSendAlert(errorCode, AlertLevel.CRITICAL)).isFalse();

        // but: 같은 에러 코드여도 HIGH 레벨은 여전히 허용됨
        assertThat(rateLimiter.shouldSendAlert(errorCode, AlertLevel.HIGH)).isTrue();
    }

    @Test
    @DisplayName("reset() 호출 시 Redis 키 삭제 및 재사용 가능")
    void shouldDeleteRedisKeysOnReset() {
        // given
        rateLimiter.shouldSendAlert("TEST1", AlertLevel.INFO); // 1회 제한
        rateLimiter.shouldSendAlert("TEST2", AlertLevel.MEDIUM); // 2회 중 1회

        // when: 제한 도달 확인
        assertThat(rateLimiter.shouldSendAlert("TEST1", AlertLevel.INFO)).isFalse();

        // when: reset 호출
        rateLimiter.reset();

        // then: 다시 허용되어야 함
        assertThat(rateLimiter.shouldSendAlert("TEST1", AlertLevel.INFO)).isTrue();
        assertThat(rateLimiter.shouldSendAlert("TEST2", AlertLevel.MEDIUM)).isTrue();
    }

    @Test
    @DisplayName("Redis 키가 올바른 형식으로 생성됨")
    void shouldCreateCorrectRedisKeyFormat() {
        // given
        String errorCode = "KEY_FORMAT_TEST";
        AlertLevel level = AlertLevel.HIGH;

        // when
        rateLimiter.shouldSendAlert(errorCode, level);

        // then
        String expectedKey = "ratelimit:alert:" + errorCode + ":" + level;
        String value = stringRedisTemplate.opsForValue().get(expectedKey);

        assertThat(value).isNotNull();
        assertThat(value).isEqualTo("1");
    }

    @Test
    @DisplayName("연속된 요청이 올바르게 카운트됨")
    void shouldCountConsecutiveRequestsCorrectly() {
        // given
        String errorCode = "CONSECUTIVE";
        AlertLevel level = AlertLevel.MEDIUM; // 2회 제한

        // when & then
        assertThat(rateLimiter.shouldSendAlert(errorCode, level)).isTrue();  // count: 1
        assertThat(rateLimiter.shouldSendAlert(errorCode, level)).isTrue();  // count: 2
        assertThat(rateLimiter.shouldSendAlert(errorCode, level)).isFalse(); // count: 3, 거부

        // 계속 거부되어야 함
        for (int i = 0; i < 5; i++) {
            assertThat(rateLimiter.shouldSendAlert(errorCode, level)).isFalse();
        }

        // Redis 카운트 확인
        String key = "ratelimit:alert:" + errorCode + ":" + level;
        String count = stringRedisTemplate.opsForValue().get(key);
        assertThat(Integer.parseInt(count)).isGreaterThan(2);
    }
}
