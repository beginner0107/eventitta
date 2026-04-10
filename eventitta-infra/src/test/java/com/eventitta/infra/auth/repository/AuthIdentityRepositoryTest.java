package com.eventitta.infra.auth.repository;

import com.eventitta.domain.auth.domain.AuthIdentity;
import com.eventitta.domain.auth.domain.AuthProvider;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@ActiveProfiles("test")
@EntityScan(basePackages = "com.eventitta.domain")
@Import(QuerydslConfig.class)
class AuthIdentityRepositoryTest {

    @Autowired
    private JpaAuthIdentityRepository authIdentityRepository;

    @Autowired
    private JpaUserRepository userRepository;

    @Test
    @DisplayName("provider + providerUserId 는 유니크해야 한다")
    void save_duplicateProviderIdentity_throwsException() {
        User firstUser = userRepository.saveAndFlush(createUser("first@test.com", "firstUser"));
        User secondUser = userRepository.saveAndFlush(createUser("second@test.com", "secondUser"));

        authIdentityRepository.saveAndFlush(AuthIdentity.create(firstUser.getId(), AuthProvider.KAKAO, "provider-user-id", "first@test.com", true));

        assertThatThrownBy(() -> authIdentityRepository.saveAndFlush(
            AuthIdentity.create(secondUser.getId(), AuthProvider.KAKAO, "provider-user-id", "second@test.com", true)
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("userId 와 provider 로 identity 를 조회한다")
    void findByUserIdAndProvider_returnsIdentity() {
        User user = userRepository.saveAndFlush(createUser("user@test.com", "tester"));
        AuthIdentity identity = authIdentityRepository.saveAndFlush(
            AuthIdentity.create(user.getId(), AuthProvider.KAKAO, "provider-user-id", "user@test.com", true)
        );

        assertThat(authIdentityRepository.findByUserIdAndProvider(user.getId(), AuthProvider.KAKAO))
            .contains(identity);
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
