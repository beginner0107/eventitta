package com.eventitta.event.client;

import com.eventitta.common.config.RestClientConfig.SeoulFestivalApi;
import com.eventitta.event.dto.SeoulFestivalResponse;
import com.eventitta.event.service.PageResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SeoulFestivalClient implements FestivalApiClient<SeoulFestivalResponse.SeoulEventItem> {

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

    @Override
    public PageResult<SeoulFestivalResponse.SeoulEventItem> fetchPage(int pageNo, int pageSize, String dateParam) {
        int startIndex = (pageNo - 1) * pageSize + 1;
        int endIndex = pageNo * pageSize;

        log.debug(
            "SeoulFestivalClient: serviceKey={}, fileType={}, serviceName={}, start={}, end={}, date={}"
            , serviceKey, fileType, serviceName, startIndex, endIndex, dateParam);

        SeoulFestivalResponse response = seoulFestivalApi.getSeoulEvents(
            serviceKey, fileType, serviceName,
            startIndex, endIndex, "%20", "%20", dateParam
        );

        if (response == null || response.getResponseWrapper() == null) {
            throw new RuntimeException("SeoulFestivalClient: API 호출 결과가 null");
        }

        var wrapper = response.getResponseWrapper();
        return new PageResult<>(wrapper.getItems(), wrapper.getTotalCount());
    }
}
