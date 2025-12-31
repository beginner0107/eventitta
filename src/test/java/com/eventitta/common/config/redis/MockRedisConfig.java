package com.eventitta.common.config.redis;

import com.eventitta.common.config.TestMissingBeansConfig;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

/**
 * 테스트용 Mock Redis 설정
 * TestContainers 대신 Mock 객체 사용
 */
@TestConfiguration
@Profile("test")
@SuppressWarnings("null")
@Import(TestMissingBeansConfig.class)
public class MockRedisConfig {

    @Bean
    @Primary
    public RedisConnectionFactory mockRedisConnectionFactory() {
        return Mockito.mock(RedisConnectionFactory.class);
    }

    @Bean
    @Primary
    @SuppressWarnings("unchecked")
    public RedisTemplate<String, Object> mockRedisTemplate() {
        RedisTemplate<String, Object> template = Mockito.mock(RedisTemplate.class);
        ZSetOperations<String, Object> zSetOps = Mockito.mock(ZSetOperations.class);

        // 기본 동작 설정
        Mockito.when(template.opsForZSet()).thenReturn(zSetOps);

        // ZSet 연산 기본 값 설정
        Mockito.when(zSetOps.zCard(Mockito.anyString())).thenReturn(0L);
        Mockito.when(zSetOps.reverseRangeWithScores(Mockito.anyString(), Mockito.anyLong(), Mockito.anyLong()))
            .thenReturn(null);

        return template;
    }
}
