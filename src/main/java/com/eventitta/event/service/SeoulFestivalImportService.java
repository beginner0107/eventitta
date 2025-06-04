package com.eventitta.event.service;

import com.eventitta.event.client.SeoulApiClient;
import com.eventitta.event.dto.SeoulApiResponse;
import com.eventitta.event.mapper.EventMapper;
import com.eventitta.event.repository.FestivalRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.eventitta.event.dto.SeoulApiResponse.SeoulEventItem;

@Slf4j
@Service
public class SeoulFestivalImportService
    extends BaseUpsertService<SeoulEventItem> {

    private final SeoulApiClient seoulApiClient;

    public SeoulFestivalImportService(
        @Value("${festival.seoul.import.source}") String source,
        @Value("${festival.seoul.import.page-size}") int pageSize,
        EventMapper<SeoulEventItem> mapper,
        FestivalRepository repository,
        SeoulApiClient seoulApiClient
    ) {
        super(mapper, repository, source, pageSize);
        this.seoulApiClient = seoulApiClient;
    }

    @Override
    protected PageResult<SeoulEventItem> fetchPageAndCount(int pageNo, String dateParam) {
        int startIndex = (pageNo - 1) * pageSize + 1;
        int endIndex = pageNo * pageSize;

        // 1) 서울 API 호출
        SeoulApiResponse.SeoulResponseWrapper wrapper =
            seoulApiClient.fetchSeoulPage(
                startIndex,
                endIndex,
                "%20",
                "%20",
                dateParam
            );

        if (wrapper == null) {
            throw new RuntimeException("서울시 API 호출 결과가 null");
        }

        List<SeoulEventItem> items = wrapper.getItems();
        int totalCount = wrapper.getTotalCount();

        return new PageResult<>(items, totalCount);
    }

    // ▶ 연도 단위 Upsert 호출 예시
    public void upsertByYear(String year) {
        importAllUpsert(year);
    }

    // ▶ 월 단위 Upsert 호출 예시
    public void upsertByYearMonth(String yearMonth) {
        importAllUpsert(yearMonth);
    }

}
