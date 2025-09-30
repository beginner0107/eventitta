package com.eventitta.gamification.service;

import com.eventitta.gamification.domain.ActivityType;
import com.eventitta.gamification.domain.UserPoints;
import com.eventitta.gamification.repository.ActivityTypeRepository;
import com.eventitta.gamification.repository.UserActivityRepository;
import com.eventitta.gamification.repository.UserPointsRepository;
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

@SpringBootTest
@ActiveProfiles("test")
class UserActivityServiceProblemsTest {

    @Autowired
    private UserActivityService userActivityService;

    @Autowired
    private UserActivityRepository userActivityRepository;

    @Autowired
    private UserPointsRepository userPointsRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ActivityTypeRepository activityTypeRepository;

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
        userPointsRepository.deleteAll();
        userRepository.deleteAll();
        activityTypeRepository.deleteAll();
    }

    @Transactional
    @Commit
    Long createTestData() {
        // 테스트 사용자 생성
        User testUser = userRepository.save(User.builder()
            .email("test@example.com")
            .password("pw1231231231231231231312132")
            .nickname("testuser")
            .role(Role.USER)
            .provider(Provider.LOCAL)
            .build());

        // 초기 포인트 설정 - 같은 트랜잭션에서 처리
        userPointsRepository.save(UserPoints.of(testUser));

        // 활동 타입들 생성
        activityTypeRepository.save(new ActivityType("LOGIN", "로그인", 10));
        activityTypeRepository.save(new ActivityType("COMMENT", "댓글작성", 5));
        activityTypeRepository.save(new ActivityType("LIKE", "좋아요", 3));

        // 강제로 flush하여 DB에 확실히 저장
        entityManager.flush();

        return testUser.getId();
    }

    @Test
    @DisplayName("문제 1: REPEATABLE READ로 인한 데이터 불일치")
    @Transactional
    void JPA_Native_혼재_문제_증명() {
        // given: 초기 상태 확인
        UserPoints initialPoints = userPointsRepository.findByUserId(testUserId).orElseThrow();
        System.out.println("=== JPA와 Native SQL 혼재 문제 ===");
        System.out.println("초기 포인트: " + initialPoints.getPoints());
        System.out.println("초기 버전: " + initialPoints.getVersion());

        // when: Native SQL로 포인트 직접 업데이트
        userPointsRepository.upsertAndAddPoints(testUserId, 20);
        System.out.println("Native SQL로 20 포인트 추가 완료");

        // then: JPA로 다시 조회했을 때 문제 확인
        UserPoints afterNativeUpdate = userPointsRepository.findByUserId(testUserId).orElseThrow();
        System.out.println("JPA 조회 포인트: " + afterNativeUpdate.getPoints());
        System.out.println("JPA 조회 버전: " + afterNativeUpdate.getVersion());

        // native query로 조회
        Object[] queryResult = userPointsRepository.getCurrentPointsAndVersionByUserId(testUserId);
        Object[] actualResult = (Object[]) queryResult[0];
        Integer currentPoints = (Integer) actualResult[0];
        Long currentVersion = (Long) actualResult[1];
        System.out.println("Native SQL 조회 포인트: " + currentPoints);
        System.out.println("Native SQL 조회 버전: " + currentVersion);


        // EntityManager 클리어 후 재조회
        entityManager.clear();
        UserPoints afterClear = userPointsRepository.findByUserId(testUserId).orElseThrow();
        System.out.println("EntityManager.clear() 후 포인트: " + afterClear.getPoints());
        System.out.println("EntityManager.clear() 후 버전: " + afterClear.getVersion());

        // DB에서 직접 확인
        Object[] dbResult = (Object[]) entityManager.createNativeQuery(
                "SELECT points, version FROM user_points WHERE user_id = ?")
            .setParameter(1, testUserId)
            .getSingleResult();

        System.out.println("실제 DB 포인트: " + dbResult[0]);
        System.out.println("실제 DB 버전: " + dbResult[1]);
    }
}
