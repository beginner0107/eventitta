package com.eventitta.festival.service.loader;

import com.eventitta.common.external.api.NationalFestivalApi;
import com.eventitta.event.dto.response.NationalFestivalResponse;
import com.eventitta.festival.config.NationalFestivalConfig;
import com.eventitta.festival.domain.CulturalEvent;
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
public class NationalFestivalDataLoader {

    private final NationalFestivalApi nationalFestivalApi;
    private final CulturalEventMapper mapper;
    private final NationalFestivalConfig config;

    public Iterator<CulturalEvent> loadEvents(String serviceKey, LocalDate cutoff) {
        return new NationalEventIterator(serviceKey);
    }

    private List<NationalFestivalResponse.FestivalItem> fetchPage(String serviceKey, int page) {
        NationalFestivalResponse response = callNationalApi(serviceKey, page);
        return extractItems(response);
    }

    private NationalFestivalResponse callNationalApi(String serviceKey, int page) {
        return nationalFestivalApi.getFestivals(
            serviceKey,
            page,
            config.getPageSize(),
            config.getServiceFormat()
        );
    }

    private List<NationalFestivalResponse.FestivalItem> extractItems(NationalFestivalResponse response) {
        if (response.getResponse() != null &&
            response.getResponse().getBody() != null &&
            response.getResponse().getBody().getItems() != null) {
            return response.getResponse().getBody().getItems();
        }
        return List.of();
    }

    private class NationalEventIterator implements Iterator<CulturalEvent> {
        private final String serviceKey;
        private final BatchLoader batchLoader;

        public NationalEventIterator(String serviceKey) {
            this.serviceKey = serviceKey;
            this.batchLoader = new BatchLoader();
        }

        @Override
        public boolean hasNext() {
            return batchLoader.hasNext();
        }

        @Override
        public CulturalEvent next() {
            NationalFestivalResponse.FestivalItem item = batchLoader.next();
            return mapper.from(item);
        }

        private class BatchLoader {
            private int currentPage = 1;
            private List<NationalFestivalResponse.FestivalItem> currentBatch = List.of();
            private int currentIndex = 0;
            private boolean hasMorePages = true;

            public BatchLoader() {
                loadNextBatch();
            }

            public boolean hasNext() {
                return hasCurrentItem() || canLoadMore();
            }

            public NationalFestivalResponse.FestivalItem next() {
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

            private NationalFestivalResponse.FestivalItem getCurrentItem() {
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
