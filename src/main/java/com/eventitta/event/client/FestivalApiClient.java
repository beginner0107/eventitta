package com.eventitta.event.client;

import com.eventitta.event.service.PageResult;

/**
 * 공통 페스티벌 API 클라이언트 인터페이스.
 *
 * @param <D> 아이템 DTO 타입
 */
public interface FestivalApiClient<D> {
    /**
     * API를 호출하여 한 페이지의 데이터를 조회한다.
     *
     * @param pageNo    1-based 페이지 번호
     * @param pageSize  한 페이지 크기
     * @param dateParam 날짜 파라미터 (연, 월 등 API별 형식)
     * @return 페이지 결과(아이템 목록 + 전체 개수)
     */
    PageResult<D> fetchPage(int pageNo, int pageSize, String dateParam);
}
