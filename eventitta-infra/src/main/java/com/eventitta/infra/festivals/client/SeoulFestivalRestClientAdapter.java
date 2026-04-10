package com.eventitta.infra.festivals.client;

import com.eventitta.domain.festivals.client.FestivalBatch;
import com.eventitta.domain.festivals.client.SeoulFestivalClient;
import com.eventitta.domain.festivals.dto.external.seoul.SeoulFestivalResponse;
import com.eventitta.domain.festivals.dto.external.seoul.SeoulFestivalRow;
import com.eventitta.infra.festivals.config.SeoulFestivalProperties;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SeoulFestivalRestClientAdapter implements SeoulFestivalClient {

    private final SeoulFestivalHttpApi seoulFestivalHttpApi;
    private final SeoulFestivalProperties properties;

    @Override
    public FestivalBatch<SeoulFestivalRow> fetchPage(String serviceKey, int page) {
        return fetch(serviceKey, page, null);
    }

    @Override
    public FestivalBatch<SeoulFestivalRow> fetchPageForDate(String serviceKey, int page, LocalDate targetDate) {
        return fetch(serviceKey, page, targetDate);
    }

    private FestivalBatch<SeoulFestivalRow> fetch(String serviceKey, int page, LocalDate targetDate) {
        int startIndex = (page - 1) * properties.getPageSize() + 1;
        int endIndex = page * properties.getPageSize();
        SeoulFestivalResponse response = seoulFestivalHttpApi.getSeoulEvents(
            serviceKey,
            properties.getServiceFormat(),
            properties.getServiceName(),
            startIndex,
            endIndex,
            " ",
            " ",
            targetDate != null ? targetDate.toString() : ""
        );

        List<SeoulFestivalRow> rows = List.of();
        if (response != null
            && response.culturalEventInfo() != null
            && response.culturalEventInfo().row() != null
        ) {
            rows = response.culturalEventInfo().row();
        }

        boolean hasMore = rows.size() == properties.getPageSize() && page < properties.getMaxPages();
        return new FestivalBatch<>(rows, hasMore);
    }
}
