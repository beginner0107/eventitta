package com.eventitta.festivals.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SeoulFestivalRow(
    @JsonProperty("CODENAME")
    String codeName,
    @JsonProperty("GUNAME")
    String guName,
    @JsonProperty("TITLE")
    String title,
    @JsonProperty("DATE")
    String date,
    @JsonProperty("PLACE")
    String place,
    @JsonProperty("ORG_NAME")
    String orgName,
    @JsonProperty("USE_TRGT")
    String useTarget,
    @JsonProperty("USE_FEE")
    String useFee,
    @JsonProperty("PLAYER")
    String player,
    @JsonProperty("PROGRAM")
    String program,
    @JsonProperty("ETC_DESC")
    String etcDesc,
    @JsonProperty("ORG_LINK")
    String orgLink,
    @JsonProperty("MAIN_IMG")
    String mainImg,
    @JsonProperty("RGSTDATE")
    String registerDate,
    @JsonProperty("TICKET")
    String ticket,
    @JsonProperty("STRTDATE")
    String startDate,
    @JsonProperty("END_DATE")
    String endDate,
    @JsonProperty("THEMECODE")
    String themeCode,
    @JsonProperty("LOT")
    String longitude,
    @JsonProperty("LAT")
    String latitude,
    @JsonProperty("IS_FREE")
    String isFree,
    @JsonProperty("HMPG_ADDR")
    String homepageAddr
) {
}
