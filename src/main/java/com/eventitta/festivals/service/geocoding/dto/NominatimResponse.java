package com.eventitta.festivals.service.geocoding.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record NominatimResponse(
    @JsonProperty("lat")
    String lat,
    @JsonProperty("lon")
    String lon,
    @JsonProperty("display_name")
    String displayName
) {
}
