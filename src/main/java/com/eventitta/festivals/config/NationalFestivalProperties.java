package com.eventitta.festivals.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "festival.national")
public class NationalFestivalProperties {

    private int pageSize = 100;
    private int maxPages = 100;
    private String serviceFormat = "json";
}
