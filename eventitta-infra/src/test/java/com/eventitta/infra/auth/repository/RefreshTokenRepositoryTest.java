package com.eventitta.infra.auth.repository;

import com.eventitta.domain.auth.domain.RefreshToken;
import com.eventitta.domain.auth.repository.RefreshTokenRepository;
import com.eventitta.domain.user.domain.Provider;
import com.eventitta.domain.user.domain.Role;
import com.eventitta.domain.user.domain.User;
import com.eventitta.infra.common.config.jpa.QuerydslConfig;
import com.eventitta.infra.user.repository.JpaUserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@ActiveProfiles("test")
@EntityScan(basePackages = "com.eventitta.domain")
@Import(QuerydslConfig.class)
@DisplayName("리프레시 토큰 저장소 슬라이스 테스트")
class RefreshTokenRepositoryTest {

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private JpaUserRepository userRepository;

    @Test
    @DisplayName("tokenKey 로 refresh token 을 조회한다")
    void findByTokenKey_returnsToken() {
        User user = userRepository.saveAndFlush(createUser("repository-expired@test.com", "repoExpired"));
        RefreshToken savedToken = refreshTokenRepository.saveAndFlush(
            RefreshToken.issue(
                user.getId(),
                "session-id-1",
                "refresh-key",
                "expired-hash",
                LocalDateTime.now().plusMinutes(10),
                LocalDateTime.now(),
                "192.168.0.0/24",
                "Mozilla/5.0"
            )
        );

        assertThat(refreshTokenRepository.findByTokenKey("refresh-key"))
            .contains(savedToken);
    }

    @Test
    @DisplayName("refresh 회전용 조회는 tokenKey 기준 row lock 대상 세션을 반환한다")
    void findByTokenKeyForUpdate_returnsSessionRow() {
        User user = userRepository.saveAndFlush(createUser("repository-lock@test.com", "repoLock"));
        RefreshToken savedToken = refreshTokenRepository.saveAndFlush(
            RefreshToken.issue(
                user.getId(),
                "session-id-lock",
                "locked-key",
                "locked-hash",
                LocalDateTime.now().plusMinutes(10),
                LocalDateTime.now(),
                "10.0.0.0/24",
                "Chrome/123.0"
            )
        );

        assertThat(refreshTokenRepository.findByTokenKeyForUpdate("locked-key"))
            .contains(savedToken);
    }

    @Test
    @DisplayName("사용자 id로 삭제하면 해당 사용자의 토큰만 제거한다")
    void deleteByUserId_deletesOnlyTargetUsersTokens() {
        User targetUser = userRepository.saveAndFlush(createUser("repository-target@test.com", "repoTarget"));
        User otherUser = userRepository.saveAndFlush(createUser("repository-other@test.com", "repoOther"));

        refreshTokenRepository.saveAndFlush(RefreshToken.issue(
            targetUser.getId(),
            "session-target-1",
            "target-key-1",
            "target-hash-1",
            LocalDateTime.now().plusDays(1),
            LocalDateTime.now(),
            null,
            null
        ));
        refreshTokenRepository.saveAndFlush(RefreshToken.issue(
            targetUser.getId(),
            "session-target-2",
            "target-key-2",
            "target-hash-2",
            LocalDateTime.now().plusDays(1),
            LocalDateTime.now(),
            null,
            null
        ));
        refreshTokenRepository.saveAndFlush(RefreshToken.issue(
            otherUser.getId(),
            "session-other-1",
            "other-key",
            "other-hash",
            LocalDateTime.now().plusDays(1),
            LocalDateTime.now(),
            null,
            null
        ));

        refreshTokenRepository.deleteByUserId(targetUser.getId());

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
