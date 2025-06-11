package com.eventitta.event.dto.response;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class SeoulFestivalResponse {
    @JsonProperty("culturalEventInfo")
    private SeoulResponseWrapper responseWrapper;

    @Setter
    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SeoulResponseWrapper {
        @JsonProperty("list_total_count")
        private int totalCount;

        @JsonProperty("RESULT")
        private Result result;

        @JsonProperty("row")
        private List<SeoulEventItem> items;
    }

    @Setter
    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Result {
        @JsonProperty("CODE")
        private String code;
        @JsonProperty("MESSAGE")
        private String message;
    }

    @Setter
    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SeoulEventItem {
        @JsonProperty("CODENAME")
        private String category;
        @JsonProperty("GUNAME")
        private String district;
        @JsonProperty("TITLE")
        private String title;
        @JsonProperty("DATE")
        private String date;
        @JsonProperty("PLACE")
        private String place;
        @JsonProperty("ORG_NAME")
        private String orgName;
        @JsonProperty("USE_TRGT")
        private String useTarget;
        @JsonProperty("USE_FEE")
        private String useFee;
        @JsonProperty("PLAYER")
        private String performer;
        @JsonProperty("PROGRAM")
        private String program;
        @JsonProperty("ETC_DESC")
        private String etcDesc;
        @JsonProperty("ORG_LINK")
        private String homepageUrl;
        @JsonProperty("MAIN_IMG")
        private String mainImgUrl;
        @JsonProperty("RGSTDATE")
        private String registerDate;
        @JsonProperty("TICKET")
        private String ticket;
        @JsonProperty("STRTDATE")
        private String startDate;
        @JsonProperty("END_DATE")
        private String endDate;
        @JsonProperty("THEMECODE")
        private String themeCode;
        @JsonProperty("LOT")
        private String lot;
        @JsonProperty("LAT")
        private String lat;
        @JsonProperty("IS_FREE")
        private String isFree;
        @JsonProperty("HMPG_ADDR")
        private String detailUrl;
    }
}
