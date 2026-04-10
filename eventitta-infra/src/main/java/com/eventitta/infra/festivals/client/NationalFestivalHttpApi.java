package com.eventitta.infra.festivals.client;

import com.eventitta.domain.festivals.dto.external.national.NationalFestivalResponse;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;

public interface NationalFestivalHttpApi {

    @GetExchange
    NationalFestivalResponse getFestivals(
        @RequestParam("serviceKey") String key,
        @RequestParam("pageNo") int pageNo,
        @RequestParam("numOfRows") int numOfRows,
        @RequestParam("type") String type
    );
}
