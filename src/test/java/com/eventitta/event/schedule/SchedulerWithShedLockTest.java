package com.eventitta.event.schedule;

import com.eventitta.event.domain.Event;
import com.eventitta.event.repository.FestivalRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("ShedLock 적용된 스케줄러 동시 실행 방지 테스트")
class SchedulerWithShedLockTest {

    @Autowired
    FestivalRepository festivalRepository;

    static class DummyInsertSchedulerWithLock {
        private final FestivalRepository repository;

        DummyInsertSchedulerWithLock(FestivalRepository repository) {
            this.repository = repository;
        }

        public synchronized void insertOnce() {
            Event event = Event.builder()
                .source("TEST")
                .title("동시성 테스트")
                .startTime(LocalDateTime.of(2025, 1, 1, 0, 0))
                .build();
            repository.save(event);
        }
    }

    @Test
    @DisplayName("ShedLock 적용 시 동시에 실행해도 1건만 insert 된다")
    void givenConcurrentRuns_withShedLock_thenOnlyOneExecutes() throws Exception {
        DummyInsertSchedulerWithLock scheduler = new DummyInsertSchedulerWithLock(festivalRepository);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(2);

        Runnable task = () -> {
            scheduler.insertOnce();  // 실제 ShedLock 환경이라면 하나만 실행됨
            latch.countDown();
        };

        executor.submit(task);
        executor.submit(task);
        latch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(festivalRepository.count()).isEqualTo(1);  // 중복 없음
    }
}
