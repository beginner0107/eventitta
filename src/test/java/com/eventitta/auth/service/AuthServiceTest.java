package com.eventitta.auth.service;

import com.eventitta.auth.exception.AuthException;
import com.eventitta.auth.service.dto.*;
import com.eventitta.user.domain.User;
import com.eventitta.user.exception.UserException;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.stream.Stream;

import static com.eventitta.auth.constants.AuthConstants.ACCESS_TOKEN;
import static com.eventitta.auth.constants.AuthConstants.REFRESH_TOKEN;
import static com.eventitta.auth.exception.AuthErrorCode.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.*;
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
    private HttpServletResponse response;
    @Mock
    private CookieManager cookieManager;

    @Nested
    @DisplayName("회원가입")
    class SignUp {

        @Test
        @DisplayName("회원가입 서비스가 사용자를 반환하면 사용자 정보를 SignUpResult로 변환해 반환한다")
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

            UserException exception = com.eventitta.user.exception.UserErrorCode.CONFLICTED_EMAIL.defaultException();
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

            UserException exception = com.eventitta.user.exception.UserErrorCode.CONFLICTED_NICKNAME.defaultException();
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
        @DisplayName("인증에 성공해 사용자 id를 반환하면 토큰을 발급하고 쿠키에 저장한다")
        void loginSuccess() {
            // given
            SignInCommand command = new SignInCommand("test@test.com", "password123!");
            Long userId = 1L;
            TokenResult tokenResult = new TokenResult("access-token", "refresh-token");

            given(loginService.authenticate(command.email(), command.password()))
                .willReturn(userId);
            given(tokenService.issueTokens(userId))
                .willReturn(tokenResult);

            // when
            authService.login(command, response);

            // then
            then(loginService).should().authenticate(command.email(), command.password());
            then(tokenService).should().issueTokens(userId);
            then(cookieManager).should().addTokenCookies(response, tokenResult);
        }

        @Test
        @DisplayName("인증에 실패하면 INVALID_CREDENTIALS 예외가 그대로 전파된다")
        void loginFailWhenAuthenticationFails() {
            // given
            SignInCommand command = new SignInCommand("test@test.com", "wrong-password");
            AuthException exception = INVALID_CREDENTIALS.defaultException();

            given(loginService.authenticate(command.email(), command.password()))
                .willThrow(exception);

            // when // then
            assertThatThrownBy(() -> authService.login(command, response))
                .isSameAs(exception);

            then(tokenService).shouldHaveNoInteractions();
            then(cookieManager).shouldHaveNoInteractions();
        }
    }

    @Nested
    @DisplayName("토큰 갱신")
    class TokenRefresh {

        @Test
        @DisplayName("토큰 갱신 서비스가 새 토큰을 반환하면 새 토큰을 쿠키에 저장한다")
        void refreshSuccess() {
            // given
            RefreshCommand command = new RefreshCommand("expired-access-token", "valid-refresh-token");
            TokenResult tokenResult = new TokenResult("new-access-token", "new-refresh-token");

            given(refreshService.refresh(command)).willReturn(tokenResult);

            // when
            authService.refresh(command, response);

            // then
            then(refreshService).should().refresh(command);
            then(cookieManager).should().addTokenCookies(response, tokenResult);
        }

        @Test
        @DisplayName("토큰 갱신에 실패하면 예외가 그대로 전파되고 쿠키는 저장되지 않는다")
        void refreshFailWhenRefreshServiceThrowsException() {
            // given
            RefreshCommand command = new RefreshCommand("", "valid-refresh-token");
            AuthException exception = ACCESS_TOKEN_INVALID.defaultException();

            given(refreshService.refresh(command)).willThrow(exception);

            // when // then
            assertThatThrownBy(() -> authService.refresh(command, response))
                .isSameAs(exception);

            then(cookieManager).shouldHaveNoInteractions();
        }
    }

    @Nested
    @DisplayName("로그아웃")
    class Logout {

        @Test
        @DisplayName("accessToken과 refreshToken이 모두 있으면 토큰 무효화를 시도하고 쿠키를 삭제한다")
        void logoutSuccessWithValidTokens() {
            // given
            LogoutCommand command = new LogoutCommand("valid-access-token", "valid-refresh-token");

            // when
            authService.logout(command, response);

            // then
            then(refreshService).should().invalidateByToken("valid-access-token", "valid-refresh-token");
            then(cookieManager).should().deleteCookie(response, ACCESS_TOKEN);
            then(cookieManager).should().deleteCookie(response, REFRESH_TOKEN);
        }

        @Test
        @DisplayName("토큰이 유효하지 않아도 예외를 무시하고 쿠키를 삭제한다")
        void logoutSuccessEvenIfTokensAreInvalid() {
            // given
            LogoutCommand command = new LogoutCommand("invalid-access-token", "invalid-refresh-token");

            willThrow(ACCESS_TOKEN_INVALID.defaultException())
                .given(refreshService)
                .invalidateByToken("invalid-access-token", "invalid-refresh-token");

            // when
            authService.logout(command, response);

            // then
            then(refreshService).should().invalidateByToken("invalid-access-token", "invalid-refresh-token");
            then(cookieManager).should().deleteCookie(response, ACCESS_TOKEN);
            then(cookieManager).should().deleteCookie(response, REFRESH_TOKEN);
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("missingOrBlankTokenCases")
        @DisplayName("accessToken 또는 refreshToken이 없으면 무효화 없이 쿠키만 삭제한다")
        void logoutSuccessWithoutInvalidationWhenAnyTokenIsMissingOrBlank(
            String description,
            String accessToken,
            String refreshToken
        ) {
            // given
            LogoutCommand command = new LogoutCommand(accessToken, refreshToken);

            // when
            authService.logout(command, response);

            // then
            then(refreshService).shouldHaveNoInteractions();
            then(cookieManager).should().deleteCookie(response, ACCESS_TOKEN);
            then(cookieManager).should().deleteCookie(response, REFRESH_TOKEN);
        }

        private static Stream<Arguments> missingOrBlankTokenCases() {
            return Stream.of(
                Arguments.of("accessToken이 null이면 무효화 없이 쿠키만 삭제한다", null, "valid-refresh-token"),
                Arguments.of("refreshToken이 null이면 무효화 없이 쿠키만 삭제한다", "valid-access-token", null),
                Arguments.of("accessToken이 blank면 무효화 없이 쿠키만 삭제한다", "   ", "valid-refresh-token"),
                Arguments.of("refreshToken이 blank면 무효화 없이 쿠키만 삭제한다", "valid-access-token", "   ")
            );
        }
    }
}
