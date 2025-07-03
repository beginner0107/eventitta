package com.eventitta.gamification;

import com.eventitta.gamification.domain.ActivityType;
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

    private User testUser;

    @BeforeEach
    void setUp() {
        // 기존 데이터 정리
        userRepository.deleteAll();

        testUser = userRepository.save(User.builder()
            .email("test@example.com")
            .password("password1234")
            .nickname("testuser")
            .role(Role.USER)
            .provider(Provider.LOCAL)
            .points(0)
            .build());
    }

    @Test
    @DisplayName("동시에 두 활동이 발생하면 포인트 중 하나만 정상적으로 더해진다.")
    void givenConcurrentActivities_whenRecordActivity_thenPointsAreNotDuplicated() throws InterruptedException {
        // given
        Long userId = testUser.getId();
        Long targetId1 = 100L;
        Long targetId2 = 101L;

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(2);

        // when: 두 개의 활동을 동시에 수행
        executor.submit(() -> {
            try {
                userActivityService.recordActivity(userId, ActivityType.CREATE_POST, targetId1);
            } finally {
                latch.countDown();
            }
        });

        executor.submit(() -> {
            try {
                userActivityService.recordActivity(userId, ActivityType.CREATE_POST, targetId2);
            } finally {
                latch.countDown();
            }
        });

        latch.await();
        executor.shutdown();

        // then
        User updatedUser = userRepository.findById(userId).orElseThrow();
        int expected = ActivityType.CREATE_POST.getPoints() * 2;
        assertThat(updatedUser.getPoints()).isNotEqualTo(expected);
//        assertThat(updatedUser.getPoints()).isEqualTo(expected);
    }
}
