package com.eventitta.festivals.service;

import com.eventitta.festivals.service.processor.FestivalProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class FestivalMetricsLogger {

    public void logSeoulResults(FestivalProcessor.ProcessingMetrics metrics) {
        log.info("서울시 문화행사 적재 완료: INSERT={}, UPDATE={}, SKIP={}, OUTDATED={}",
            metrics.getInsertCount(),
            metrics.getUpdateCount(),
            metrics.getSkipCount(),
            metrics.getOutdatedCount());
    }

    public void logNationalResults(FestivalProcessor.ProcessingMetrics metrics) {
        log.info("국가 문화행사 적재 완료: INSERT={}, UPDATE={}, SKIP={}, OUTDATED={}",
            metrics.getInsertCount(),
            metrics.getUpdateCount(),
            metrics.getSkipCount(),
            metrics.getOutdatedCount());
    }
}
