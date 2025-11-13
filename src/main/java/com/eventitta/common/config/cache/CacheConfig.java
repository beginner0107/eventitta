package com.eventitta.common.config.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@EnableCaching
@EnableConfigurationProperties(CacheProperties.class)
@RequiredArgsConstructor
public class CacheConfig {

    private final CacheProperties cacheProperties;

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
            CacheConstants.REGIONS,
            CacheConstants.REGION_OPTIONS
        );

        cacheManager.setCaffeine(Caffeine.newBuilder()
            .maximumSize(cacheProperties.getMaximumSize())
            .expireAfterWrite(Duration.ofDays(cacheProperties.getTtlDays()))
            .recordStats());

        return cacheManager;
    }
}
