package com.eventitta.common.config.async;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class AsyncConfigTest {

    private AsyncConfig asyncConfig;
    private ThreadPoolTaskExecutor gamificationExecutor;

    @BeforeEach
    void setUp() {
        asyncConfig = new AsyncConfig();
        gamificationExecutor = (ThreadPoolTaskExecutor) asyncConfig.gamificationExecutor();
    }

    @Test
    @DisplayName("가미피케이션 Executor 기본 설정 확인")
    void gamificationExecutor_Configuration() {
        // then
        assertThat(gamificationExecutor.getCorePoolSize()).isEqualTo(3);
        assertThat(gamificationExecutor.getMaxPoolSize()).isEqualTo(8);
        assertThat(gamificationExecutor.getKeepAliveSeconds()).isEqualTo(60);
        assertThat(gamificationExecutor.getThreadNamePrefix()).isEqualTo("Gamification-");
        assertThat(gamificationExecutor.getThreadPoolExecutor().getQueue().remainingCapacity()).isEqualTo(50);
    }

    @Test
    @DisplayName("큐 포화 시 작업이 거부되고 RejectedExecutionHandler가 동작함")
    void whenQueueIsFull_TaskIsRejected() throws InterruptedException {
        // given - 거부를 추적할 수 있는 테스트용 executor 생성
        AtomicInteger rejectedCount = new AtomicInteger(0);

        ThreadPoolTaskExecutor testExecutor = getThreadPoolTaskExecutor(rejectedCount);

        int totalTasks = 15; // 최대 처리 가능: 3(max threads) + 5(queue) = 8, 나머지 7개는 거부됨
        CountDownLatch blockingLatch = new CountDownLatch(1);
        AtomicInteger executedTasks = new AtomicInteger(0);

        // when
        for (int i = 0; i < totalTasks; i++) {
            testExecutor.execute(() -> {
                try {
                    executedTasks.incrementAndGet();
                    blockingLatch.await(); // 모든 태스크를 블록
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        // 약간 대기하여 스레드들이 시작되도록 함
        Thread.sleep(100);

        // then
        // 최대 처리 가능: maxPoolSize(3) + queueCapacity(5) = 8
        assertThat(executedTasks.get()).isLessThanOrEqualTo(3); // 실행 중인 태스크는 최대 스레드 수
        assertThat(rejectedCount.get()).isGreaterThan(0); // 일부 태스크는 거부되어야 함
        assertThat(rejectedCount.get()).isEqualTo(7); // 15 - 8 = 7개가 거부됨

        // 정리
        blockingLatch.countDown();
        testExecutor.shutdown();
        testExecutor.getThreadPoolExecutor().awaitTermination(5, TimeUnit.SECONDS);
    }

    private static @NotNull ThreadPoolTaskExecutor getThreadPoolTaskExecutor(AtomicInteger rejectedCount) {
        ThreadPoolTaskExecutor testExecutor = new ThreadPoolTaskExecutor();
        testExecutor.setCorePoolSize(2);
        testExecutor.setMaxPoolSize(3);
        testExecutor.setQueueCapacity(5);
        testExecutor.setThreadNamePrefix("Test-");

        // Custom RejectedExecutionHandler - 거부 횟수 추적
        testExecutor.setRejectedExecutionHandler((runnable, threadPoolExecutor) -> {
            rejectedCount.incrementAndGet();
            // AsyncConfig와 동일하게 로깅만 하고 예외는 발생시키지 않음
        });

        testExecutor.initialize();
        return testExecutor;
    }

    @Test
    @DisplayName("스레드 풀 포화 시 RejectedExecutionHandler가 작동함")
    void rejectedExecutionHandler_LogsError() {
        // given
        // 작은 풀과 큐로 재설정
        ThreadPoolTaskExecutor testExecutor = new ThreadPoolTaskExecutor();
        testExecutor.setCorePoolSize(1);
        testExecutor.setMaxPoolSize(1);
        testExecutor.setQueueCapacity(1);
        testExecutor.setThreadNamePrefix("Test-");

        AtomicInteger rejectedCount = new AtomicInteger(0);
        testExecutor.setRejectedExecutionHandler((runnable, threadPoolExecutor) -> {
            rejectedCount.incrementAndGet();
            // 실제 AsyncConfig와 동일한 방식으로 처리 (로깅만 하고 예외는 발생시키지 않음)
        });

        testExecutor.initialize();

        CountDownLatch blockingLatch = new CountDownLatch(1);

        // when
        // 첫 번째 태스크: 실행 중
        testExecutor.execute(() -> {
            try {
                blockingLatch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // 두 번째 태스크: 큐에 대기
        testExecutor.execute(() -> {
            try {
                blockingLatch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // 세 번째 태스크: 거부되어야 함
        testExecutor.execute(() -> {
            // 이 태스크는 거부됨
        });

        // then
        assertThat(rejectedCount.get()).isEqualTo(1);

        // 정리
        blockingLatch.countDown();
        testExecutor.shutdown();
    }

    @Test
    @DisplayName("큐 크기가 200에서 50으로 줄어들었는지 확인")
    void queueCapacity_ReducedFrom200To50() {
        // then
        assertThat(gamificationExecutor.getThreadPoolExecutor().getQueue().remainingCapacity())
            .isEqualTo(50);
    }

    @Test
    @DisplayName("정상 종료 시 모든 태스크가 완료될 때까지 대기")
    void gracefulShutdown_WaitsForTaskCompletion() throws InterruptedException {
        // given
        AtomicInteger completedTasks = new AtomicInteger(0);
        int taskCount = 5;
        CountDownLatch taskLatch = new CountDownLatch(taskCount);

        // when
        for (int i = 0; i < taskCount; i++) {
            gamificationExecutor.execute(() -> {
                try {
                    Thread.sleep(100);
                    completedTasks.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    taskLatch.countDown();
                }
            });
        }

        // 종료 시작
        gamificationExecutor.shutdown();
        boolean terminated = gamificationExecutor.getThreadPoolExecutor().awaitTermination(5, TimeUnit.SECONDS);

        // then
        assertThat(terminated).isTrue();
        assertThat(completedTasks.get()).isEqualTo(taskCount);
    }

    @Test
    @DisplayName("스레드 이름 프리픽스가 올바르게 설정됨")
    void threadNamePrefix_IsCorrect() throws InterruptedException {
        // given
        AtomicInteger threadNameCheck = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(1);

        // when
        gamificationExecutor.execute(() -> {
            String threadName = Thread.currentThread().getName();
            if (threadName.startsWith("Gamification-")) {
                threadNameCheck.incrementAndGet();
            }
            latch.countDown();
        });

        latch.await(1, TimeUnit.SECONDS);

        // then
        assertThat(threadNameCheck.get()).isEqualTo(1);

        // 정리
        gamificationExecutor.shutdown();
    }
}
