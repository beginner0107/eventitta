package com.eventitta.festivals.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Setter
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString
public class NationalFestivalResponse {

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
        private List<NationalFestivalItem> items;

        @JsonProperty("totalCount")
        private int totalCount;

        @JsonProperty("numOfRows")
        private int numOfRows;

        @JsonProperty("pageNo")
        private int pageNo;
    }
}
