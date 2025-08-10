package com.eventitta.festivals.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SeoulFestivalResponse(
    @JsonProperty("culturalEventInfo")
    CulturalEventInfo culturalEventInfo
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CulturalEventInfo(
        @JsonProperty("row")
        List<SeoulFestivalRow> row
    ) {
    }
}
