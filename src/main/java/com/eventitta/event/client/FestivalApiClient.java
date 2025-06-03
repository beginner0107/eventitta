package com.eventitta.event.client;

import com.eventitta.common.config.RestClientConfig;
import com.eventitta.event.dto.FestivalApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class FestivalApiClient {

    private final RestClientConfig.FestivalApi festivalApi;

    @Value("${festival.api.service-key}")
    private String serviceKey;

    public FestivalApiClient(RestClientConfig.FestivalApi festivalApi) {
        this.festivalApi = festivalApi;
    }

    public FestivalApiResponse.Body fetchFestivalPage(int pageNo, int numOfRows) {
        log.debug("festivalApiClient.serviceKey = [{}]", serviceKey);
        log.debug("pageNo = {}, numOfRows = {}", pageNo, numOfRows);

        if (serviceKey == null || serviceKey.isBlank()) {
            throw new IllegalStateException("serviceKey가 null 또는 빈 문자열입니다.");
        }

        try {
            FestivalApiResponse response = festivalApi.getFestivals(serviceKey, pageNo, numOfRows, "json");
            if (response == null || response.getResponse() == null || response.getResponse().getBody() == null) {
                throw new RuntimeException("축제 API 호출 결과가 null");
            }
            return response.getResponse().getBody();
        } catch (IllegalArgumentException e) {
            log.error("getFestivals 호출 중 예외 발생:");
            log.error("  serviceKey=[{}], pageNo=[{}], numOfRows=[{}]", serviceKey, pageNo, numOfRows);
            throw e;
        }
    }
}
