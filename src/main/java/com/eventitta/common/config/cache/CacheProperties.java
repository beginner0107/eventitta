package com.eventitta.common.config.cache;


import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;


@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "cache.caffeine")
public class CacheProperties {

    @Min(1)
    private int maximumSize = 20;

    @Min(1)
    private int ttlDays = 30;
}
