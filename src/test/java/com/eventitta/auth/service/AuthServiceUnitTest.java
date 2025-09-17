package com.eventitta.auth.service;

import com.eventitta.auth.dto.request.SignInRequest;
import com.eventitta.auth.dto.request.SignUpRequest;
import com.eventitta.auth.dto.response.TokenResponse;
import com.eventitta.auth.exception.AuthErrorCode;
import com.eventitta.auth.exception.AuthException;
import com.eventitta.auth.jwt.JwtTokenProvider;
import com.eventitta.common.util.CookieUtil;
import com.eventitta.user.domain.User;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;

import static com.eventitta.auth.exception.AuthErrorCode.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("인증 단위 테스트")
public class AuthServiceUnitTest {
    @InjectMocks
    private AuthService authService;

    @Mock
    private SignUpService signUpService;
    @Mock
    private LoginService loginService;
    @Mock
    private TokenService tokenService;
    @Mock
    private RefreshTokenService refreshService;
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private HttpServletResponse response;

    @Test
    @DisplayName("회원가입 요청을 수행한다.")
    void givenValidSignUpRequest_whenSignUp_thenReturnsUser() {
        // given
        SignUpRequest req = new SignUpRequest("a@b.com", "P@ssw0rd!", "nick");
        User user = User.builder()
            .id(1L)
            .email("a@b.com")
            .password("encoded")
            .nickname("nick")
            .build();
        given(signUpService.register(req)).willReturn(user);

        // when
        User result = authService.signUp(req);

        // then
        assertThat(result).isSameAs(user);
        then(signUpService).should().register(req);
    }

    @Test
    @DisplayName("중복된 이메일이 입력되었을 경우 에러가 발생한다.")
    void givenConflictEmail_whenSignUp_thenThrowsException() {
        // given
        SignUpRequest request = new SignUpRequest("absdd1@dkdk.com", "P@ssw0rd!", "nick12");

        given(signUpService.register(request)).willThrow(CONFLICTED_EMAIL.defaultException());

        // when & then
        assertThatThrownBy(() -> authService.signUp(request))
            .isInstanceOf(AuthException.class)
            .hasMessage(CONFLICTED_EMAIL.defaultMessage());
    }

    @Test
    @DisplayName("중복된 닉네임이 입력되었을 경우 에러가 발생한다.")
    void givenConflictNickname_whenSignUp_thenThrowsException() {
        // given
        SignUpRequest request = new SignUpRequest("absdd1@dkdk.com", "P@ssw0rd!", "nick12");

        given(signUpService.register(request)).willThrow(CONFLICTED_NICKNAME.defaultException());

        // when & then
        assertThatThrownBy(() -> authService.signUp(request))
            .isInstanceOf(AuthException.class)
            .hasMessage(CONFLICTED_NICKNAME.defaultMessage());
    }

    @Test
    @DisplayName("올바른 자격으로 로그인하면 인증 서비스, 토큰 서비스, 쿠키 유틸이 차례로 호출된다")
    void givenValidCredentials_whenLogin_thenAuthenticateAndIssueTokensAndSetCookies() {
        // given
        SignInRequest request = new SignInRequest("abcde@b.com", "correctPw!");
        given(loginService.authenticate("abcde@b.com", "correctPw!")).willReturn(42L);
        TokenResponse tokens = new TokenResponse("access-token", "refresh-token");
        given(tokenService.issueTokens(42L)).willReturn(tokens);

        try (MockedStatic<CookieUtil> cookieUtil = mockStatic(CookieUtil.class)) {
            // when
            authService.login(request, response);

            // then
            then(loginService).should().authenticate("abcde@b.com", "correctPw!");
            then(tokenService).should().issueTokens(42L);
            cookieUtil.verify(() ->
                CookieUtil.addTokenCookies(response, tokens, jwtTokenProvider)
            );
        }
    }

    @Test
    @DisplayName("인증 자격이 유효하지 않으면 예외 메시지를 반환한다.")
    void givenInvalidCredentials_whenLogin_thenThrowsException() {
        // given
        SignInRequest req = new SignInRequest("abcde@b.com", "asdf1234!@");
        given(loginService.authenticate(any(), any()))
            .willThrow(INVALID_CREDENTIALS.defaultException());

        // when & then
        assertThatThrownBy(() -> authService.login(req, response))
            .isInstanceOf(AuthException.class)
            .hasMessage(INVALID_CREDENTIALS.defaultMessage());
    }

