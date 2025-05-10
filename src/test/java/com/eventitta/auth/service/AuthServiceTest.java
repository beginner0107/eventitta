package com.eventitta.auth.service;

import com.eventitta.IntegrationTestSupport;
import com.eventitta.auth.dto.request.SignUpRequest;
import com.eventitta.auth.exception.AuthException;
import com.eventitta.common.exception.CustomException;
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

@DisplayName("회원 인증/인가 비즈니스 로직 테스트")
class AuthServiceTest extends IntegrationTestSupport {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @AfterEach
    void tearDown() {
        userRepository.deleteAllInBatch();
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

    private SignUpRequest createSignUpRequest(String email, String password, String nickname) {
        return new SignUpRequest(email, password, nickname);
    }
}
