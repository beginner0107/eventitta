package com.eventitta.gamification;

import com.eventitta.gamification.constant.ActivityCodes;
import com.eventitta.gamification.domain.ActivityType;
import com.eventitta.gamification.domain.UserPoints;
import com.eventitta.gamification.repository.ActivityTypeRepository;
import com.eventitta.gamification.repository.UserActivityRepository;
import com.eventitta.gamification.repository.UserPointsRepository;
import com.eventitta.gamification.service.UserActivityService;
import com.eventitta.user.domain.Provider;
import com.eventitta.user.domain.Role;
import com.eventitta.user.domain.User;
import com.eventitta.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SpringBootTest
@ActiveProfiles("test")
class UserActivityConcurrencyTest {

    private static final Logger log = LoggerFactory.getLogger(UserActivityConcurrencyTest.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserActivityService userActivityService;

    @Autowired
    private ActivityTypeRepository activityTypeRepository;

    @Autowired
    private UserPointsRepository userPointsRepository;

    @Autowired
    private UserActivityRepository userActivityRepository;

    private Long testUserId;
    private ActivityType activityType;

    @BeforeEach
    void setUp() {
        // 데이터 초기화
        userActivityRepository.deleteAll();
        userPointsRepository.deleteAll();
        activityTypeRepository.deleteAll();
        userRepository.deleteAll();

        // 테스트 사용자 생성
        User testUser = userRepository.save(User.builder()
            .email("test@example.com")
            .password("password1234")
            .nickname("testuser")
            .role(Role.USER)
            .provider(Provider.LOCAL)
            .build());

        testUserId = testUser.getId(); // ID만 저장

        activityType = activityTypeRepository.save(
            new ActivityType(ActivityCodes.CREATE_POST, "게시글 작성", 10)
        );
    }

    @Test
    @DisplayName("동시에 여러 활동이 발생하면 포인트가 정확히 합산된다")
    void givenConcurrentActivities_whenRecordActivity_thenPointsAddedCorrectly() throws InterruptedException {
        // given
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when
        for (int i = 0; i < threadCount; i++) {
            final long targetId = 100L + i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    userActivityService.recordActivity(testUserId, ActivityCodes.CREATE_POST, targetId);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean finished = endLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        // then
        assertThat(finished).isTrue();
        assertThat(failCount.get()).isZero();

        UserPoints userPoints = userPointsRepository.findByUserId(testUserId)
            .orElseThrow(() -> new AssertionError("UserPoints not found"));

        int expectedPoints = activityType.getDefaultPoint() * threadCount;
        assertThat(userPoints.getPoints()).isEqualTo(expectedPoints);

        long activityCount = userActivityRepository.countByUserId(testUserId);
        assertThat(activityCount).isEqualTo(threadCount);
    }

    @Test
    @DisplayName("같은 targetId로 중복 활동을 시도하면 한 번만 기록된다")
    void givenDuplicateActivity_whenRecordActivity_thenRecordedOnce() throws InterruptedException {
        // given
        Long targetId = 100L;
        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        // when
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    userActivityService.recordActivity(testUserId, ActivityCodes.CREATE_POST, targetId);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    log.warn("동시성 테스트 중 예외 발생: {}", e.getMessage(), e);
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        endLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        Thread.sleep(100);

        // then
        assertThat(successCount.get()).isEqualTo(threadCount);

        UserPoints userPoints = userPointsRepository.findByUserId(testUserId)
            .orElseThrow(() -> new AssertionError("UserPoints not found"));

        assertThat(userPoints.getPoints()).isEqualTo(activityType.getDefaultPoint());

        long activityCount = userActivityRepository.countByUserId(testUserId);
        assertThat(activityCount).isEqualTo(1L);
    }
}
