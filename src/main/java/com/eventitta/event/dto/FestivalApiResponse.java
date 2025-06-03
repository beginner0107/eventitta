package com.eventitta.event.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class FestivalApiResponse {

    @JsonProperty("response")
    private ResponseWrapper response;

    @Setter
    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ResponseWrapper {

        @JsonProperty("header")
        private Header header;

        @JsonProperty("body")
        private Body body;

        @Setter
        @Getter
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Header {
            @JsonProperty("resultCode")
            private String resultCode;

            @JsonProperty("resultMsg")
            private String resultMsg;

            @JsonProperty("type")
            private String type;

        }
    }

    @Setter
    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Body {

        @JsonProperty("items")
        private List<FestivalItem> items;

        @JsonProperty("totalCount")
        private int totalCount;

        @JsonProperty("numOfRows")
        private int numOfRows;

        @JsonProperty("pageNo")
        private int pageNo;

    }

    @Setter
    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FestivalItem {

        @JsonProperty("fstvlNm")
        private String festivalName;

        @JsonProperty("opar")
        private String place;

        @JsonProperty("fstvlStartDate")
        private String startDate;   // "yyyy-MM-dd" 형식

        @JsonProperty("fstvlEndDate")
        private String endDate;     // "yyyy-MM-dd" 형식

        @JsonProperty("fstvlCo")
        private String description;

        @JsonProperty("mnnstNm")
        private String organizerName;

        @JsonProperty("auspcInsttNm")
        private String hostName;

        @JsonProperty("suprtInsttNm")
        private String sponsorName;

        @JsonProperty("phoneNumber")
        private String phoneNumber;

        @JsonProperty("homepageUrl")
        private String homepageUrl;

        @JsonProperty("relateInfo")
        private String relatedInfo;

        @JsonProperty("rdnmadr")
        private String roadAddress;

        @JsonProperty("lnmadr")
        private String landAddress;

        @JsonProperty("latitude")
        private String latitude;

        @JsonProperty("longitude")
        private String longitude;

        @JsonProperty("referenceDate")
        private String referenceDate;

        @JsonProperty("insttCode")
        private String institutionCode;

        @JsonProperty("insttNm")
        private String institutionName;

    }
}
