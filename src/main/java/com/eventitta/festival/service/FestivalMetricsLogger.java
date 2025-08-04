package com.eventitta.festival.service;

import com.eventitta.festival.service.processor.CulturalEventProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class FestivalMetricsLogger {

    public void logSeoulResults(CulturalEventProcessor.ProcessingMetrics metrics) {
        log.info("서울시 문화행사 적재 완료: INSERT={}, UPDATE={}, SKIP={}, OUTDATED={}",
            metrics.getInsertCount(),
            metrics.getUpdateCount(),
            metrics.getSkipCount(),
            metrics.getOutdatedCount());
    }

    public void logNationalResults(CulturalEventProcessor.ProcessingMetrics metrics) {
        log.info("국가 문화행사 적재 완료: INSERT={}, UPDATE={}, SKIP={}, OUTDATED={}",
            metrics.getInsertCount(),
            metrics.getUpdateCount(),
            metrics.getSkipCount(),
            metrics.getOutdatedCount());
    }
}
