package com.eventitta.domain.festivals.service.loader;

import com.eventitta.domain.festivals.client.FestivalBatch;
import com.eventitta.domain.festivals.client.SeoulFestivalClient;
import com.eventitta.domain.festivals.domain.Festival;
import com.eventitta.domain.festivals.dto.external.seoul.SeoulFestivalRow;
import com.eventitta.domain.festivals.exception.FestivalErrorCode;
import com.eventitta.domain.festivals.mapper.FestivalMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Iterator;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SeoulFestivalDataLoader {

    private final SeoulFestivalClient seoulFestivalClient;
    private final FestivalMapper mapper;

    public Iterator<Festival> loadEvents(String serviceKey) {
        return new SeoulEventIterator(serviceKey);
    }

    /**
     * 특정 날짜의 서울시 축제 데이터만 로드 (일별 동기화용)
     */
    public Iterator<Festival> loadEventsForDate(String serviceKey, LocalDate targetDate) {
        return new SeoulEventIterator(serviceKey, targetDate);
    }

    private List<SeoulFestivalRow> fetchPage(String serviceKey, int page) {
        try {
            return seoulFestivalClient.fetchPage(serviceKey, page).items();
        } catch (Exception e) {
            log.error("서울시 축제 API 호출 중 오류 발생 - 페이지: {}", page, e);
            throw FestivalErrorCode.EXTERNAL_API_ERROR.defaultException(e);
        }
    }

    private List<SeoulFestivalRow> fetchPageForDate(String serviceKey, int page, LocalDate targetDate) {
        try {
            return seoulFestivalClient.fetchPageForDate(serviceKey, page, targetDate).items();
        } catch (Exception e) {
            log.error("서울시 축제 API 날짜별 호출 중 오류 발생 - 날짜: {}, 페이지: {}", targetDate, page, e);
            throw FestivalErrorCode.EXTERNAL_API_ERROR.defaultException(e);
        }
    }

    private class SeoulEventIterator implements Iterator<Festival> {
        private final String serviceKey;
        private final LocalDate targetDate;
        private final BatchLoader batchLoader;

        public SeoulEventIterator(String serviceKey, LocalDate targetDate) {
            this.serviceKey = serviceKey;
            this.targetDate = targetDate;
            this.batchLoader = new BatchLoader();
        }

        public SeoulEventIterator(String serviceKey) {
            this(serviceKey, null);
        }

        @Override
        public boolean hasNext() {
            return batchLoader.hasNext();
        }

        @Override
        public Festival next() {
            SeoulFestivalRow row = batchLoader.next();
            return mapper.from(row);
        }

        private class BatchLoader {
            private int currentPage = 1;
            private List<SeoulFestivalRow> currentBatch = List.of();
            private int currentIndex = 0;
            private boolean hasMorePages = true;

            public BatchLoader() {
                loadNextBatch();
            }

            public boolean hasNext() {
                return hasCurrentItem() || canLoadMore();
            }

            public SeoulFestivalRow next() {
                ensureCurrentItem();
                return getCurrentItem();
            }

            private boolean hasCurrentItem() {
                return currentIndex < currentBatch.size();
            }

            private boolean canLoadMore() {
                return hasMorePages;
            }

            private void ensureCurrentItem() {
                if (!hasCurrentItem()) {
                    loadNextBatch();
                }
            }

            private SeoulFestivalRow getCurrentItem() {
                return currentBatch.get(currentIndex++);
            }

            private void loadNextBatch() {
                if (!canLoadMore()) {
                    return;
                }

                fetchAndUpdateBatch();
                resetBatchPosition();
                moveToNextPage();
            }

            private void fetchAndUpdateBatch() {
                FestivalBatch<SeoulFestivalRow> batch = targetDate != null
                    ? seoulFestivalClient.fetchPageForDate(serviceKey, currentPage, targetDate)
                    : seoulFestivalClient.fetchPage(serviceKey, currentPage);
                currentBatch = batch.items();
                hasMorePages = batch.hasMore();
            }

            private void resetBatchPosition() {
                currentIndex = 0;
            }

            private void moveToNextPage() {
                currentPage++;
            }
        }
    }
}
