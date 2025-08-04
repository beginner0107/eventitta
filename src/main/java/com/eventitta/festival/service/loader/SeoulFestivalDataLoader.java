package com.eventitta.festival.service.loader;

import com.eventitta.common.external.api.SeoulFestivalApi;
import com.eventitta.festival.config.SeoulFestivalConfig;
import com.eventitta.festival.domain.CulturalEvent;
import com.eventitta.festival.dto.SeoulFestivalResponse;
import com.eventitta.festival.dto.SeoulFestivalRow;
import com.eventitta.festival.mapper.CulturalEventMapper;
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
    private final CulturalEventMapper mapper;
    private final SeoulFestivalConfig config;

    public Iterator<CulturalEvent> loadEvents(String serviceKey, LocalDate cutoff) {
        return new SeoulEventIterator(serviceKey);
    }

    private List<SeoulFestivalRow> fetchPage(String serviceKey, int page) {
        PageRequest pageRequest = createPageRequest(page);
        SeoulFestivalResponse response = callSeoulApi(serviceKey, pageRequest);
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

    private List<SeoulFestivalRow> extractRows(SeoulFestivalResponse response) {
        List<SeoulFestivalRow> rows = response.getCulturalEventInfo().getRow();
        return rows != null ? rows : List.of();
    }

    private record PageRequest(int startIndex, int endIndex) {}

    private class SeoulEventIterator implements Iterator<CulturalEvent> {
        private final String serviceKey;
        private final BatchLoader batchLoader;

        public SeoulEventIterator(String serviceKey) {
            this.serviceKey = serviceKey;
            this.batchLoader = new BatchLoader();
        }

        @Override
        public boolean hasNext() {
            return batchLoader.hasNext();
        }

        @Override
        public CulturalEvent next() {
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
                currentBatch = fetchPage(serviceKey, currentPage);
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
