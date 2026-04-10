package com.eventitta.domain.gamification.service;

import com.eventitta.IntegrationTestSupport;
import com.eventitta.domain.gamification.api.internal.facade.GamificationInternalFacade;
import com.eventitta.domain.gamification.domain.RewardActionType;
import com.eventitta.domain.gamification.repository.GamificationActionRecordRepository;
import com.eventitta.domain.gamification.repository.UserActivityStatsRepository;
import com.eventitta.domain.gamification.repository.UserGamificationStatsRepository;
import com.eventitta.domain.user.domain.Provider;
import com.eventitta.domain.user.domain.Role;
import com.eventitta.domain.user.domain.User;
import com.eventitta.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import static com.eventitta.domain.gamification.domain.ActivityType.CREATE_POST;
import static org.assertj.core.api.Assertions.assertThat;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:gamification_facade_db;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;NON_KEYWORDS=VALUE,KEY,USER"
})
class DefaultGamificationFacadeIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private GamificationInternalFacade gamificationFacade;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserGamificationStatsRepository userGamificationStatsRepository;

    @Autowired
    private UserActivityStatsRepository userActivityStatsRepository;

    @Autowired
    private GamificationActionRecordRepository actionRecordRepository;

    private User user;

    @BeforeEach
    void setUp() {
        long suffix = System.nanoTime();
        user = userRepository.save(User.builder()
            .email("gamification-" + suffix + "@test.com")
            .password("password123")
            .nickname("gamificationUser" + suffix)
            .role(Role.USER)
            .provider(Provider.LOCAL)
            .build());
    }

    @Test
    @DisplayName("게시글 보상을 적립하면 stats 테이블과 action record가 함께 갱신된다")
    void onPostCreated_updatesStatsAndActionRecord() {
        gamificationFacade.onPostCreated(user.getId(), 101L);

        var totalStats = userGamificationStatsRepository.findById(user.getId()).orElseThrow();
        var actionStats = userActivityStatsRepository.findByUserIdAndActionType(
            user.getId(),
            RewardActionType.CREATE_POST
        ).orElseThrow();

        assertThat(totalStats.getTotalPoints()).isEqualTo(10);
        assertThat(totalStats.getTotalActivityCount()).isEqualTo(1L);
        assertThat(actionStats.getActionCount()).isEqualTo(1L);
        assertThat(actionStats.getPointsTotal()).isEqualTo(10);
        assertThat(actionRecordRepository.countByUserIdAndActivityType(user.getId(), CREATE_POST)).isEqualTo(1L);
    }

    @Test
    @DisplayName("같은 대상에 대한 중복 적립은 한 번만 반영된다")
    void duplicateGrant_isIgnored() {
        gamificationFacade.onPostCreated(user.getId(), 101L);
        gamificationFacade.onPostCreated(user.getId(), 101L);

        var totalStats = userGamificationStatsRepository.findById(user.getId()).orElseThrow();
        assertThat(totalStats.getTotalPoints()).isEqualTo(10);
        assertThat(totalStats.getTotalActivityCount()).isEqualTo(1L);
        assertThat(actionRecordRepository.countByUserIdAndActivityType(user.getId(), CREATE_POST)).isEqualTo(1L);
    }

    @Test
    @DisplayName("기존 보상을 회수하면 stats가 감소하고 action stat row는 정리된다")
    void revoke_decrementsStats() {
        gamificationFacade.onPostCreated(user.getId(), 101L);

        gamificationFacade.onPostDeleted(user.getId(), 101L);

        var totalStats = userGamificationStatsRepository.findById(user.getId()).orElseThrow();
        assertThat(totalStats.getTotalPoints()).isEqualTo(0);
        assertThat(totalStats.getTotalActivityCount()).isEqualTo(0L);
        assertThat(userActivityStatsRepository.findByUserIdAndActionType(
            user.getId(),
            RewardActionType.CREATE_POST
        )).isEmpty();
        assertThat(actionRecordRepository.countByUserIdAndActivityType(user.getId(), CREATE_POST)).isEqualTo(0L);
    }

    @Test
    @DisplayName("회원 탈퇴 정리 시 gamification 데이터가 함께 제거된다")
    void removeUserData_clearsGamificationState() {
        gamificationFacade.onPostCreated(user.getId(), 101L);

        gamificationFacade.removeUserData(user.getId());

        assertThat(userGamificationStatsRepository.findById(user.getId())).isEmpty();
        assertThat(userActivityStatsRepository.findByUserIdAndActionType(
            user.getId(),
            RewardActionType.CREATE_POST
        )).isEmpty();
        assertThat(actionRecordRepository.countByUserIdAndActivityType(user.getId(), CREATE_POST)).isEqualTo(0L);
    }
}
