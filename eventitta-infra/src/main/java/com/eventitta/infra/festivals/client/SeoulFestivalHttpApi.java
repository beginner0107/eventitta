package com.eventitta.infra.festivals.client;

import com.eventitta.domain.festivals.dto.external.seoul.SeoulFestivalResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;

public interface SeoulFestivalHttpApi {

    @GetExchange(
        "/{serviceKey}/{fileType}/{serviceName}/{startIndex}/{endIndex}/{codeName}/{title}/{dateParam}"
    )
    SeoulFestivalResponse getSeoulEvents(
        @PathVariable("serviceKey") String serviceKey,
        @PathVariable("fileType") String fileType,
        @PathVariable("serviceName") String serviceName,
        @PathVariable("startIndex") int startIndex,
        @PathVariable("endIndex") int endIndex,
        @PathVariable("codeName") String codeName,
        @PathVariable("title") String title,
        @PathVariable("dateParam") String date
    );
}
