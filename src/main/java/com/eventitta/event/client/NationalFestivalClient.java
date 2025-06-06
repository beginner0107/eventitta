package com.eventitta.event.client;

import com.eventitta.common.config.RestClientConfig.NationalFestivalApi;
import com.eventitta.event.dto.NationalFestivalResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NationalFestivalClient {

    private final NationalFestivalApi nationalFestivalApi;

    @Value("${festival.national.service-key}")
    private String serviceKey;

    public NationalFestivalClient(@Qualifier("nationalFestivalApi") NationalFestivalApi nationalFestivalApi) {
        this.nationalFestivalApi = nationalFestivalApi;
    }

    /**
     * @param pageNo   1-based 페이지 번호
     * @param pageSize 한 페이지당 아이템 개수
     * @return Body (items + totalCount)
     */
    public NationalFestivalResponse.Body fetchPage(int pageNo, int pageSize) {
        log.debug("NationalFestivalClient: serviceKey=[{}], pageNo=[{}], pageSize=[{}]",
            serviceKey, pageNo, pageSize);

        if (serviceKey == null || serviceKey.isBlank()) {
            throw new IllegalStateException("NationalFestivalClient: serviceKey가 비어있음");
        }

        NationalFestivalResponse response =
            nationalFestivalApi.getFestivals(serviceKey, pageNo, pageSize, "json");

        if (response == null || response.getResponse() == null || response.getResponse().getBody() == null) {
            throw new RuntimeException("NationalFestivalClient: API 호출 결과가 null");
        }

        return response.getResponse().getBody();
    }
}
