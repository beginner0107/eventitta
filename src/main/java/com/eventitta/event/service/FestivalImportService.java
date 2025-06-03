package com.eventitta.event.service;

import com.eventitta.event.client.FestivalApiClient;
import com.eventitta.event.domain.Event;
import com.eventitta.event.dto.FestivalApiResponse;
import com.eventitta.event.mapper.FestivalMapper;
import com.eventitta.event.repository.FestivalRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 기존 delete-then-insert 전략
 */
@Service
public class FestivalImportService {

    private final FestivalApiClient apiClient;
    private final FestivalMapper mapper;
    private final FestivalRepository repository;

    @Value("${festival.import.source:SEOUL}")
    private String source;

    @Value("${festival.api.page-size:100}")
    private int numOfRows;

    public FestivalImportService(FestivalApiClient apiClient,
                                 FestivalMapper mapper,
                                 FestivalRepository repository) {
        this.apiClient = apiClient;
        this.mapper = mapper;
        this.repository = repository;
    }

    /**
     * "전체 삭제 후 삽입" 방식
     * 1) 먼저 repository.deleteAllInBatch() 로 테이블 삭제
     * 2) API에서 가져온 모든 페이지 데이터를 순차적으로 insert
     */
    @Transactional
    public void importAllFestivalsInsert() {
        // 1) 기존 데이터를 모두 삭제
        repository.deleteAllInBatch();

        int pageNo = 1;

        while (true) {
            FestivalApiResponse.Body body = apiClient.fetchFestivalPage(pageNo, numOfRows);
            if (body == null) {
                break;
            }

            List<FestivalApiResponse.FestivalItem> items = body.getItems();
            if (items == null || items.isEmpty()) {
                break;
            }

            // 2) 페이지 내 모든 아이템을 순차적으로 Entity 로 변환 후 save()
            for (FestivalApiResponse.FestivalItem dto : items) {
                Event newEvent = mapper.toEntity(dto, source);
                repository.save(newEvent);
            }

            int totalCount = body.getTotalCount();
            int fetchedCountSoFar = pageNo * numOfRows;
            if (fetchedCountSoFar >= totalCount) {
                break;
            }
            pageNo++;
        }
    }
}
