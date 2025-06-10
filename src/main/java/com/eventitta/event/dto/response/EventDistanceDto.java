package com.eventitta.event.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public interface EventDistanceDto {
    Long getId();

    String getTitle();

    String getPlace();

    @JsonFormat(
        shape = JsonFormat.Shape.STRING,
        pattern = "yyyy-MM-dd HH:mm",
        timezone = "Asia/Seoul"
    )
    LocalDateTime getStartTime();

    @JsonFormat(
        shape = JsonFormat.Shape.STRING,
        pattern = "yyyy-MM-dd HH:mm",
        timezone = "Asia/Seoul"
    )
    LocalDateTime getEndTime();

    String getCategory();

    Boolean getIsFree();

    String getHomepageUrl();

    Double getDistance();
}
