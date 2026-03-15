package com.eventitta.auth.repository;

import com.eventitta.auth.domain.RefreshToken;
import com.eventitta.common.config.jpa.QuerydslConfig;
import com.eventitta.user.domain.Provider;
import com.eventitta.user.domain.Role;
import com.eventitta.user.domain.User;
import com.eventitta.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(QuerydslConfig.class)
@ActiveProfiles("test")
@EntityScan(basePackages = "com.eventitta")
@DisplayName("리프레시 토큰 저장소 슬라이스 테스트")
class RefreshTokenRepositoryTest {

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("만료 시각 이전의 리프레시 토큰만 삭제한다")
    void deleteByExpiresAtBefore_deletesOnlyExpiredTokens() {
        // given
        User user = userRepository.saveAndFlush(createUser("repository-expired@test.com", "repoExpired"));
        RefreshToken expiredToken = refreshTokenRepository.saveAndFlush(
            new RefreshToken(user, "expired-hash", LocalDateTime.now().minusMinutes(1))
        );
        RefreshToken validToken = refreshTokenRepository.saveAndFlush(
            new RefreshToken(user, "valid-hash", LocalDateTime.now().plusMinutes(10))
        );

        // when
        long deletedCount = refreshTokenRepository.deleteByExpiresAtBefore(LocalDateTime.now());

        // then
        assertThat(deletedCount).isEqualTo(1);
        assertThat(refreshTokenRepository.findById(expiredToken.getId())).isEmpty();
        assertThat(refreshTokenRepository.findById(validToken.getId())).isPresent();
    }

    @Test
    @DisplayName("사용자 id로 삭제하면 해당 사용자의 토큰만 제거한다")
    void deleteByUserId_deletesOnlyTargetUsersTokens() {
        // given
        User targetUser = userRepository.saveAndFlush(createUser("repository-target@test.com", "repoTarget"));
        User otherUser = userRepository.saveAndFlush(createUser("repository-other@test.com", "repoOther"));

        refreshTokenRepository.saveAndFlush(new RefreshToken(targetUser, "target-hash-1", LocalDateTime.now().plusDays(1)));
        refreshTokenRepository.saveAndFlush(new RefreshToken(targetUser, "target-hash-2", LocalDateTime.now().plusDays(1)));
        refreshTokenRepository.saveAndFlush(new RefreshToken(otherUser, "other-hash", LocalDateTime.now().plusDays(1)));

        // when
        refreshTokenRepository.deleteByUserId(targetUser.getId());

        // then
        assertThat(refreshTokenRepository.findAllByUserId(targetUser.getId())).isEmpty();
        assertThat(refreshTokenRepository.findAllByUserId(otherUser.getId())).hasSize(1);
    }

    private User createUser(String email, String nickname) {
        return User.builder()
            .email(email)
            .password("encoded-password")
            .nickname(nickname)
            .provider(Provider.LOCAL)
            .role(Role.USER)
            .build();
    }
}
