package com.eventitta.festival.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class SeoulFestivalResponse {
    @JsonProperty("culturalEventInfo")
    private CulturalEventInfo culturalEventInfo;

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CulturalEventInfo {
        @JsonProperty("row")
        private List<SeoulFestivalRow> row;
    }
}
