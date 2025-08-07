package com.eventitta.festivals.service.processor;

import com.eventitta.festivals.domain.Festival;
import com.eventitta.festivals.repository.FestivalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@RequiredArgsConstructor
public class FestivalProcessor {

    private final FestivalRepository eventRepository;

    public ProcessingResult processEvent(Festival event, LocalDate cutoff) {
        if (isEventOutdated(event, cutoff)) {
            return ProcessingResult.OUTDATED;
        }

        return saveOrUpdateEvent(event);
    }

    private boolean isEventOutdated(Festival event, LocalDate cutoff) {
        return event.getEndDate() != null && event.getEndDate().isBefore(cutoff);
    }

    private ProcessingResult saveOrUpdateEvent(Festival event) {
        return eventRepository.findByExternalIdAndDataSource(event.getExternalId(), event.getDataSource())
            .map(existing -> updateExistingEvent(existing, event))
            .orElseGet(() -> insertNewEvent(event));
    }

    private ProcessingResult updateExistingEvent(Festival existing, Festival event) {
        existing.updateFestivalInfo(event);
        eventRepository.save(existing);
        return ProcessingResult.UPDATED;
    }

    private ProcessingResult insertNewEvent(Festival event) {
        eventRepository.save(event);
        return ProcessingResult.INSERTED;
    }

    public enum ProcessingResult {
        INSERTED, UPDATED, SKIPPED, OUTDATED
    }

    public static class ProcessingMetrics {
        private final AtomicInteger insertCount = new AtomicInteger(0);
        private final AtomicInteger updateCount = new AtomicInteger(0);
        private final AtomicInteger skipCount = new AtomicInteger(0);
        private final AtomicInteger outdatedCount = new AtomicInteger(0);

        public void record(ProcessingResult result) {
            switch (result) {
                case INSERTED -> insertCount.incrementAndGet();
                case UPDATED -> updateCount.incrementAndGet();
                case SKIPPED -> skipCount.incrementAndGet();
                case OUTDATED -> outdatedCount.incrementAndGet();
            }
        }

        public int getInsertCount() {
            return insertCount.get();
        }

        public int getUpdateCount() {
            return updateCount.get();
        }

        public int getSkipCount() {
            return skipCount.get();
        }

        public int getOutdatedCount() {
            return outdatedCount.get();
        }
    }
}
