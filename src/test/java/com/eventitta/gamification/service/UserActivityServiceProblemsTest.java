package com.eventitta.gamification.service;

import com.eventitta.gamification.repository.UserActivityRepository;
import com.eventitta.user.domain.Provider;
import com.eventitta.user.domain.Role;
import com.eventitta.user.domain.User;
import com.eventitta.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static com.eventitta.gamification.domain.ActivityType.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class UserActivityServiceProblemsTest {

    @Autowired
    private UserActivityService userActivityService;

    @Autowired
    private UserActivityRepository userActivityRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    private Long testUserId;

    @BeforeEach
    void setUp() {
        // 데이터 초기화 - 별도 트랜잭션으로 처리
        cleanupData();

        // 테스트 데이터 생성 - 별도 트랜잭션으로 처리
        testUserId = createTestData();

        System.out.println("setUp 완료 - testUserId: " + testUserId);
    }

    @Transactional
    @Commit
    void cleanupData() {
        userActivityRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Transactional
    @Commit
    Long createTestData() {
        // 테스트 사용자 생성 (초기 포인트는 0)
        User testUser = userRepository.save(User.builder()
            .email("test@example.com")
            .password("pw1231231231231231231312132")
            .nickname("testuser")
            .role(Role.USER)
            .provider(Provider.LOCAL)
            .build());

        return testUser.getId();
    }

    @Test
    @DisplayName("활동 기록 및 포인트 적립 테스트")
    @Transactional
    void testRecordActivity() {
        // given: 초기 상태 확인

        // when
        userActivityService.recordActivity(testUserId, USER_LOGIN, 1L);

        // then: 포인트 추가 증가 확인
        User afterComment = userRepository.findById(testUserId).orElseThrow();
        assertThat(afterComment.getPoints()).isEqualTo(5);
    }

    @Test
    @DisplayName("활동 취소 및 포인트 차감 테스트")
    @Transactional
    void testRevokeActivity() {
        // given: 먼저 활동을 기록하여 포인트 적립
        userActivityService.recordActivity(testUserId, CREATE_COMMENT, 1L);
        entityManager.flush();
        entityManager.clear();

        User userBeforeRevoke = userRepository.findById(testUserId).orElseThrow();
        System.out.println("=== 활동 취소 및 포인트 차감 테스트 ===");
        System.out.println("취소 전 포인트: " + userBeforeRevoke.getPoints());

        // when: 댓글 활동 취소
        userActivityService.revokeActivity(testUserId, DELETE_COMMENT, 1L);
        entityManager.flush();
        entityManager.clear();

        // then: 포인트 차감 확인
        User afterRevoke = userRepository.findById(testUserId).orElseThrow();
        System.out.println("활동 취소 후 포인트: " + afterRevoke.getPoints());
    }
}
