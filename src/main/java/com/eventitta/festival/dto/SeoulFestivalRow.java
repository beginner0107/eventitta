package com.eventitta.festival.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class SeoulFestivalRow {
    @JsonProperty("CODENAME")
    private String CODENAME;
    @JsonProperty("GUNAME")
    private String GUNAME;
    @JsonProperty("TITLE")
    private String TITLE;
    @JsonProperty("DATE")
    private String DATE;
    @JsonProperty("PLACE")
    private String PLACE;
    @JsonProperty("ORG_NAME")
    private String ORG_NAME;
    @JsonProperty("USE_TRGT")
    private String USE_TRGT;
    @JsonProperty("USE_FEE")
    private String USE_FEE;
    @JsonProperty("PLAYER")
    private String PLAYER;
    @JsonProperty("PROGRAM")
    private String PROGRAM;
    @JsonProperty("ETC_DESC")
    private String ETC_DESC;
    @JsonProperty("ORG_LINK")
    private String ORG_LINK;
    @JsonProperty("MAIN_IMG")
    private String MAIN_IMG;
    @JsonProperty("RGSTDATE")
    private String RGSTDATE;
    @JsonProperty("TICKET")
    private String TICKET;
    @JsonProperty("STRTDATE")
    private String STRTDATE;
    @JsonProperty("END_DATE")
    private String END_DATE;
    @JsonProperty("THEMECODE")
    private String THEMECODE;
    @JsonProperty("LOT")
    private String LOT;
    @JsonProperty("LAT")
    private String LAT;
    @JsonProperty("IS_FREE")
    private String IS_FREE;
    @JsonProperty("HMPG_ADDR")
    private String HMPG_ADDR;
}
