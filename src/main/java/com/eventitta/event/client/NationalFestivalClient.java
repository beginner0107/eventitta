package com.eventitta.event.client;

import com.eventitta.common.config.RestClientConfig.NationalFestivalApi;
import com.eventitta.event.dto.NationalFestivalResponse;
import com.eventitta.event.service.PageResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NationalFestivalClient implements FestivalApiClient<NationalFestivalResponse.FestivalItem> {


    private final NationalFestivalApi nationalFestivalApi;

    @Value("${festival.national.service-key}")
    private String serviceKey;

    public NationalFestivalClient(@Qualifier("nationalFestivalApi") NationalFestivalApi nationalFestivalApi) {
        this.nationalFestivalApi = nationalFestivalApi;
    }

    @Override
    public PageResult<NationalFestivalResponse.FestivalItem> fetchPage(int pageNo, int pageSize, String dateParam) {
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

        var body = response.getResponse().getBody();
        return new PageResult<>(body.getItems(), body.getTotalCount());
    }
}
