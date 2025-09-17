package com.eventitta.festivals.api;

import com.eventitta.festivals.dto.NationalFestivalResponse;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;

public interface NationalFestivalApi {
    @GetExchange
    NationalFestivalResponse getFestivals(
        @RequestParam("serviceKey") String key,
        @RequestParam("pageNo") int pageNo,
        @RequestParam("numOfRows") int numOfRows,
        @RequestParam("type") String type
    );
}
