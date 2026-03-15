package com.eventitta.user.repository;

import com.eventitta.common.config.jpa.QuerydslConfig;
import com.eventitta.user.domain.Provider;
import com.eventitta.user.domain.Role;
import com.eventitta.user.domain.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(QuerydslConfig.class)
@ActiveProfiles("test")
@EntityScan(basePackages = "com.eventitta")
@DisplayName("사용자 저장소 슬라이스 테스트")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("findActiveByEmail 은 삭제된 사용자를 제외한다")
    void findActiveByEmail_excludesDeletedUser() {
        // given
        User deletedUser = saveUser("deleted-email@test.com", "deletedEmailUser", 0);
        deletedUser.delete();
        userRepository.flush();
        entityManager.clear();

        // when // then
        assertThat(userRepository.findActiveByEmail(deletedUser.getEmail())).isEmpty();
    }

    @Test
    @DisplayName("findActiveById 는 삭제된 사용자를 제외한다")
    void findActiveById_excludesDeletedUser() {
        // given
        User deletedUser = saveUser("deleted-id@test.com", "deletedIdUser", 0);
        deletedUser.delete();
        userRepository.flush();
        entityManager.clear();

        // when // then
        assertThat(userRepository.findActiveById(deletedUser.getId())).isEmpty();
    }

    @Test
    @DisplayName("incrementPoints 는 사용자의 포인트를 증가시키고 update count 1을 반환한다")
    void incrementPoints_updatesPoints() {
        // given
        User user = saveUser("increment@test.com", "incrementUser", 10);

        // when
        int updatedCount = userRepository.incrementPoints(user.getId(), 5);

        // then
        assertThat(updatedCount).isEqualTo(1);
        User persistedUser = userRepository.findById(user.getId()).orElseThrow();
        assertThat(persistedUser.getPoints()).isEqualTo(15);
    }

    @Test
    @DisplayName("decrementPoints 는 포인트가 충분하면 차감하고 update count 1을 반환한다")
    void decrementPoints_withEnoughPoints_updatesPoints() {
        // given
        User user = saveUser("decrement-success@test.com", "decrementUser", 10);

        // when
        int updatedCount = userRepository.decrementPoints(user.getId(), 4);

        // then
        assertThat(updatedCount).isEqualTo(1);
        User persistedUser = userRepository.findById(user.getId()).orElseThrow();
        assertThat(persistedUser.getPoints()).isEqualTo(6);
    }

    @Test
    @DisplayName("decrementPoints 는 포인트가 부족하면 update count 0을 반환하고 포인트를 유지한다")
    void decrementPoints_withInsufficientPoints_returnsZero() {
        // given
        User user = saveUser("decrement-fail@test.com", "decrementFailUser", 3);

        // when
        int updatedCount = userRepository.decrementPoints(user.getId(), 5);

        // then
        assertThat(updatedCount).isEqualTo(0);
        User persistedUser = userRepository.findById(user.getId()).orElseThrow();
        assertThat(persistedUser.getPoints()).isEqualTo(3);
    }

    @Test
    @DisplayName("findTopUsersByPoints 는 삭제되지 않은 사용자만 포인트 내림차순으로 반환한다")
    void findTopUsersByPoints_returnsOnlyActiveUsersInDescOrder() {
        // given
        User topUser = saveUser("rank-top@test.com", "topUser", 30);
        User secondUser = saveUser("rank-second@test.com", "secondUser", 20);
        User deletedUser = saveUser("rank-deleted@test.com", "deletedRankUser", 100);
        deletedUser.delete();
        userRepository.flush();
        entityManager.clear();

        // when
        var result = userRepository.findTopUsersByPoints(PageRequest.of(0, 5));

        // then
        assertThat(result).extracting(User::getId).containsExactly(topUser.getId(), secondUser.getId());
    }

    @Test
    @DisplayName("countByPointsGreaterThan 은 삭제되지 않은 사용자만 strict greater-than 로 계산한다")
    void countByPointsGreaterThan_countsOnlyActiveUsers() {
        // given
        saveUser("count-high@test.com", "highUser", 50);
        saveUser("count-low@test.com", "lowUser", 20);
        User deletedUser = saveUser("count-deleted@test.com", "deletedCountUser", 100);
        deletedUser.delete();
        userRepository.flush();
        entityManager.clear();

        // when
        long count = userRepository.countByPointsGreaterThan(30);

        // then
        assertThat(count).isEqualTo(1);
    }

    private User saveUser(String email, String nickname, int points) {
        return userRepository.saveAndFlush(User.builder()
            .email(email)
            .password("encoded-password")
            .nickname(nickname)
            .points(points)
            .provider(Provider.LOCAL)
            .role(Role.USER)
            .deleted(false)
            .build());
    }
}
