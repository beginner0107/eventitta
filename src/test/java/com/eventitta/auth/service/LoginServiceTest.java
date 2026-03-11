package com.eventitta.auth.service;

import com.eventitta.auth.domain.UserPrincipal;
import com.eventitta.auth.exception.AuthException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import static com.eventitta.auth.exception.AuthErrorCode.INVALID_CREDENTIALS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginServiceTest {

    @InjectMocks
    private LoginService loginService;

    @Mock
    private AuthenticationManager authManager;

    @Test
    @DisplayName("이메일과 비밀번호가 올바르면 사용자 id를 반환한다.")
    void authenticate_success() {
        // given
        String email = "test@example.com";
        String rawPassword = "password123";

        UserPrincipal principal = mock(UserPrincipal.class);
        Authentication authentication = mock(Authentication.class);

        when(authManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(principal);
        when(principal.getId()).thenReturn(1L);

        // when
        Long userId = loginService.authenticate(email, rawPassword);

        // then
        assertThat(userId).isEqualTo(1L);
        verify(authManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    @DisplayName("이메일 또는 비밀번호가 올바르지 않으면 INVALID_CREDENTIALS 예외를 던진다.")
    void authenticate_fail() {
        // given
        String email = "test@example.com";
        String rawPassword = "wrong-password";

        when(authManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenThrow(new BadCredentialsException("bad credentials"));

        // when
        Throwable thrown = catchThrowable(() -> loginService.authenticate(email, rawPassword));

        // then
        assertThat(thrown).isInstanceOf(AuthException.class);
        assertThat(((AuthException) thrown).getErrorCode()).isEqualTo(INVALID_CREDENTIALS);

        verify(authManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }
}
