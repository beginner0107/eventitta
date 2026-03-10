package com.eventitta.auth.service;

import com.eventitta.auth.exception.AuthException;
import com.eventitta.auth.jwt.JwtTokenProvider;
import com.eventitta.auth.service.dto.*;
import com.eventitta.common.util.CookieUtil;
import com.eventitta.user.domain.User;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.eventitta.auth.constants.AuthConstants.ACCESS_TOKEN;
import static com.eventitta.auth.constants.AuthConstants.REFRESH_TOKEN;
import static com.eventitta.auth.exception.AuthErrorCode.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.only;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @InjectMocks
    private AuthService authService;

    @Mock
    private LoginService loginService;
    @Mock
    private SignUpService signUpService;
    @Mock
    private TokenService tokenService;
    @Mock
    private RefreshTokenService refreshService;
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private HttpServletResponse response;

    @Nested
    @DisplayName("회원가입")
    class SignUp {

        @Test
        @DisplayName("회원가입에 성공하면 사용자 정보를 SignUpResult로 변환해 반환한다")
        void signUpSuccess() {
            // given
            SignUpCommand command = new SignUpCommand(
                "test@test.com",
                "password123!",
                "tanuki"
            );

            User user = User.builder()
                .id(1L)
                .email("test@test.com")
                .nickname("tanuki")
                .build();

            given(signUpService.register(command)).willReturn(user);

            // when
            SignUpResult result = authService.signUp(command);

            // then
            assertThat(result).isNotNull();
            assertThat(result.email()).isEqualTo("test@test.com");
            assertThat(result.nickname()).isEqualTo("tanuki");

            then(signUpService).should(only()).register(command);
        }

        @Test
        @DisplayName("이미 사용 중인 이메일이면 예외를 그대로 전파한다")
        void signUpFailByDuplicatedEmail() {
            // given
            SignUpCommand command = new SignUpCommand(
                "duplicate@test.com",
                "password123!",
                "tanuki"
            );

            AuthException exception = CONFLICTED_EMAIL.defaultException();
            given(signUpService.register(command)).willThrow(exception);

            // when // then
            assertThatThrownBy(() -> authService.signUp(command))
                .isSameAs(exception);

            then(signUpService).should(only()).register(command);
        }

        @Test
        @DisplayName("이미 사용 중인 닉네임이면 예외를 그대로 전파한다")
        void signUpFailByDuplicatedNickname() {
            // given
            SignUpCommand command = new SignUpCommand(
                "test@test.com",
                "password123!",
                "duplicatedNickname"
            );

            AuthException exception = CONFLICTED_NICKNAME.defaultException();
            given(signUpService.register(command)).willThrow(exception);

            // when // then
            assertThatThrownBy(() -> authService.signUp(command))
                .isSameAs(exception);

            then(signUpService).should(only()).register(command);
        }
    }

    @Nested
    @DisplayName("로그인")
    class Login {

        @Test
        @DisplayName("로그인에 성공하면 토큰을 발급하고 쿠키에 저장한다")
        void loginSuccess() {
            // given
            SignInCommand command = new SignInCommand("test@test.com", "password123!");
            Long userId = 1L;
            TokenResult tokenResult = new TokenResult("access-token", "refresh-token");

            given(loginService.authenticate(command.email(), command.password()))
                .willReturn(userId);
            given(tokenService.issueTokens(userId))
                .willReturn(tokenResult);

            try (MockedStatic<CookieUtil> cookieUtil = mockStatic(CookieUtil.class)) {
                // when
                authService.login(command, response);

                // then
                then(loginService).should().authenticate(command.email(), command.password());
                then(tokenService).should().issueTokens(userId);

                cookieUtil.verify(() ->
                    CookieUtil.addTokenCookies(response, tokenResult, jwtTokenProvider)
                );
            }
        }

        @Test
        @DisplayName("인증에 실패하면 INVALID_CREDENTIALS 예외가 그대로 전파된다")
        void loginFailWhenAuthenticationFails() {
            // given
            SignInCommand command = new SignInCommand("test@test.com", "wrong-password");
            AuthException exception = INVALID_CREDENTIALS.defaultException();

            given(loginService.authenticate(command.email(), command.password()))
                .willThrow(exception);

            try (MockedStatic<CookieUtil> cookieUtil = mockStatic(CookieUtil.class)) {
                // when // then
                assertThatThrownBy(() -> authService.login(command, response))
                    .isSameAs(exception);

                then(loginService).should().authenticate(command.email(), command.password());
                then(tokenService).shouldHaveNoInteractions();
                cookieUtil.verifyNoInteractions();
            }
        }
    }

    @Nested
    @DisplayName("토큰 갱신")
    class TokenRefresh {
        @Test
        @DisplayName("토큰 갱신에 성공하면 새 토큰을 쿠키에 저장한다")
        void refreshSuccess() {
            // given
            RefreshCommand command = new RefreshCommand("expired-access-token", "valid-refresh-token");
            TokenResult tokenResult = new TokenResult("new-access-token", "new-refresh-token");

            given(refreshService.refresh(command)).willReturn(tokenResult);

            try (MockedStatic<CookieUtil> cookieUtil = mockStatic(CookieUtil.class)) {
                // when
                authService.refresh(command, response);

                // then
                then(refreshService).should().refresh(command);
                cookieUtil.verify(() ->
                    CookieUtil.addTokenCookies(response, tokenResult, jwtTokenProvider)
                );
            }
        }

        @Test
        @DisplayName("토큰 갱신에 실패하면 예외가 그대로 전파되고 쿠키는 저장되지 않는다")
        void refreshFailWhenRefreshServiceThrowsException() {
            // given
            RefreshCommand command = new RefreshCommand("", "valid-refresh-token");
            AuthException exception = ACCESS_TOKEN_INVALID.defaultException();

            given(refreshService.refresh(command)).willThrow(exception);

            try (MockedStatic<CookieUtil> cookieUtil = mockStatic(CookieUtil.class)) {
                // when // then
                assertThatThrownBy(() -> authService.refresh(command, response))
                    .isSameAs(exception);

                then(refreshService).should().refresh(command);
                cookieUtil.verifyNoInteractions();
            }
        }
    }

    @Nested
    @DisplayName("로그아웃")
    class Logout {

        @Test
        @DisplayName("유효한 accessToken이 있으면 refresh token 무효화를 시도하고 쿠키를 삭제한다")
        void logoutSuccessWithValidAccessToken() {
            // given
            LogoutCommand command = new LogoutCommand("valid-access-token");

            try (MockedStatic<CookieUtil> cookieUtil = mockStatic(CookieUtil.class)) {
                // when
                authService.logout(command, response);

                // then
                then(refreshService).should().invalidateByAccessToken("valid-access-token");

                cookieUtil.verify(() ->
                    CookieUtil.deleteCookie(response, ACCESS_TOKEN)
                );
                cookieUtil.verify(() ->
                    CookieUtil.deleteCookie(response, REFRESH_TOKEN)
                );
            }
        }

        @Test
        @DisplayName("유효하지 않은 accessToken이어도 예외를 무시하고 쿠키를 삭제한다")
        void logoutSuccessEvenIfAccessTokenIsInvalid() {
            // given
            LogoutCommand command = new LogoutCommand("invalid-access-token");
            AuthException exception = ACCESS_TOKEN_INVALID.defaultException();

            willThrow(exception)
                .given(refreshService)
                .invalidateByAccessToken("invalid-access-token");

            try (MockedStatic<CookieUtil> cookieUtil = mockStatic(CookieUtil.class)) {
                // when
                authService.logout(command, response);

                // then
                then(refreshService).should().invalidateByAccessToken("invalid-access-token");

                cookieUtil.verify(() ->
                    CookieUtil.deleteCookie(response, ACCESS_TOKEN)
                );
                cookieUtil.verify(() ->
                    CookieUtil.deleteCookie(response, REFRESH_TOKEN)
                );
            }
        }

        @Test
        @DisplayName("accessToken이 없으면 무효화 없이 쿠키만 삭제한다")
        void logoutSuccessWithoutAccessToken() {
            // given
            LogoutCommand command = new LogoutCommand(null);

            try (MockedStatic<CookieUtil> cookieUtil = mockStatic(CookieUtil.class)) {
                // when
                authService.logout(command, response);

                // then
                then(refreshService).shouldHaveNoInteractions();

                cookieUtil.verify(() ->
                    CookieUtil.deleteCookie(response, ACCESS_TOKEN)
                );
                cookieUtil.verify(() ->
                    CookieUtil.deleteCookie(response, REFRESH_TOKEN)
                );
            }
        }

        @Test
        @DisplayName("accessToken이 blank면 무효화 없이 쿠키만 삭제한다")
        void logoutSuccessWithoutAccessTokenWhenBlank() {
            // given
            LogoutCommand command = new LogoutCommand("   ");

            try (MockedStatic<CookieUtil> cookieUtil = mockStatic(CookieUtil.class)) {
                // when
                authService.logout(command, response);

                // then
                then(refreshService).shouldHaveNoInteractions();

                cookieUtil.verify(() ->
                    CookieUtil.deleteCookie(response, ACCESS_TOKEN)
                );
                cookieUtil.verify(() ->
                    CookieUtil.deleteCookie(response, REFRESH_TOKEN)
                );
            }
        }
    }
}
