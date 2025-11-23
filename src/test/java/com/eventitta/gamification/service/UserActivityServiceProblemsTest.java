package com.eventitta.gamification.service;

import com.eventitta.IntegrationTestSupport;
import com.eventitta.user.domain.Provider;
import com.eventitta.user.domain.Role;
import com.eventitta.user.domain.User;
import com.eventitta.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import static com.eventitta.gamification.domain.ActivityType.*;
import static org.assertj.core.api.Assertions.assertThat;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class UserActivityServiceProblemsTest extends IntegrationTestSupport {

    @Autowired
    private UserActivityService userActivityService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    private User testUser;

    @BeforeEach
    void setUp() {
        // 테스트 데이터 생성
        testUser = userRepository.save(User.builder()
            .email("test@example.com")
            .password("pw1231231231231231231312132")
            .nickname("testuser")
            .role(Role.USER)
            .provider(Provider.LOCAL)
            .build());
    }

    @Test
    @DisplayName("활동 기록 및 포인트 적립 테스트")
    @Transactional
    void testRecordActivity() {
        // given: 초기 상태 확인

        // when
        userActivityService.recordActivity(testUser.getId(), USER_LOGIN, 1L);

        // then: 포인트 추가 증가 확인
        User afterComment = userRepository.findById(testUser.getId()).orElseThrow();
        assertThat(afterComment.getPoints()).isEqualTo(5);
    }

    @Test
    @DisplayName("활동 취소 및 포인트 차감 테스트")
    @Transactional
    void testRevokeActivity() {
        // given: 먼저 활동을 기록하여 포인트 적립
        userActivityService.recordActivity(testUser.getId(), CREATE_COMMENT, 1L);
        entityManager.flush();
        entityManager.clear();

        User userBeforeRevoke = userRepository.findById(testUser.getId()).orElseThrow();
        assertThat(userBeforeRevoke.getPoints()).isEqualTo(5);

        // when: 같은 활동을 취소 (CREATE_COMMENT를 취소)
        userActivityService.revokeActivity(testUser.getId(), CREATE_COMMENT, 1L);
        entityManager.flush();
        entityManager.clear();

        // then: 포인트 차감 확인
        User afterRevoke = userRepository.findById(testUser.getId()).orElseThrow();
        assertThat(afterRevoke.getPoints()).isEqualTo(0);
    }
}
