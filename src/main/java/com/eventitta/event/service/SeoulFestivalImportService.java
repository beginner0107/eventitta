package com.eventitta.event.service;

import com.eventitta.event.client.SeoulFestivalClient;
import com.eventitta.event.mapper.FestivalToEventMapper;
import com.eventitta.event.repository.FestivalRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.YearMonth;

import static com.eventitta.event.dto.SeoulFestivalResponse.SeoulEventItem;

@Slf4j
@Service
public class SeoulFestivalImportService extends PageBasedFestivalImportService<SeoulEventItem> {
    public SeoulFestivalImportService(SeoulFestivalClient seoulClient,
                                      FestivalToEventMapper<SeoulEventItem> mapper,
                                      FestivalRepository repository,
                                      FestivalUpsertWorker upsertWorker,
                                      @Value("${festival.seoul.import.source}") String source,
                                      @Value("${festival.seoul.import.page-size}") int pageSize) {
        super(seoulClient, mapper, repository, upsertWorker, source, pageSize);
    }

    public void importByYear(String year) {
        importAll(year);
    }

    public void importByYearMonth(String yearMonth) {
        importAll(yearMonth);
    }

    public void importCurrentMonth() {
        String currentYm = YearMonth.now().toString();
        importByYearMonth(currentYm);
    }
}
