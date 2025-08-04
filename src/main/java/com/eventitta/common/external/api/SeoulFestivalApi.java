package com.eventitta.common.external.api;

import com.eventitta.festival.dto.SeoulFestivalResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;

public interface SeoulFestivalApi {
    @GetExchange(
        "/{serviceKey}/{fileType}/{serviceName}/{startIndex}/{endIndex}/{codeName}/{title}/{dateParam}"
    )
    SeoulFestivalResponse getSeoulEvents(
        @PathVariable("serviceKey") String serviceKey,
        @PathVariable("fileType") String fileType,              // json 또는 xml
        @PathVariable("serviceName") String serviceName,        // culturalEventInfo
        @PathVariable("startIndex") int startIndex,             // 1
        @PathVariable("endIndex") int endIndex,                 // 1000
        @PathVariable("codeName") String codeName,              // 공연 분류 (or "%20")
        @PathVariable("title") String title,                    // 검색어 (or "%20")
        @PathVariable("dateParam") String date                  // YYYY-MM-DD 형식
    );
}
