package com.eventitta.event.service;

import com.eventitta.event.client.NationalFestivalClient;
import com.eventitta.event.dto.NationalFestivalResponse;
import com.eventitta.event.mapper.FestivalToEventMapper;
import com.eventitta.event.repository.FestivalRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class NationalFestivalImportService extends PageBasedFestivalImportService<NationalFestivalResponse.FestivalItem> {

    public NationalFestivalImportService(NationalFestivalClient nationalClient,
                                         FestivalToEventMapper<NationalFestivalResponse.FestivalItem> mapper,
                                         FestivalRepository repository,
                                         FestivalUpsertWorker upsertWorker,
                                         @Value("${festival.national.import.source}") String source,
                                         @Value("${festival.national.import.page-size}") int pageSize) {
        super(nationalClient, mapper, repository, upsertWorker, source, pageSize);
    }

    public void importAll() {
        importAll(null);
        super.importAll(null);
    }
}
