package com.eventitta.domain.festivals.client;

import com.eventitta.domain.festivals.dto.external.seoul.SeoulFestivalRow;

import java.time.LocalDate;

public interface SeoulFestivalClient {

    FestivalBatch<SeoulFestivalRow> fetchPage(String serviceKey, int page);

    FestivalBatch<SeoulFestivalRow> fetchPageForDate(String serviceKey, int page, LocalDate targetDate);
}
