package com.eventitta.gamification;

import com.eventitta.gamification.constant.ActivityCodes;
import com.eventitta.gamification.domain.ActivityType;
import com.eventitta.gamification.domain.UserPoints;
import com.eventitta.gamification.repository.*;
import com.eventitta.gamification.service.UserActivityService;
import com.eventitta.user.domain.Provider;
import com.eventitta.user.domain.Role;
import com.eventitta.user.domain.User;
import com.eventitta.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

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

    @Autowired
    private UserBadgeRepository userBadgeRepository;

    @Autowired
    private BadgeRuleRepository badgeRuleRepository;

    private Long testUserId;
    private ActivityType activityType;

    @BeforeEach
    void setUp() {
        userBadgeRepository.deleteAll();
        userActivityRepository.deleteAll();
        userPointsRepository.deleteAll();
        badgeRuleRepository.deleteAll();
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
        boolean finished = endLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(finished).isTrue();
        await().atMost(Duration.ofSeconds(2))
            .until(() -> userPointsRepository.findByUserId(testUserId).isPresent() &&
                userActivityRepository.countByUserId(testUserId) == 1L);
        // then
        assertThat(successCount.get()).isEqualTo(1); // 중복 활동은 한 번만 성공

        UserPoints userPoints = userPointsRepository.findByUserId(testUserId)
            .orElseThrow(() -> new AssertionError("UserPoints not found"));

        assertThat(userPoints.getPoints()).isEqualTo(activityType.getDefaultPoint());

        long activityCount = userActivityRepository.countByUserId(testUserId);
        assertThat(activityCount).isEqualTo(1L);
    }

    @Test
    @DisplayName("동시성 문제 재현 - 포인트 불일치 확인")
    void givenConcurrentActivitiesWithDifferentTargetIds_whenRecordActivity_thenAllPointsAddedCorrectly() throws InterruptedException {
        // given
        int threadCount = 20;  // 더 많은 스레드로 경합 조건 증가
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when - 동시에 서로 다른 targetId로 활동 기록
        for (int i = 0; i < threadCount; i++) {
            final long targetId = 1000L + i;  // 각각 다른 targetId
            executor.submit(() -> {
                try {
                    startLatch.await();
                    userActivityService.recordActivity(testUserId, ActivityCodes.CREATE_POST, targetId);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    log.error("활동 기록 실패: {}", e.getMessage(), e);
                    failCount.incrementAndGet();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean finished = endLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        // then - 결과 검증
        assertThat(finished).isTrue();

        Thread.sleep(1000);

        UserPoints finalPoints = userPointsRepository.findByUserId(testUserId)
            .orElseThrow(() -> new AssertionError("UserPoints not found"));

        long activityCount = userActivityRepository.countByUserId(testUserId);
        int expectedPoints = activityType.getDefaultPoint() * (int) activityCount;

        log.info("성공: {}, 실패: {}, 실제 활동 수: {}, 포인트: {}, 예상 포인트: {}",
            successCount.get(), failCount.get(), activityCount,
            finalPoints.getPoints(), expectedPoints);

        assertThat(finalPoints.getPoints()).isEqualTo(expectedPoints);
        assertThat(activityCount).isEqualTo(threadCount);
    }
}
