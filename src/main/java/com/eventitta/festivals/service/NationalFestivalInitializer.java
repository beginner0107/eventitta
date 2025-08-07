package com.eventitta.festivals.service;

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
    private final FestivalMetricsLogger metricsLogger;

    @Value("${api.national.key}")
    private String nationalServiceKey;

    public void loadInitialData() {
        LocalDate cutoff = calculateCutoffDate();
        var metrics = processEvents(cutoff);
        metricsLogger.logNationalResults(metrics);
    }

    private LocalDate calculateCutoffDate() {
        return LocalDate.now().minusYears(1);
    }

    private FestivalProcessor.ProcessingMetrics processEvents(LocalDate cutoff) {
        var metrics = new FestivalProcessor.ProcessingMetrics();
        var eventIterator = dataLoader.loadEvents(nationalServiceKey);

        eventIterator.forEachRemaining(event ->
            metrics.record(eventProcessor.processEvent(event, cutoff))
        );

        return metrics;
    }
}
