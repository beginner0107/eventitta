package com.eventitta.event.client;

import com.eventitta.common.config.RestClientConfig.SeoulFestivalApi;
import com.eventitta.event.dto.SeoulFestivalResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SeoulFestivalClient {

    private final SeoulFestivalApi seoulFestivalApi;

    @Value("${festival.seoul.service-key}")
    private String serviceKey;

    @Value("${festival.seoul.file-type}")
    private String fileType;

    @Value("${festival.seoul.service-name}")
    private String serviceName;

    public SeoulFestivalClient(SeoulFestivalApi seoulFestivalApi) {
        this.seoulFestivalApi = seoulFestivalApi;
    }

    /**
     * @param startIndex 1-based 인덱스 (inclusive)
     * @param endIndex   endIndex inclusive (1-based)
     * @param dateParam  검색 기준 (YYYYMMDD, YYYY-MM 등)
     */
    public SeoulFestivalResponse.SeoulResponseWrapper fetchPage(int startIndex,
                                                                int endIndex,
                                                                String unused1,
                                                                String unused2,
                                                                String dateParam) {
        log.debug("SeoulFestivalClient: serviceKey={}, fileType={}, serviceName={}, start={}, end={}, date={}",
            serviceKey, fileType, serviceName, startIndex, endIndex, dateParam);

        SeoulFestivalResponse response = seoulFestivalApi.getSeoulEvents(
            serviceKey, fileType, serviceName,
            startIndex, endIndex, unused1, unused2, dateParam
        );

        if (response == null || response.getResponseWrapper() == null) {
            throw new RuntimeException("SeoulFestivalClient: API 호출 결과가 null");
        }

        return response.getResponseWrapper();
    }
}
