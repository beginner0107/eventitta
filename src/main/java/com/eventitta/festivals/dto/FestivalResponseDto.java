package com.eventitta.festivals.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;

public interface FestivalResponseDto {
    Long getId();

    String getTitle();

    String getPlace();

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd", timezone = "Asia/Seoul")
    LocalDate getStartTime();

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd", timezone = "Asia/Seoul")
    LocalDate getEndTime();

    String getCategory();

    Boolean getIsFree();

    String getHomepageUrl();

    Double getDistance();
}
