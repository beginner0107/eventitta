package com.eventitta.domain.festivals.client;

import com.eventitta.domain.festivals.dto.external.national.NationalFestivalItem;

public interface NationalFestivalClient {

    FestivalBatch<NationalFestivalItem> fetchPage(String serviceKey, int page);
}
