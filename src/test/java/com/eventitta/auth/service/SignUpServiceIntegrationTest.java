package com.eventitta.auth.service;

import com.eventitta.IntegrationTestSupport;
import com.eventitta.auth.service.dto.SignUpCommand;
import com.eventitta.user.domain.Provider;
import com.eventitta.user.domain.Role;
import com.eventitta.user.domain.User;
import com.eventitta.user.exception.UserErrorCode;
import com.eventitta.user.exception.UserException;
import com.eventitta.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("회원가입 서비스 통합 테스트")
@Transactional
class SignUpServiceIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private SignUpService signUpService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("회원가입에 성공하면 암호화된 비밀번호와 기본 속성으로 사용자가 저장된다")
    void register_persistsUserWithEncodedPassword() {
        // given
        SignUpCommand command = new SignUpCommand(
            "signup-success@test.com",
            "Password123!",
            "signupTester"
        );

        // when
        User savedUser = signUpService.register(command);

        // then
        User persistedUser = userRepository.findById(savedUser.getId()).orElseThrow();
        assertThat(persistedUser.getEmail()).isEqualTo(command.email());
        assertThat(persistedUser.getNickname()).isEqualTo(command.nickname());
        assertThat(passwordEncoder.matches(command.password(), persistedUser.getPassword())).isTrue();
        assertThat(persistedUser.getPassword()).isNotEqualTo(command.password());
        assertThat(persistedUser.getRole()).isEqualTo(Role.USER);
        assertThat(persistedUser.getProvider()).isEqualTo(Provider.LOCAL);
        assertThat(persistedUser.getPoints()).isZero();
        assertThat(persistedUser.isDeleted()).isFalse();
    }

    @Test
    @DisplayName("이미 사용 중인 이메일이면 회원가입을 거부한다")
    void register_throwsConflictWhenEmailAlreadyExists() {
        // given
        userRepository.saveAndFlush(createUser("duplicated-email@test.com", "anotherNickname"));
        SignUpCommand command = new SignUpCommand(
            "duplicated-email@test.com",
            "Password123!",
            "signupTester"
        );

        // when // then
        assertThatThrownBy(() -> signUpService.register(command))
            .isInstanceOf(UserException.class)
            .extracting("errorCode")
            .isEqualTo(UserErrorCode.CONFLICTED_EMAIL);
    }

    @Test
    @DisplayName("이미 사용 중인 닉네임이면 회원가입을 거부한다")
    void register_throwsConflictWhenNicknameAlreadyExists() {
        // given
        userRepository.saveAndFlush(createUser("another-email@test.com", "duplicatedNickname"));
        SignUpCommand command = new SignUpCommand(
            "signup-nickname@test.com",
            "Password123!",
            "duplicatedNickname"
        );

        // when // then
        assertThatThrownBy(() -> signUpService.register(command))
            .isInstanceOf(UserException.class)
            .extracting("errorCode")
            .isEqualTo(UserErrorCode.CONFLICTED_NICKNAME);
    }

    private User createUser(String email, String nickname) {
        return User.builder()
            .email(email)
            .password(passwordEncoder.encode("Password123!"))
            .nickname(nickname)
            .role(Role.USER)
            .provider(Provider.LOCAL)
            .build();
    }
}
