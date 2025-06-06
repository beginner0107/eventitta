package com.eventitta.event.service;

import com.eventitta.event.domain.Event;
import com.eventitta.event.mapper.FestivalToEventMapper;
import com.eventitta.event.repository.FestivalRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class FestivalUpsertWorker {

    private final FestivalRepository repository;

    public FestivalUpsertWorker(FestivalRepository repository) {
        this.repository = repository;
    }

    /**
     * 한 페이지 분량(dtoList)에 대해 Upsert 처리
     * <p>
     * - 첫 단계: “source|title|startTime” 을 키로 중복 DTO 제거
     * - 두 번째: DB 조회 후, 이미 존재하는 것은 update, 신규 건은 inserts 리스트에 추가
     * - 세 번째: inserts 를 saveAll() → batch insert
     *
     * @param <D>    DTO 타입 (FestivalItem or SeoulEventItem)
     * @param dtos   API DTO 목록
     * @param source “NATIONAL” or “SEOUL”
     * @param mapper DTO → Event 매핑 전략
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public <D> void upsertSinglePage(List<D> dtos, String source, FestivalToEventMapper<D> mapper) {
        // (1) 중복 키 제거용 Map
        Map<String, Event> uniqueMap = new LinkedHashMap<>();
        for (D dto : dtos) {
            Event e = mapper.toEntity(dto, source);
            String key = e.getSource() + "|" + e.getTitle() + "|" + e.getStartTime();
            uniqueMap.putIfAbsent(key, e);
        }

        // (2) 이미 DB에 있는 건 update, 신규 건만 inserts
        List<Event> inserts = new ArrayList<>();
        for (Event e : uniqueMap.values()) {
            repository.findBySourceAndTitleAndStartTime(e.getSource(), e.getTitle(), e.getStartTime())
                .ifPresentOrElse(
                    exist -> exist.updateFrom(e),
                    () -> inserts.add(e)
                );
        }

        // (3) 신규 건 batch insert
        if (!inserts.isEmpty()) {
            try {
                repository.saveAll(inserts);
                repository.flush();
            } catch (DataIntegrityViolationException ex) {
                log.warn("[FestivalUpsertWorker] batch insert 중 중복 키 예외 발생, 개별적으로 다시 삽입 시도 필요할 수 있음 → {}", ex.getMessage());
            }
        }
    }
}
