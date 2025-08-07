package com.eventitta.festivals.service;

import com.eventitta.festivals.exception.FestivalErrorCode;
import com.eventitta.festivals.service.loader.SeoulFestivalDataLoader;
import com.eventitta.festivals.service.processor.FestivalProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeoulFestivalInitializer {

    private final SeoulFestivalDataLoader dataLoader;
    private final FestivalProcessor eventProcessor;
    private final FestivalMetricsLogger metricsLogger;

    @Value("${api.seoul.key}")
    private String seoulServiceKey;

    public void loadInitialData() {
        try {
            LocalDate cutoff = calculateCutoffDate();
            var metrics = processEvents(cutoff);
            metricsLogger.logSeoulResults(metrics);
        } catch (Exception e) {
            log.error("서울시 축제 데이터 로딩 중 오류 발생", e);
            throw FestivalErrorCode.DATA_SYNC_ERROR.defaultException(e);
        }
    }

    /**
     * 특정 날짜의 서울시 축제 데이터만 로드 (일별 동기화용)
     */
    public void loadDataForDate(LocalDate targetDate) {
        try {
            LocalDate cutoff = calculateCutoffDate();
            var metrics = processEventsForDate(targetDate, cutoff);
            metricsLogger.logSeoulResults(metrics);
            log.info("서울시 축제 데이터 날짜별 로드 완료 - 대상 날짜: {}", targetDate);
        } catch (Exception e) {
            log.error("서울시 축제 데이터 날짜별 로딩 중 오류 발생 - 대상 날짜: {}", targetDate, e);
            throw FestivalErrorCode.DATA_SYNC_ERROR.defaultException(e);
        }
    }

    private LocalDate calculateCutoffDate() {
        return LocalDate.now().minusYears(1);
    }

    private FestivalProcessor.ProcessingMetrics processEvents(LocalDate cutoff) {
        try {
            var metrics = new FestivalProcessor.ProcessingMetrics();
            var eventIterator = dataLoader.loadEvents(seoulServiceKey);

            eventIterator.forEachRemaining(event ->
                metrics.record(eventProcessor.processEvent(event, cutoff))
            );

            return metrics;
        } catch (Exception e) {
            log.error("서울시 축제 이벤트 처리 중 오류 발생", e);
            throw FestivalErrorCode.EXTERNAL_API_ERROR.defaultException(e);
        }
    }

    private FestivalProcessor.ProcessingMetrics processEventsForDate(LocalDate targetDate, LocalDate cutoff) {
        try {
            var metrics = new FestivalProcessor.ProcessingMetrics();
            var eventIterator = dataLoader.loadEventsForDate(seoulServiceKey, targetDate);

            eventIterator.forEachRemaining(event ->
                metrics.record(eventProcessor.processEvent(event, cutoff))
            );

            return metrics;
        } catch (Exception e) {
            log.error("서울시 축제 이벤트 날짜별 처리 중 오류 발생 - 대상 날짜: {}", targetDate, e);
            throw FestivalErrorCode.EXTERNAL_API_ERROR.defaultException(e);
        }
    }
}
