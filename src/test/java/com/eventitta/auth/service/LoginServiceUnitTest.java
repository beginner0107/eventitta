package com.eventitta.auth.service;

import com.eventitta.auth.domain.UserPrincipal;
import com.eventitta.auth.exception.AuthException;
import com.eventitta.user.domain.Provider;
import com.eventitta.user.domain.Role;
import com.eventitta.user.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static com.eventitta.auth.exception.AuthErrorCode.INVALID_CREDENTIALS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("로그인 서비스 단위 테스트")
class LoginServiceUnitTest {

    @InjectMocks
    private LoginService loginService;

    @Mock
    private AuthenticationManager authManager;

    @Test
    @DisplayName("유효한 자격으로 인증하면 회원의 ID를 반환한다")
    void 존재하는_회원_인증_성공() {
        // given
        User user = User.builder()
            .id(99L)
            .email("user1234@e.com")
            .password("encryptedPassword")
            .nickname("nick")
            .role(Role.USER)
            .provider(Provider.LOCAL)
            .build();
        UserPrincipal principal = new UserPrincipal(user);

        Authentication auth = new UsernamePasswordAuthenticationToken(
            principal,
            null,
            List.of(new SimpleGrantedAuthority("USER"))
        );
        given(authManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .willReturn(auth);

        // when
        Long userId = loginService.authenticate("user1234@e.com", "password");

        // then
        assertThat(userId).isEqualTo(99L);

        ArgumentCaptor<UsernamePasswordAuthenticationToken> captor =
            ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
        then(authManager).should().authenticate(captor.capture());
        UsernamePasswordAuthenticationToken token = captor.getValue();
        assertThat(token.getName()).isEqualTo("user1234@e.com");
        assertThat(token.getCredentials()).isEqualTo("password");
    }

    @Test
    @DisplayName("유효하지 않은 인증인 경우 예외를 반환한다.")
    void 유효하지_않은_인증_예외() {
        // given
        given(authManager.authenticate(any()))
            .willThrow(new BadCredentialsException("bad creds"));

        // when & then
        AuthException ex = assertThrows(AuthException.class,
            () -> loginService.authenticate("user1234@e.com", "wrongpw1234")
        );
        assertThat(ex.getErrorCode()).isEqualTo(INVALID_CREDENTIALS);
    }
}
