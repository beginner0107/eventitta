package com.eventitta.event.dto;

import java.time.LocalDateTime;

public interface EventDistanceDto {
    Long getId();

    String getTitle();

    String getPlace();

    LocalDateTime getStartTime();

    LocalDateTime getEndTime();

    String getCategory();

    Boolean getIsFree();

    String getHomepageUrl();

    Double getDistance();
}
