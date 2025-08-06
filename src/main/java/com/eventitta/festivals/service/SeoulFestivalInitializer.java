package com.eventitta.festivals.service;

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
        LocalDate cutoff = calculateCutoffDate();
        var metrics = processEvents(cutoff);
        metricsLogger.logSeoulResults(metrics);
    }

    /**
     * 특정 날짜의 서울시 축제 데이터만 로드 (일별 동기화용)
     */
    public void loadDataForDate(LocalDate targetDate) {
        LocalDate cutoff = calculateCutoffDate();
        var metrics = processEventsForDate(targetDate, cutoff);
        metricsLogger.logSeoulResults(metrics);
        log.info("서울시 축제 데이터 날짜별 로드 완료 - 대상 날짜: {}", targetDate);
    }

    private LocalDate calculateCutoffDate() {
        return LocalDate.now().minusYears(1);
    }

    private FestivalProcessor.ProcessingMetrics processEvents(LocalDate cutoff) {
        var metrics = new FestivalProcessor.ProcessingMetrics();
        var eventIterator = dataLoader.loadEvents(seoulServiceKey, cutoff);

        eventIterator.forEachRemaining(event ->
            metrics.record(eventProcessor.processEvent(event, cutoff))
        );

        return metrics;
    }

    private FestivalProcessor.ProcessingMetrics processEventsForDate(LocalDate targetDate, LocalDate cutoff) {
        var metrics = new FestivalProcessor.ProcessingMetrics();
        var eventIterator = dataLoader.loadEventsForDate(seoulServiceKey, targetDate, cutoff);

        eventIterator.forEachRemaining(event ->
            metrics.record(eventProcessor.processEvent(event, cutoff))
        );

        return metrics;
    }
}
