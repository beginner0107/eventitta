package com.eventitta.gamification;

import com.eventitta.gamification.constant.ActivityCodes;
import com.eventitta.gamification.domain.ActivityType;
import com.eventitta.gamification.domain.UserPoints;
import com.eventitta.gamification.repository.ActivityTypeRepository;
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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class UserActivityConcurrencyTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserActivityService userActivityService;

    @Autowired
    private ActivityTypeRepository activityTypeRepository;

    @Autowired
    private UserPointsRepository userPointsRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        activityTypeRepository.deleteAll();
        userPointsRepository.deleteAll();

        testUser = userRepository.save(User.builder()
            .email("test@example.com")
            .password("password1234")
            .nickname("testuser")
            .role(Role.USER)
            .provider(Provider.LOCAL)
            .build());

        if (activityTypeRepository.findByCode(ActivityCodes.CREATE_POST).isEmpty()) {
            activityTypeRepository.save(new ActivityType(ActivityCodes.CREATE_POST, "게시글 작성", 10));
        }
    }

    @Test
    @DisplayName("동시에 두 활동이 발생하면 포인트가 중복되지 않고 정확히 합산된다.")
    void givenConcurrentActivities_whenRecordActivity_thenPointsAddedOncePerActivity() throws InterruptedException {
        // given
        Long userId = testUser.getId();
        Long targetId1 = 100L;
        Long targetId2 = 101L;

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(2);

        // when
        executor.submit(() -> {
            try {
                userActivityService.recordActivity(userId, ActivityCodes.CREATE_POST, targetId1);
            } finally {
                latch.countDown();
            }
        });

        executor.submit(() -> {
            try {
                userActivityService.recordActivity(userId, ActivityCodes.CREATE_POST, targetId2);
            } finally {
                latch.countDown();
            }
        });

        latch.await();
        executor.shutdown();

        // then
        UserPoints userPoints = userPointsRepository.findById(userId)
            .orElseThrow(() -> new IllegalStateException("UserPoints not found"));

        ActivityType activityType = activityTypeRepository.findByCode(ActivityCodes.CREATE_POST)
            .orElseThrow(() -> new IllegalStateException("ActivityType not found"));

        int expected = activityType.getDefaultPoint() * 2;
        assertThat(userPoints.getPoints()).isEqualTo(expected);
    }
}
