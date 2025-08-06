package com.eventitta.festivals.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class NationalFestivalItem {

    @JsonProperty("fstvlNm")
    private String fstvlNm;

    @JsonProperty("opar")
    private String opar;

    @JsonProperty("fstvlStartDate")
    private String fstvlStartDate;

    @JsonProperty("fstvlEndDate")
    private String fstvlEndDate;

    @JsonProperty("fstvlCo")
    private String fstvlCo;

    @JsonProperty("mnnstNm")
    private String mnnstNm;

    @JsonProperty("auspcInsttNm")
    private String auspcInsttNm;

    @JsonProperty("suprtInsttNm")
    private String suprtInsttNm;

    @JsonProperty("phoneNumber")
    private String phoneNumber;

    @JsonProperty("homepageUrl")
    private String homepageUrl;

    @JsonProperty("relateInfo")
    private String relateInfo;

    @JsonProperty("rdnmadr")
    private String rdnmadr;

    @JsonProperty("lnmadr")
    private String lnmadr;

    @JsonProperty("latitude")
    private String latitude;

    @JsonProperty("longitude")
    private String longitude;

    @JsonProperty("referenceDate")
    private String referenceDate;

    @JsonProperty("insttCode")
    private String insttCode;

    @JsonProperty("insttNm")
    private String insttNm;
}
