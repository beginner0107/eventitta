package com.eventitta.festival.config;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Component
@ConfigurationProperties(prefix = "festival.national")
public class NationalFestivalConfig {

    private int pageSize = 100;
    private int maxPages = 100;
    private String serviceFormat = "json";

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public void setMaxPages(int maxPages) {
        this.maxPages = maxPages;
    }

    public void setServiceFormat(String serviceFormat) {
        this.serviceFormat = serviceFormat;
    }
}
