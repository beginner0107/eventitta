package com.eventitta.infra.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class ClockConfig {

    @Bean
    public ZoneId applicationZoneId(@Value("${app.time-zone:Asia/Seoul}") String timeZone) {
        return ZoneId.of(timeZone);
    }

    @Bean
    public Clock clock(ZoneId applicationZoneId) {
        return Clock.system(applicationZoneId);
    }
}
