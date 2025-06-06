package com.eventitta.event.service;

import com.eventitta.event.client.FestivalApiClient;
import com.eventitta.event.mapper.FestivalToEventMapper;
import com.eventitta.event.repository.FestivalRepository;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class PageBasedFestivalImportService<D> {

    protected final FestivalApiClient<D> apiClient;
    protected final FestivalToEventMapper<D> mapper;
    protected final FestivalRepository repository;
    protected final FestivalUpsertWorker upsertWorker;
    protected final String source;
    protected final int pageSize;

    protected PageBasedFestivalImportService(FestivalApiClient<D> apiClient,
                                             FestivalToEventMapper<D> mapper,
                                             FestivalRepository repository,
                                             FestivalUpsertWorker upsertWorker,
                                             String source,
                                             int pageSize) {
        this.apiClient = apiClient;
        this.mapper = mapper;
        this.repository = repository;
        this.upsertWorker = upsertWorker;
        this.source = source;
        this.pageSize = pageSize;
    }

    /**
     * 한 페이지 분량의 리스트 + 전체 count 를 다시 반환
     *
     * @param pageNo    1-based 페이지 번호
     * @param dateParam API 별 날짜/기간 파라미터 (e.g. "2025", "2025-06", "20230101" 등)
     * @return 해당 페이지 아이템 목록 + 전체 총 개수
     */
    protected PageResult<D> fetchPageAndCount(int pageNo, String dateParam) {
        return apiClient.fetchPage(pageNo, pageSize, dateParam);
    }

    /**
     * 전체 데이터를 API에서 페이징 형태로 조회하여 Upsert를 수행
     *
     * @param dateParam “연도” 또는 “연-월” 등의 기준 파라미터
     */
    public void importAll(String dateParam) {
        int pageNo = 1;

        while (true) {
            PageResult<D> pageResponse = fetchPageAndCount(pageNo, dateParam);
            List<D> items = pageResponse.items();
            if (items == null || items.isEmpty()) {
                break;
            }

            upsertWorker.upsertSinglePage(items, source, mapper);

            int fetchedSoFar = pageNo * pageSize;
            if (fetchedSoFar >= pageResponse.totalCount()) {
                break;
            }
            pageNo++;
        }
    }
}
