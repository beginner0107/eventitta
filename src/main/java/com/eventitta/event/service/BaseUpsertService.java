package com.eventitta.event.service;

import com.eventitta.event.domain.Event;
import com.eventitta.event.mapper.EventMapper;
import com.eventitta.event.repository.FestivalRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public abstract class BaseUpsertService<T> {

    protected final EventMapper<T> mapper;
    protected final FestivalRepository repository;
    protected final String source;
    protected final int pageSize;

    public BaseUpsertService(EventMapper<T> mapper,
                             FestivalRepository repository,
                             String source,
                             int pageSize) {
        this.mapper = mapper;
        this.repository = repository;
        this.source = source;
        this.pageSize = pageSize;
    }

    /**
     * 한 페이지를 가져와서 PageResult(items + totalCount)로 반환
     *
     * @param pageNo    1-based 페이지 번호
     * @param dateParam 연도 또는 연-월 문자열 (서비스별로 의미가 다를 수 있음)
     */
    protected abstract PageResult<T> fetchPageAndCount(int pageNo, String dateParam);

    /**
     * Upsert 전체 반복 로직:
     * 1) pageNo=1부터 fetchPageAndCount 호출 → items, totalCount 반환
     * 2) items가 비어 있거나 null이면 종료
     * 3) items 순회하며 Upsert (findBySourceAndTitleAndStartTime → update or save)
     * 4) totalCount를 보고 “더 요청해야 할 페이지가 있는지” 판단
     */
    @Transactional
    public void importAllUpsert(String dateParam) {
        int pageNo = 1;
        while (true) {
            PageResult<T> pageResult;
            try {
                pageResult = fetchPageAndCount(pageNo, dateParam);
            } catch (Exception e) {
                log.error("페이지 {} 호출 중 예외 발생: {}", pageNo, e.getMessage());
                break;
            }

            List<T> items = pageResult.items();
            if (items == null || items.isEmpty()) {
                break;
            }

            // ▶ Upsert 로직
            for (T dto : items) {
                Event newEvent = mapper.toEntity(dto, source);
                repository.findBySourceAndTitleAndStartTime(
                    newEvent.getSource(),
                    newEvent.getTitle(),
                    newEvent.getStartTime()
                ).ifPresentOrElse(
                    existing -> existing.updateFrom(newEvent),
                    () -> repository.save(newEvent)
                );
            }

            // ▶ 페이징 종료 여부 판단
            int totalCount = pageResult.totalCount();
            // 현재까지 조회한 건수
            int fetchedSoFar = pageNo * pageSize;
            if (fetchedSoFar >= totalCount) {
                break;
            }
            pageNo++;
        }
    }
}
