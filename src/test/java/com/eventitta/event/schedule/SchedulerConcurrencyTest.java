package com.eventitta.event.schedule;

import com.eventitta.event.domain.Event;
import com.eventitta.event.repository.FestivalRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("스케줄러 동시성 테스트")
class SchedulerConcurrencyTest {

    @Autowired
    FestivalRepository festivalRepository;

    static class DummyInsertScheduler {
        private final FestivalRepository repository;

        DummyInsertScheduler(FestivalRepository repository) {
            this.repository = repository;
        }

        @Transactional
        public void insertFixedEvent() {
            Event event = Event.builder()
                .source("TEST")
                .title("동시성 테스트")
                .startTime(LocalDateTime.of(2025, 1, 1, 0, 0))
                .build();
            repository.save(event);
        }
    }

    @Test
    @DisplayName("스케줄러가 동시에 실행되면 하나만 성공하고 나머지는 중복 예외가 발생한다")
    void givenConcurrentRuns_whenInsert_thenDetectDuplicate() throws Exception {
        DummyInsertScheduler scheduler = new DummyInsertScheduler(festivalRepository);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);
        AtomicBoolean duplicateError = new AtomicBoolean(false);

        Runnable task = () -> {
            try {
                startLatch.await();
                scheduler.insertFixedEvent();
            } catch (DataIntegrityViolationException e) {
                duplicateError.set(true);
            } catch (InterruptedException ignore) {
            } finally {
                doneLatch.countDown();
            }
        };

        executor.submit(task);
        executor.submit(task);
        startLatch.countDown();
        doneLatch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(duplicateError.get()).isTrue();
        assertThat(festivalRepository.count()).isEqualTo(1);
    }
}
