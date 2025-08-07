package com.eventitta.festivals.service.loader;

import com.eventitta.common.external.api.SeoulFestivalApi;
import com.eventitta.festivals.config.SeoulFestivalConfig;
import com.eventitta.festivals.domain.Festival;
import com.eventitta.festivals.dto.SeoulFestivalResponse;
import com.eventitta.festivals.dto.SeoulFestivalRow;
import com.eventitta.festivals.mapper.FestivalMapper;
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

    private final SeoulFestivalApi seoulFestivalApi;
    private final FestivalMapper mapper;
    private final SeoulFestivalConfig config;

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
        PageRequest pageRequest = createPageRequest(page);
        SeoulFestivalResponse response = callSeoulApi(serviceKey, pageRequest);
        return extractRows(response);
    }

    private List<SeoulFestivalRow> fetchPageForDate(String serviceKey, int page, LocalDate targetDate) {
        PageRequest pageRequest = createPageRequest(page);
        SeoulFestivalResponse response = callSeoulApiForDate(serviceKey, pageRequest, targetDate);
        return extractRows(response);
    }

    private PageRequest createPageRequest(int page) {
        int startIndex = (page - 1) * config.getPageSize() + 1;
        int endIndex = page * config.getPageSize();
        return new PageRequest(startIndex, endIndex);
    }

    private SeoulFestivalResponse callSeoulApi(String serviceKey, PageRequest pageRequest) {
        return seoulFestivalApi.getSeoulEvents(
            serviceKey,
            config.getServiceFormat(),
            config.getServiceName(),
            pageRequest.startIndex(),
            pageRequest.endIndex(),
            " ", " ", ""
        );
    }

    private SeoulFestivalResponse callSeoulApiForDate(String serviceKey, PageRequest pageRequest, LocalDate targetDate) {
        String dateParam = targetDate != null ? targetDate.toString() : "";
        return seoulFestivalApi.getSeoulEvents(
            serviceKey,
            config.getServiceFormat(),
            config.getServiceName(),
            pageRequest.startIndex(),
            pageRequest.endIndex(),
            " ", " ", dateParam
        );
    }

    private List<SeoulFestivalRow> extractRows(SeoulFestivalResponse response) {
        List<SeoulFestivalRow> rows = response.culturalEventInfo().row();
        return rows != null ? rows : List.of();
    }

    private record PageRequest(int startIndex, int endIndex) {
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
                return hasMorePages && currentPage <= config.getMaxPages();
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
                if (targetDate != null) {
                    currentBatch = fetchPageForDate(serviceKey, currentPage, targetDate);
                } else {
                    currentBatch = fetchPage(serviceKey, currentPage);
                }
                hasMorePages = currentBatch.size() == config.getPageSize();
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