    @Test
    @DisplayName("유효한 리프레시 토큰이면 엑세스 토큰과 리프레시 토큰을 재발급한다.")
    void givenValidRefresh_whenRefresh_thenIssueNewTokensAndSetCookies() {
        // given
        String at = "expiredAt", rt = "rawRefresh";
        TokenResponse newTokens = new TokenResponse("newAt", "newRt");
        given(refreshService.refresh(at, rt)).willReturn(newTokens);

        // when & then
        try (MockedStatic<CookieUtil> cookieUtil = mockStatic(CookieUtil.class)) {
            authService.refresh(at, rt, response);

            then(refreshService).should().refresh(at, rt);
            cookieUtil.verify(() ->
                CookieUtil.addTokenCookies(response, newTokens, jwtTokenProvider)
            );
        }
    }

    @Test
    @DisplayName("리프레시 토큰이 없으면 예외를 반환한다.")
    void givenMissingRefreshToken_whenRefresh_thenThrowsException() {
        // given
        given(refreshService.refresh(any(), any()))
            .willThrow(AuthErrorCode.REFRESH_TOKEN_MISSING.defaultException());

        // when & then
        assertThatThrownBy(() -> authService.refresh("anyAt", null, response))
            .isInstanceOf(AuthException.class)
            .hasMessage(REFRESH_TOKEN_MISSING.defaultMessage());
    }

    @Test
    @DisplayName("토큰 검증에 실패하면 예외를 반환한다.")
    void givenInvalidRefreshToken_whenRefresh_thenThrowsException() {
        // given
        given(refreshService.refresh(any(), any()))
            .willThrow(AuthErrorCode.REFRESH_TOKEN_INVALID.defaultException());

        // when & then
        assertThatThrownBy(() -> authService.refresh("anyAt", "badRt", response))
            .isInstanceOf(AuthException.class)
            .hasMessage(REFRESH_TOKEN_INVALID.defaultMessage());
        then(refreshService).should().refresh(any(), any());
    }

    @Test
    @DisplayName("엑세스 토큰으로 사용자를 검증하고, 리프레시 토큰과 쿠키를 삭제한다.")
    void givenValidToken_whenLogout_thenInvalidateAndClearCookies() {
        // given
        String token = "validToken";
        doNothing().when(refreshService).invalidateByAccessToken(token);

        MockHttpServletResponse res = new MockHttpServletResponse();

        // when
        authService.logout(token, res);

        // then
        verify(refreshService).invalidateByAccessToken(token);

        // then
        List<String> setCookie = res.getHeaders(HttpHeaders.SET_COOKIE);
        assertThat(setCookie).hasSize(2);
        assertThat(setCookie.get(0)).startsWith("access_token=;");
        assertThat(setCookie.get(1)).startsWith("refresh_token=;");
    }

    @Test
    @DisplayName("잘못된 토큰으로 예외가 발생해도 쿠키는 항상 삭제된다")
    void givenInvalidToken_whenLogout_thenStillClearCookies() {
        // given
        String badToken = "badToken";
        willThrow(ACCESS_TOKEN_INVALID.defaultException())
            .given(refreshService).invalidateByAccessToken(badToken);

        MockHttpServletResponse res = new MockHttpServletResponse();

        // when
        authService.logout(badToken, res);

        // then
        verify(refreshService).invalidateByAccessToken(badToken);

        // then
        List<String> setCookie = res.getHeaders(HttpHeaders.SET_COOKIE);
        assertThat(setCookie).hasSize(2);
        assertThat(setCookie.get(0)).startsWith("access_token=;");
        assertThat(setCookie.get(1)).startsWith("refresh_token=;");
    }

    @Test
    @DisplayName("토큰이 없으면 검증은 건너뛰고 쿠키만 삭제한다")
    void givenNoToken_whenLogout_thenOnlyClearCookies() {
        // given
        MockHttpServletResponse res = new MockHttpServletResponse();

        // when
        authService.logout(null, res);

        // then
        then(refreshService).shouldHaveNoInteractions();

        List<String> setCookies = res.getHeaders(HttpHeaders.SET_COOKIE);
        assertThat(setCookies).hasSize(2);
        assertThat(setCookies.get(0)).startsWith("access_token=;");
        assertThat(setCookies.get(1)).startsWith("refresh_token=;");
    }
}
