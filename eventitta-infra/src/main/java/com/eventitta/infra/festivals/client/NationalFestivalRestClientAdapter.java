package com.eventitta.infra.festivals.client;

import com.eventitta.domain.festivals.client.FestivalBatch;
import com.eventitta.domain.festivals.client.NationalFestivalClient;
import com.eventitta.domain.festivals.dto.external.national.NationalFestivalItem;
import com.eventitta.domain.festivals.dto.external.national.NationalFestivalResponse;
import com.eventitta.infra.festivals.config.NationalFestivalProperties;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NationalFestivalRestClientAdapter implements NationalFestivalClient {

    private final NationalFestivalHttpApi nationalFestivalHttpApi;
    private final NationalFestivalProperties properties;

    @Override
    public FestivalBatch<NationalFestivalItem> fetchPage(String serviceKey, int page) {
        NationalFestivalResponse response = nationalFestivalHttpApi.getFestivals(
            serviceKey,
            page,
            properties.getPageSize(),
            properties.getServiceFormat()
        );

        List<NationalFestivalItem> items = List.of();
        if (response != null
            && response.response() != null
            && response.response().body() != null
            && response.response().body().items() != null
        ) {
            items = response.response().body().items();
        }

        boolean hasMore = items.size() == properties.getPageSize() && page < properties.getMaxPages();
        return new FestivalBatch<>(items, hasMore);
    }
}
