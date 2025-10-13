package com.eventitta.festivals.service.geocoding;

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
    private int timeoutSeconds;
    private long requestDelayMs;
    private boolean enabled;
}
