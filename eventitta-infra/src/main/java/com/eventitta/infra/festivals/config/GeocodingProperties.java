package com.eventitta.infra.festivals.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "festival.geocoding")
public class GeocodingProperties {
    private String baseUrl;
    private String userAgent;
    private int timeoutSeconds = 5;
    private long requestDelayMs = 1000L;
    private boolean enabled = true;
}
