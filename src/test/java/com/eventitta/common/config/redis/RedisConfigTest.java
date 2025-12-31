package com.eventitta.common.config.redis;

import com.eventitta.gamification.service.RankingService;
import com.eventitta.testsupport.EnabledIfDockerAvailable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.GenericContainer;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@ActiveProfiles("test")
@Import(RedisTestConfig.class)
@EnabledIfDockerAvailable
@SuppressWarnings("null")
class RedisConfigTest {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private GenericContainer<?> redisContainer;

    @MockitoBean
    private RankingService rankingService;

    @AfterEach
    void tearDown() {
        RedisTestConfig.RedisTestHelper.cleanupRedis(redisContainer);
    }

    @Test
    @DisplayName("Redis 연결 테스트")
    void testRedisConnection() {
        // when
        String pong = stringRedisTemplate.execute((org.springframework.data.redis.core.RedisCallback<String>) connection -> connection.ping());

        // then
        assertThat(pong).isEqualTo("PONG");
    }

    @Test
    @DisplayName("문자열 저장 및 조회")
    void testStringOperations() {
        // given
        String key = "test:string";
        String value = "Hello Redis";

        // when
        stringRedisTemplate.opsForValue().set(key, value);
        String retrieved = stringRedisTemplate.opsForValue().get(key);

        // then
        assertThat(retrieved).isEqualTo(value);
    }

    @Test
    @DisplayName("객체 저장 및 조회")
    void testObjectOperations() {
        // given
        String key = "test:user";
        TestUser user = new TestUser(1L, "testuser", 100);

        // when
        redisTemplate.opsForValue().set(key, user);
        Object retrieved = redisTemplate.opsForValue().get(key);

        // then
        assertThat(retrieved).isInstanceOf(TestUser.class);
        TestUser retrievedUser = (TestUser) retrieved;
        assertThat(retrievedUser.getId()).isEqualTo(1L);
        assertThat(retrievedUser.getNickname()).isEqualTo("testuser");
        assertThat(retrievedUser.getPoints()).isEqualTo(100);
    }

    @Test
    @DisplayName("TTL 설정 테스트")
    void testTTL() {
        // given
        String key = "test:ttl";
        String value = "expire soon";

        // when
        stringRedisTemplate.opsForValue().set(key, value, 1, TimeUnit.SECONDS);

        // then - 즉시 값이 존재하는지 확인
        assertThat(stringRedisTemplate.opsForValue().get(key)).isEqualTo(value);

        // Awaitility로 만료 확인 - 최대 2초 대기, 100ms 간격으로 체크
        await()
            .atMost(Duration.ofSeconds(2))
            .pollInterval(Duration.ofMillis(100))
            .untilAsserted(() ->
                assertThat(stringRedisTemplate.opsForValue().get(key)).isNull()
            );
    }

    @Test
    @DisplayName("ZSet을 사용한 순위 시스템 테스트")
    void testZSetOperations() {
        // given
        String key = "test:ranking";

        // when
        redisTemplate.opsForZSet().add(key, "user1", 100.0);
        redisTemplate.opsForZSet().add(key, "user2", 200.0);
        redisTemplate.opsForZSet().add(key, "user3", 150.0);

        // then
        Set<Object> topUsers = redisTemplate.opsForZSet().reverseRange(key, 0, 1);
        assertThat(topUsers).containsExactly("user2", "user3");

        Long rank = redisTemplate.opsForZSet().reverseRank(key, "user1");
        assertThat(rank).isEqualTo(2L); // 0-based index

        Double score = redisTemplate.opsForZSet().score(key, "user1");
        assertThat(score).isEqualTo(100.0);
    }

    @Test
    @DisplayName("Hash 작업 테스트")
    void testHashOperations() {
        // given
        String key = "test:user:profile";

        // when
        redisTemplate.opsForHash().put(key, "name", "John");
        redisTemplate.opsForHash().put(key, "age", "30");
        redisTemplate.opsForHash().put(key, "points", "1000");

        // then
        assertThat(redisTemplate.opsForHash().get(key, "name")).isEqualTo("John");
        assertThat(redisTemplate.opsForHash().get(key, "age")).isEqualTo("30");
        assertThat(redisTemplate.opsForHash().get(key, "points")).isEqualTo("1000");

        assertThat(redisTemplate.opsForHash().size(key)).isEqualTo(3);
    }

    @Test
    @DisplayName("파이프라인을 사용한 배치 처리")
    void testPipelineOperations() {
        // given
        int batchSize = 1000;

        // when
        stringRedisTemplate.executePipelined((org.springframework.data.redis.core.RedisCallback<Object>) connection -> {
            for (int i = 0; i < batchSize; i++) {
                connection.zSetCommands().zAdd(
                    ("test:batch:ranking").getBytes(),
                    i * 10.0,
                    ("user" + i).getBytes()
                );
            }
            return null;
        });

        // then
        // 파이프라인을 통해 모든 데이터가 정상적으로 저장되었는지 검증
        Long count = stringRedisTemplate.opsForZSet().zCard("test:batch:ranking");
        assertThat(count).isEqualTo(batchSize);

        // 상위 3개 사용자가 올바른 순서로 저장되었는지 확인 (점수가 높은 순)
        Set<String> topUsers = stringRedisTemplate.opsForZSet().reverseRange("test:batch:ranking", 0, 2);
        assertThat(topUsers).hasSize(3);
        assertThat(topUsers).containsExactly("user999", "user998", "user997");
    }

    static class TestUser {
        private Long id;
        private String nickname;
        private int points;

        public TestUser() {
        }

        public TestUser(Long id, String nickname, int points) {
            this.id = id;
            this.nickname = nickname;
            this.points = points;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getNickname() {
            return nickname;
        }

        public void setNickname(String nickname) {
            this.nickname = nickname;
        }

        public int getPoints() {
            return points;
        }

        public void setPoints(int points) {
            this.points = points;
        }
    }
}
