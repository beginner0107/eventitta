package com.eventitta.domain.festivals.service.loader;

import com.eventitta.domain.festivals.client.FestivalBatch;
import com.eventitta.domain.festivals.client.NationalFestivalClient;
import com.eventitta.domain.festivals.domain.Festival;
import com.eventitta.domain.festivals.dto.external.national.NationalFestivalItem;
import com.eventitta.domain.festivals.exception.FestivalErrorCode;
import com.eventitta.domain.festivals.mapper.FestivalMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class NationalFestivalDataLoader {

    private final NationalFestivalClient nationalFestivalClient;
    private final FestivalMapper mapper;

    public Iterator<Festival> loadEvents(String serviceKey) {
        return new NationalEventIterator(serviceKey);
    }

    private List<NationalFestivalItem> fetchPage(String serviceKey, int page) {
        try {
            FestivalBatch<NationalFestivalItem> batch = nationalFestivalClient.fetchPage(serviceKey, page);
            return batch.items();
        } catch (Exception e) {
            log.error("전국 축제 API 호출 중 오류 발생 - 페이지: {}", page, e);
            throw FestivalErrorCode.EXTERNAL_API_ERROR.defaultException(e);
        }
    }

    private class NationalEventIterator implements Iterator<Festival> {
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
        public Festival next() {
            NationalFestivalItem item = batchLoader.next();
            return mapper.from(item);
        }

        private class BatchLoader {
            private int currentPage = 1;
            private List<NationalFestivalItem> currentBatch = List.of();
            private int currentIndex = 0;
            private boolean hasMorePages = true;

            public BatchLoader() {
                loadNextBatch();
            }

            public boolean hasNext() {
                return hasCurrentItem() || canLoadMore();
            }

            public NationalFestivalItem next() {
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

            private NationalFestivalItem getCurrentItem() {
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
                FestivalBatch<NationalFestivalItem> batch = nationalFestivalClient.fetchPage(serviceKey, currentPage);
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
