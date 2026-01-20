package com.eventitta.festivals.dto.external.seoul;

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
        @JacksonXmlElementWrapper(useWrapping = false)
        List<SeoulFestivalRow> row
    ) {
    }
}
