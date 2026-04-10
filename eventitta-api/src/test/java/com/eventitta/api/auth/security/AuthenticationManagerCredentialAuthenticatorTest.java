package com.eventitta.api.auth.security;

import com.eventitta.api.auth.security.UserPrincipal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import static com.eventitta.domain.auth.exception.AuthErrorCode.INVALID_CREDENTIALS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AuthenticationManagerCredentialAuthenticatorTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthenticationManagerCredentialAuthenticator authenticator;

    @Test
    @DisplayName("AuthenticationManager 인증 성공 시 principal 의 userId 를 반환한다")
    void authenticate_success() {
        UserPrincipal principal = new UserPrincipal(1L, "user@test.com", "USER");
        given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .willReturn(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

        Long userId = authenticator.authenticate("user@test.com", "password1234!@@");

        assertThat(userId).isEqualTo(1L);
    }

    @Test
    @DisplayName("AuthenticationManager 인증 실패는 INVALID_CREDENTIALS 로 변환한다")
    void authenticate_failure() {
        given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .willThrow(new BadCredentialsException("bad credentials"));

        assertThatThrownBy(() -> authenticator.authenticate("user@test.com", "wrong"))
            .extracting("errorCode")
            .isEqualTo(INVALID_CREDENTIALS);
    }
}
