package com.eventitta.festivals.dto;

import java.time.LocalDate;

public interface FestivalProjection {
    Long getId();

    String getTitle();

    String getPlace();

    LocalDate getStartTime();

    LocalDate getEndTime();

    String getCategory();

    Boolean getIsFree();

    String getHomepageUrl();

    Double getDistance();
}
