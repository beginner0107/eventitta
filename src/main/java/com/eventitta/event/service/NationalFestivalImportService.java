package com.eventitta.event.service;

import com.eventitta.event.client.NationalFestivalApiClient;
import com.eventitta.event.dto.FestivalApiResponse;
import com.eventitta.event.mapper.EventMapper;
import com.eventitta.event.repository.FestivalRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 전국 축제 API용 ImportService:
 * Upsert 전략 사용
 */
@Slf4j
@Service
public class NationalFestivalImportService
    extends BaseUpsertService<FestivalApiResponse.FestivalItem> {

    private final NationalFestivalApiClient apiClient;

    public NationalFestivalImportService(
        NationalFestivalApiClient apiClient,
        EventMapper<FestivalApiResponse.FestivalItem> mapper,
        FestivalRepository repository,
        @Value("${festival.national.import.source}") String source,
        @Value("${festival.national.import.page-size}") int pageSize
    ) {
        super(mapper, repository, source, pageSize);
        this.apiClient = apiClient;
    }

    @Override
    protected PageResult<FestivalApiResponse.FestivalItem> fetchPageAndCount(int pageNo, String dateParam) {
        // 1) API 호출
        FestivalApiResponse.Body body = apiClient.fetchFestivalPage(pageNo, pageSize);
        if (body == null) {
            throw new RuntimeException("전국 축제 API 호출이 null을 반환했습니다.");
        }

        List<FestivalApiResponse.FestivalItem> items = body.getItems();
        int totalCount = body.getTotalCount();
        return new PageResult<>(items, totalCount);
    }

    // ▶ 예시: “전체 Upsert”를 위한 편의 메서드
    public void upsertAllNational() {
        importAllUpsert(null);
    }
}
