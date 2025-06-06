package com.eventitta.event.service;

import com.eventitta.event.client.SeoulFestivalClient;
import com.eventitta.event.mapper.FestivalToEventMapper;
import com.eventitta.event.repository.FestivalRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.util.List;

import static com.eventitta.event.dto.SeoulFestivalResponse.SeoulEventItem;

@Slf4j
@Service
public class SeoulFestivalImportService extends PageBasedFestivalImportService<SeoulEventItem> {

    private final SeoulFestivalClient seoulClient;

    public SeoulFestivalImportService(SeoulFestivalClient seoulClient,
                                      FestivalToEventMapper<SeoulEventItem> mapper,
                                      FestivalRepository repository,
                                      FestivalUpsertWorker upsertWorker,
                                      @Value("${festival.seoul.import.source}") String source,
                                      @Value("${festival.seoul.import.page-size}") int pageSize) {
        super(mapper, repository, upsertWorker, source, pageSize);
        this.seoulClient = seoulClient;
    }

    @Override
    protected PageResult<SeoulEventItem> fetchPageAndCount(int pageNo, String dateParam) {
        int startIndex = (pageNo - 1) * pageSize + 1;
        int endIndex = pageNo * pageSize;

        var wrapper = seoulClient.fetchPage(startIndex, endIndex, "%20", "%20", dateParam);
        if (wrapper == null) {
            throw new RuntimeException("SeoulFestivalImportService: API 호출 결과가 null");
        }
        List<SeoulEventItem> items = wrapper.getItems();
        int totalCount = wrapper.getTotalCount();
        return new PageResult<>(items, totalCount);
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
