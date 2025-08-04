package com.eventitta.festival.service;

import com.eventitta.festival.service.loader.SeoulFestivalDataLoader;
import com.eventitta.festival.service.processor.CulturalEventProcessor;
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
    private final CulturalEventProcessor eventProcessor;
    private final FestivalMetricsLogger metricsLogger;

    @Value("${api.seoul.key}")
    private String seoulServiceKey;

    public void loadInitialData() {
        LocalDate cutoff = calculateCutoffDate();
        var metrics = processEvents(cutoff);
        metricsLogger.logSeoulResults(metrics);
    }

    private LocalDate calculateCutoffDate() {
        return LocalDate.now().minusYears(1);
    }

    private CulturalEventProcessor.ProcessingMetrics processEvents(LocalDate cutoff) {
        var metrics = new CulturalEventProcessor.ProcessingMetrics();
        var eventIterator = dataLoader.loadEvents(seoulServiceKey, cutoff);

        eventIterator.forEachRemaining(event ->
            metrics.record(eventProcessor.processEvent(event, cutoff))
        );

        return metrics;
    }
}
