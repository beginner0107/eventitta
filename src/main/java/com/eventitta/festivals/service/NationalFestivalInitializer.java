package com.eventitta.festivals.service;

import com.eventitta.festivals.exception.FestivalErrorCode;
import com.eventitta.festivals.service.loader.NationalFestivalDataLoader;
import com.eventitta.festivals.service.processor.FestivalProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class NationalFestivalInitializer {

    private final NationalFestivalDataLoader dataLoader;
    private final FestivalProcessor eventProcessor;

    @Value("${api.national.key}")
    private String nationalServiceKey;

    public void loadInitialData() {
        try {
            LocalDate cutoff = calculateCutoffDate();
            var metrics = processEvents(cutoff);
            log.info("국가 문화행사 적재 완료: INSERT={}, UPDATE={}, SKIP={}, OUTDATED={}",
                metrics.getInsertCount(),
                metrics.getUpdateCount(),
                metrics.getSkipCount(),
                metrics.getOutdatedCount());
        } catch (Exception e) {
            log.error("전국 축제 데이터 로딩 중 오류 발생", e);
            throw FestivalErrorCode.DATA_SYNC_ERROR.defaultException(e);
        }
    }

    private LocalDate calculateCutoffDate() {
        return LocalDate.now().minusYears(1);
    }

    private FestivalProcessor.ProcessingMetrics processEvents(LocalDate cutoff) {
        try {
            var metrics = new FestivalProcessor.ProcessingMetrics();
            var eventIterator = dataLoader.loadEvents(nationalServiceKey);

            eventIterator.forEachRemaining(event ->
                metrics.record(eventProcessor.processEvent(event, cutoff))
            );

            return metrics;
        } catch (Exception e) {
            log.error("전국 축제 이벤트 처리 중 오류 발생", e);
            throw FestivalErrorCode.EXTERNAL_API_ERROR.defaultException(e);
        }
    }
}
