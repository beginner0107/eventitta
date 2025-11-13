package com.eventitta.festivals.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "festival.seoul")
public class SeoulFestivalProperties {

    private int pageSize = 1000;
    private int maxPages = 100;
    private String serviceFormat = "json";
    private String serviceName = "culturalEventInfo";
}
