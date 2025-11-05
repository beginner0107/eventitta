package com.eventitta.festivals.dto.external.national;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record NationalFestivalItem(
    @JsonProperty("fstvlNm")
    String fstvlNm,

    @JsonProperty("opar")
    String opar,

    @JsonProperty("fstvlStartDate")
    String fstvlStartDate,

    @JsonProperty("fstvlEndDate")
    String fstvlEndDate,

    @JsonProperty("fstvlCo")
    String fstvlCo,

    @JsonProperty("mnnstNm")
    String mnnstNm,

    @JsonProperty("auspcInsttNm")
    String auspcInsttNm,

    @JsonProperty("suprtInsttNm")
    String suprtInsttNm,

    @JsonProperty("phoneNumber")
    String phoneNumber,

    @JsonProperty("homepageUrl")
    String homepageUrl,

    @JsonProperty("relateInfo")
    String relateInfo,

    @JsonProperty("rdnmadr")
    String rdnmadr,

    @JsonProperty("lnmadr")
    String lnmadr,

    @JsonProperty("latitude")
    String latitude,

    @JsonProperty("longitude")
    String longitude,

    @JsonProperty("referenceDate")
    String referenceDate,

    @JsonProperty("insttCode")
    String insttCode,

    @JsonProperty("insttNm")
    String insttNm
) {
}
