package com.eventitta.common.config.redis;

import com.eventitta.common.config.TestMissingBeansConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration
@Profile("test")
@Import(TestMissingBeansConfig.class)
public class RedisTestConfig {

    private static final Logger log = LoggerFactory.getLogger(RedisTestConfig.class);

    private static final String REDIS_IMAGE = "redis:7-alpine";
    private static final int REDIS_PORT = 6379;

    @Bean
    public GenericContainer<?> redisContainer() {
        GenericContainer<?> container = new GenericContainer<>(DockerImageName.parse(REDIS_IMAGE))
            .withExposedPorts(REDIS_PORT)
            .withCommand("redis-server",
                "--maxmemory", "64mb",
                "--maxmemory-policy", "allkeys-lru",
                "--timeout", "300",
                "--save", "\"\"",
                "--appendonly", "no");
        container.start();
        return container;
    }

    @Bean
    @Primary
    public RedisProperties redisProperties(GenericContainer<?> redisContainer) {
        RedisProperties properties = new RedisProperties();
        properties.setHost(redisContainer.getHost());
        properties.setPort(redisContainer.getFirstMappedPort());
        return properties;
    }

    @Bean
    @Primary
    public RedisConnectionFactory redisConnectionFactory(RedisProperties redisProperties) {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(redisProperties.getHost());
        config.setPort(redisProperties.getPort());

        if (redisProperties.getPassword() != null) {
            config.setPassword(redisProperties.getPassword());
        }

        return new LettuceConnectionFactory(config);
    }

    @Bean
    @Primary
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer();

        template.setKeySerializer(stringSerializer);
        template.setValueSerializer(jsonSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }

    @Bean
    @Primary
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }

    public static class RedisTestHelper {

        private static final Logger log = LoggerFactory.getLogger(RedisTestHelper.class);

        public static void cleanupRedis(GenericContainer<?> container) {
            try {
                container.execInContainer("redis-cli", "FLUSHALL");
            } catch (Exception e) {
                log.error("Failed to cleanup Redis", e);
            }
        }

        public static void printConnectionInfo(GenericContainer<?> container) {
            log.debug("Redis Test Container Info: Host={}, Port={}, Image={}",
                container.getHost(),
                container.getFirstMappedPort(),
                container.getDockerImageName());
        }
    }
}
