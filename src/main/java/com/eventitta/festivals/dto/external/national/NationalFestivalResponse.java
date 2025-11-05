package com.eventitta.festivals.dto.external.national;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record NationalFestivalResponse(
    @JsonProperty("response")
    ResponseWrapper response
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ResponseWrapper(
        @JsonProperty("header")
        Header header,

        @JsonProperty("body")
        Body body
    ) {

        @JsonIgnoreProperties(ignoreUnknown = true)
        public record Header(
            @JsonProperty("resultCode")
            String resultCode,

            @JsonProperty("resultMsg")
            String resultMsg,

            @JsonProperty("type")
            String type
        ) {
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Body(
        @JsonProperty("items")
        List<NationalFestivalItem> items,

        @JsonProperty("totalCount")
        int totalCount,

        @JsonProperty("numOfRows")
        int numOfRows,

        @JsonProperty("pageNo")
        int pageNo
    ) {
    }
}
