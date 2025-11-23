package com.eventitta.gamification.service;

import com.eventitta.IntegrationTestSupport;
import com.eventitta.gamification.domain.ActivityType;
import com.eventitta.user.domain.Provider;
import com.eventitta.user.domain.Role;
import com.eventitta.user.domain.User;
import com.eventitta.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class UserActivityConcurrencyTest extends IntegrationTestSupport {

    @Autowired
    private UserActivityService userActivityService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private User testUser;
    private ExecutorService executorService;

    @BeforeEach
    void setUp() {
        // 테스트 사용자 생성
        testUser = userRepository.save(User.builder()
            .email("concurrent-test@example.com")
            .password("password123")
            .nickname("concurrentUser")
            .role(Role.USER)
            .provider(Provider.LOCAL)
            .build());

        executorService = Executors.newFixedThreadPool(10);
    }

    @AfterEach
    void tearDown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }

        userRepository.deleteAll();
    }

    @Test
    @DisplayName("여러 사용자가 동시에 활동을 기록해도 포인트가 누락 없이 정확히 적립된다")
    void testConcurrentPointIncrementsWithBulkUpdate() throws InterruptedException {
        // given
        int threadCount = 5;
        int activitiesPerThread = 3;
        int pointsPerActivity = ActivityType.CREATE_POST.getDefaultPoint(); // 10포인트
        int expectedTotalPoints = threadCount * activitiesPerThread * pointsPerActivity;

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        // when
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executorService.submit(() -> {
                try {
                    startLatch.await();

                    for (int j = 0; j < activitiesPerThread; j++) {
                        try {
                            long targetId = threadId * 100L + j;
                            userActivityService.recordActivity(
                                testUser.getId(),
                                ActivityType.CREATE_POST,
                                targetId
                            );
                            successCount.incrementAndGet();
                        } catch (Exception e) {
                            errorCount.incrementAndGet();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        // 모든 스레드 동시 시작
        startLatch.countDown();

        // 모든 스레드 완료 대기 (최대 10초)
        boolean completed = endLatch.await(10, TimeUnit.SECONDS);
        assertThat(completed).isTrue();

        // then
        User updatedUser = userRepository.findById(testUser.getId()).orElseThrow();

        assertThat(updatedUser.getPoints())
            .as("Expected %d points (threads: %d × activities: %d × points: %d)",
                expectedTotalPoints, threadCount, activitiesPerThread, pointsPerActivity)
            .isEqualTo(expectedTotalPoints);

        assertThat(successCount.get())
            .as("All activities should be successfully recorded")
            .isEqualTo(threadCount * activitiesPerThread);

        assertThat(errorCount.get())
            .as("No errors should occur")
            .isEqualTo(0);
    }

    @Test
    @DisplayName("여러 사용자가 동시에 각자의 포인트를 업데이트해도 정확히 반영된다")
    void testConcurrentPointUpdatesForMultipleUsers() throws InterruptedException {
        // given - 여러 사용자 생성
        int userCount = 5;
        int operationsPerUser = 3;
        List<User> users = new ArrayList<>();

        for (int i = 0; i < userCount; i++) {
            User user = userRepository.save(User.builder()
                .email(String.format("concurrent-user-%d@example.com", i))
                .password("password123")
                .nickname("user" + i)
                .role(Role.USER)
                .provider(Provider.LOCAL)
                .build());
            users.add(user);
        }

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(userCount);
        AtomicInteger successCount = new AtomicInteger(0);

        // when - 각 사용자가 동시에 자신의 포인트를 업데이트
        for (int i = 0; i < userCount; i++) {
            final User user = users.get(i);
            final int userIndex = i;

            executorService.submit(() -> {
                try {
                    startLatch.await(); // 모든 스레드 동시 시작

                    for (int j = 0; j < operationsPerUser; j++) {
                        long targetId = (userIndex * 1000L) + j;
                        userActivityService.recordActivity(
                            user.getId(),
                            ActivityType.CREATE_POST,  // 10포인트
                            targetId
                        );
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    // 실패 시 무시 - H2의 제한으로 인한 것일 수 있음
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // 모든 스레드 시작
        boolean completed = endLatch.await(10, TimeUnit.SECONDS);
        assertThat(completed).isTrue();

        // then - 각 사용자의 포인트 확인
        int expectedPointsPerUser = operationsPerUser * 10; // 3 * 10 = 30

        for (User user : users) {
            User updatedUser = userRepository.findById(user.getId()).orElseThrow();
            assertThat(updatedUser.getPoints())
                .as("User %s should have correct points", user.getNickname())
                .isEqualTo(expectedPointsPerUser);
        }

        assertThat(successCount.get())
            .as("All operations should succeed")
            .isEqualTo(userCount * operationsPerUser);
    }

    @Test
    @DisplayName("포인트가 부족한 상황에서 여러 차감 요청이 들어와도 포인트가 음수가 되지 않는다")
    void testInsufficientPointsHandlingWithBulkUpdate() throws InterruptedException {
        // given
        // 적은 포인트로 시작 - 2개의 활동으로 20포인트 획득
        userActivityService.recordActivity(
            testUser.getId(),
            ActivityType.CREATE_POST,  // 10포인트
            2000000L
        );
        userActivityService.recordActivity(
            testUser.getId(),
            ActivityType.CREATE_POST,  // 10포인트
            2000001L
        );

        int threadCount = 5;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);
        AtomicInteger successfulDeductions = new AtomicInteger(0);
        AtomicInteger failedDeductions = new AtomicInteger(0);

        // when
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    startLatch.await();

                    // 각 스레드가 10포인트씩 차감 시도 - 트랜잭션 내에서 실행
                    Integer result = transactionTemplate.execute(status ->
                        userRepository.decrementPoints(testUser.getId(), 10)
                    );

                    if (result != null && result > 0) {
                        successfulDeductions.incrementAndGet();
                    } else {
                        failedDeductions.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = endLatch.await(10, TimeUnit.SECONDS);
        assertThat(completed).isTrue();

        // then
        User updatedUser = userRepository.findById(testUser.getId()).orElseThrow();

        // 20포인트에서 10포인트씩 차감하면 최대 2번만 성공 가능
        assertThat(successfulDeductions.get())
            .as("Only 2 deductions should succeed with 20 initial points")
            .isEqualTo(2);

        assertThat(failedDeductions.get())
            .as("3 deductions should fail due to insufficient points")
            .isEqualTo(3);

        assertThat(updatedUser.getPoints())
            .as("Points should never go negative")
            .isEqualTo(0);
    }
}
