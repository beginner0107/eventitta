package com.eventitta.common.config.redis;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration
@Profile("test")
public class RedisTestConfig {

    private static final String REDIS_IMAGE = "redis:7-alpine";
    private static final int REDIS_PORT = 6379;

    @Bean
    @ServiceConnection(name = "redis")
    public GenericContainer<?> redisContainer() {
        return new GenericContainer<>(DockerImageName.parse(REDIS_IMAGE))
            .withExposedPorts(REDIS_PORT)
            .withCommand("redis-server",
                "--maxmemory", "64mb",
                "--maxmemory-policy", "allkeys-lru",
                "--timeout", "300",
                "--save", "\"\"",
                "--appendonly", "no");
    }

    public static class RedisTestHelper {

        public static void cleanupRedis(GenericContainer<?> container) {
            try {
                container.execInContainer("redis-cli", "FLUSHALL");
            } catch (Exception e) {
                System.err.println("Failed to cleanup Redis: " + e.getMessage());
            }
        }

        public static void printConnectionInfo(GenericContainer<?> container) {
            System.out.println("Redis Test Container Info:");
            System.out.println("  Host: " + container.getHost());
            System.out.println("  Port: " + container.getFirstMappedPort());
            System.out.println("  Image: " + container.getDockerImageName());
        }
    }
}
