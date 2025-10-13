package com.eventitta.festivals.service.loader;

import com.eventitta.festivals.api.NationalFestivalApi;
import com.eventitta.festivals.config.NationalFestivalProperties;
import com.eventitta.festivals.domain.Festival;
import com.eventitta.festivals.dto.NationalFestivalItem;
import com.eventitta.festivals.dto.NationalFestivalResponse;
import com.eventitta.festivals.exception.FestivalErrorCode;
import com.eventitta.festivals.mapper.FestivalMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class NationalFestivalDataLoader {

    private final NationalFestivalApi nationalFestivalApi;
    private final FestivalMapper mapper;
    private final NationalFestivalProperties config;

    public Iterator<Festival> loadEvents(String serviceKey) {
        return new NationalEventIterator(serviceKey);
    }

    private List<NationalFestivalItem> fetchPage(String serviceKey, int page) {
        NationalFestivalResponse response = callNationalApi(serviceKey, page);
        return extractItems(response);
    }

    private NationalFestivalResponse callNationalApi(String serviceKey, int page) {
        try {
            return nationalFestivalApi.getFestivals(
                serviceKey,
                page,
                config.getPageSize(),
                config.getServiceFormat()
            );
        } catch (Exception e) {
            log.error("전국 축제 API 호출 중 오류 발생 - 페이지: {}", page, e);
            throw FestivalErrorCode.EXTERNAL_API_ERROR.defaultException(e);
        }
    }

    private List<NationalFestivalItem> extractItems(NationalFestivalResponse response) {
        try {
            if (response.response() != null &&
                response.response().body() != null &&
                response.response().body().items() != null) {
                return response.response().body().items();
            }
            return List.of();
        } catch (Exception e) {
            log.error("전국 축제 응답 데이터 추출 중 오류 발생", e);
            throw FestivalErrorCode.INVALID_FESTIVAL_DATA.defaultException(e);
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
                return hasMorePages && currentPage <= config.getMaxPages();
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
