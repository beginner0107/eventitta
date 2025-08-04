package com.eventitta.festival.config;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Component
@ConfigurationProperties(prefix = "festival.seoul")
public class SeoulFestivalConfig {

    private int pageSize = 1000;
    private int maxPages = 100;
    private String serviceFormat = "json";
    private String serviceName = "culturalEventInfo";

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public void setMaxPages(int maxPages) {
        this.maxPages = maxPages;
    }

    public void setServiceFormat(String serviceFormat) {
        this.serviceFormat = serviceFormat;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }
}
