package com.eventitta.festivals.dto.external.geocoding;

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
