package com.eventitta.auth.service;

import com.eventitta.IntegrationTestSupport;
import com.eventitta.auth.dto.request.SignUpRequest;
import com.eventitta.auth.exception.AuthException;
import com.eventitta.user.domain.Provider;
import com.eventitta.user.domain.Role;
import com.eventitta.user.domain.User;
import com.eventitta.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static com.eventitta.auth.exception.AuthErrorCode.CONFLICTED_EMAIL;
import static com.eventitta.auth.exception.AuthErrorCode.CONFLICTED_NICKNAME;
import static org.assertj.core.api.Assertions.*;

class SignUpServiceTest extends IntegrationTestSupport {

    @Autowired
    private SignUpService signUpService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @AfterEach
    void tearDown() {
        userRepository.deleteAllInBatch();
    }

    @DisplayName("회원가입 화면에서 필요한 정보를 입력하면 회원가입이 성공한다.")
    @Test
    void 회원가입_성공() {
        // given
        SignUpRequest signUpRequest = createRegisterRequest("test@naver.com", "password1234!", "nickname12");

        // when
        signUpService.register(signUpRequest);

        // then
        List<User> users = userRepository.findAll();

        assertThat(users).hasSize(1)
            .extracting("email", "nickname")
            .contains(tuple(signUpRequest.email(),
                signUpRequest.nickname()));
    }

    @DisplayName("회원가입을 수행할 때 이메일이 중복되면 예외가 발생한다.")
    @Test
    void 중복된_이메일_회원가입_실패() {
        // given
        String duplicatedEmail = "test@naver.com";
        User user = createUser(duplicatedEmail, "nickname1");
        userRepository.save(user);
        SignUpRequest signUpRequest = createRegisterRequest(duplicatedEmail, "password1234!", "nickname13");

        // when & then
        assertThatThrownBy(() -> signUpService.register(signUpRequest))
            .isInstanceOf(AuthException.class)
            .hasMessageContaining(CONFLICTED_EMAIL.defaultMessage());
    }

    @DisplayName("회원가입을 수행할 때 닉네임이 중복되면 예외가 발생한다.")
    @Test
    void 중복된_닉네임_회원가입_실패() {
        // given
        String duplicatedNickname = "nickname1";
        User user = createUser("test@naver.com", duplicatedNickname);
        userRepository.save(user);
        SignUpRequest signUpRequest = createRegisterRequest("test2@naver.com", "password1234!", duplicatedNickname);

        // when & then
        assertThatThrownBy(() -> signUpService.register(signUpRequest))
            .isInstanceOf(AuthException.class)
            .hasMessageContaining(CONFLICTED_NICKNAME.defaultMessage());
    }

    private User createUser(String email, String nickname) {
        return User.builder()
            .email(email)
            .password(passwordEncoder.encode("testpassword123"))
            .nickname(nickname)
            .role(Role.USER)
            .provider(Provider.LOCAL)
            .build();
    }

    private SignUpRequest createRegisterRequest(String email, String password, String nickname) {
        return new SignUpRequest(email, password, nickname);
    }
}
