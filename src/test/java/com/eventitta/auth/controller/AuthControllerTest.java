package com.eventitta.auth.controller;

import com.eventitta.ControllerTestSupport;
import com.eventitta.auth.dto.request.SignInRequest;
import com.eventitta.auth.dto.request.SignUpRequest;
import com.eventitta.auth.exception.AuthErrorCode;
import com.eventitta.user.domain.Provider;
import com.eventitta.user.domain.Role;
import com.eventitta.user.domain.User;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static com.eventitta.auth.exception.AuthErrorCode.*;
import static com.eventitta.auth.jwt.constants.JwtConstants.ACCESS_TOKEN;
import static com.eventitta.auth.jwt.constants.JwtConstants.REFRESH_TOKEN;
import static com.eventitta.common.constants.ValidationMessage.*;
import static com.eventitta.common.exception.CommonErrorCode.INVALID_INPUT;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("회원 인증/인가 슬라이스 테스트")
class AuthControllerTest extends ControllerTestSupport {

    @Test
    @DisplayName("회원이 유효한 이메일, 비밀번호, 닉네임으로 회원가입하면 사용자 정보가 응답된다.")
    void givenValidSignupData_whenSignup_thenReturnsUserInfo() throws Exception {
        // given
        SignUpRequest request = new SignUpRequest("user@example.com", "P@ssw0rd!", "johndoe");

        User mockUser = User.builder()
            .id(1L)
            .email("user@example.com")
            .password("encoded")
            .nickname("johndoe")
            .role(Role.USER)
            .provider(Provider.LOCAL)
            .build();

        given(authService.signUp(any())).willReturn(mockUser);

        // when & then
        mockMvc.perform(post("/api/v1/auth/signup")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("user@example.com"))
            .andExpect(jsonPath("$.nickname").value("johndoe"));
    }

    @Test
    @DisplayName("회원이 잘못된 형식의 이메일로 회원가입하면 이메일 형식 오류 메시지가 응답된다.")
    void givenInvalidEmail_whenSignup_thenReturnsBadRequest() throws Exception {
        // given
        SignUpRequest request = new SignUpRequest("invalid-email", "P@ssw0rd!", "johndoe");

        // when & then
        mockMvc.perform(post("/api/v1/auth/signup")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("email: " + EMAIL))
            .andExpect(jsonPath("$.error").value(INVALID_INPUT.toString()));
    }

    @Test
    @DisplayName("회원이 8자 미만 비밀번호로 회원가입하면 비밀번호 정책 위반 메시지가 응답된다.")
    void givenShortPassword_whenSignup_thenReturnsBadRequest() throws Exception {
        // given
        SignUpRequest request = new SignUpRequest("user@example.com", "1234", "johndoe");

        // when
        mockMvc.perform(post("/api/v1/auth/signup")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("password: " + PASSWORD))
            .andExpect(jsonPath("$.error").value(INVALID_INPUT.toString()));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "Password1",    // 특수문자 없음
        "Password!",    // 숫자 없음
        "1234!@#$"      // 영문자 없음
    })
    @DisplayName("비밀번호가 영문자, 숫자, 특수문자를 모두 포함하지 않으면 비밀번호 정책 위반 메시지가 응답된다.")
    void givenPasswordMissingCriteria_whenSignup_thenReturnsBadRequest(String invalidPassword) throws Exception {
        // given
        SignUpRequest request = new SignUpRequest("user@example.com", invalidPassword, "johndoe");

        // when & then
        mockMvc.perform(post("/api/v1/auth/signup")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("password: " + PASSWORD))
            .andExpect(jsonPath("$.error").value(INVALID_INPUT.toString()));
    }

    @Test
    @DisplayName("허용되지 않은 형식의 닉네임으로 회원가입하면 닉네임 정책 위반 메시지가 응답된다.")
    void givenInvalidNickname_whenSignup_thenReturnsBadRequest() throws Exception {
        // given
        SignUpRequest request = new SignUpRequest("user@example.com", "P@ssw0rd!", "!@#");

        // when & then
        mockMvc.perform(post("/api/v1/auth/signup")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("nickname: " + NICKNAME))
            .andExpect(jsonPath("$.error").value(INVALID_INPUT.toString()));
    }

    @Test
    @DisplayName("회원이 중복된 이메일으로 회원가입하면 중복된 이메일이라는 메시지가 응답된다.")
    void givenDuplicateEmail_whenSignup_thenReturnsConflict() throws Exception {
        // given
        SignUpRequest request = new SignUpRequest("user3@example.com", "P@ssw0rd!", "tester");
        given(authService.signUp(any())).willThrow(CONFLICTED_EMAIL.defaultException());

        // when & then
        mockMvc.perform(post("/api/v1/auth/signup")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").value(CONFLICTED_EMAIL.toString()))
            .andExpect(jsonPath("$.message").value(CONFLICTED_EMAIL.defaultMessage()));
    }

    @Test
    @DisplayName("회원이 중복된 닉네임으로 회원가입하면 중복된 닉네임이라는 메시지가 응답된다.")
    void givenDuplicateNickname_whenSignup_thenReturnsConflict() throws Exception {
        // given
        SignUpRequest request = new SignUpRequest("user4@example.com", "P@ssw0rd!", "tester");
        given(authService.signUp(any())).willThrow(CONFLICTED_NICKNAME.defaultException());

        // when & then
        mockMvc.perform(post("/api/v1/auth/signup")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").value(CONFLICTED_NICKNAME.toString()))
            .andExpect(jsonPath("$.message").value(CONFLICTED_NICKNAME.defaultMessage()));
    }

    @Test
    @DisplayName("회원이 유효한 이메일과 비밀번호로 로그인 요청을 하면 로그인을 성공한다.")
    void givenValidCredentials_whenLogin_thenInvokesLogin() throws Exception {
        // given
        SignInRequest requestDto = new SignInRequest("user@example.com", "P@ssw0rd!");

        // when
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
            .andExpect(status().isOk());

        // then
        verify(authService).login(
            eq(requestDto),
            any(HttpServletResponse.class)
        );
    }

    @Test
    @DisplayName("회원이 잘못된 형식의 이메일로 로그인하면 이메일 형식 오류 메시지가 응답된다.")
    void givenInvalidEmail_whenLogin_thenReturnsBadRequest() throws Exception {
        // given
        SignInRequest requestDto = new SignInRequest("userexample.com", "P@ssw0rd!");

        // when & then
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("email: " + EMAIL))
            .andExpect(jsonPath("$.error").value(INVALID_INPUT.toString()));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "Password1",    // 특수문자 없음
        "Password!",    // 숫자 없음
        "1234!@#$"      // 영문자 없음
    })
    @DisplayName("로그인 요청에서 비밀번호 형식이 유효하지 않을 경우 에러 메시지를 응답한다.")
    void givenInvalidPassword_whenLogin_thenReturnsBadRequest(String invalidPassword) throws Exception {
        // given
        SignInRequest requestDto = new SignInRequest("user@example.com", invalidPassword);

        // when & then
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("password: " + PASSWORD))
            .andExpect(jsonPath("$.error").value(INVALID_INPUT.toString()));
    }

    @Test
    @DisplayName("로그인 존재하지 않는 이메일을 입력하면 에러 메시지를 응답한다.")
    void givenNonexistentEmail_whenLogin_thenReturnsNotFound() throws Exception {
        // given
        SignInRequest request = new SignInRequest("user4@example.com", "P@ssw0rd!");

        doThrow(NOT_FOUND_USER_EMAIL.defaultException())
            .when(authService)
            .login(any(SignInRequest.class), any(HttpServletResponse.class));

        // when & then
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value(NOT_FOUND_USER_EMAIL.toString()))
            .andExpect(jsonPath("$.message").value(NOT_FOUND_USER_EMAIL.defaultMessage()));
    }

    @Test
    @DisplayName("유효한 엑세스/리프레시 토큰이 있으면 토큰 재발급을 수행하고 엑세스/리프레시 토큰을 재발급한다.")
    void givenValidCookies_whenRefresh_thenReturnsOk() throws Exception {
        // when & then
        mockMvc.perform(post("/api/v1/auth/refresh")
                .cookie(new Cookie(ACCESS_TOKEN, "validAt"),
                    new Cookie(REFRESH_TOKEN, "validRt")))
            .andExpect(status().isOk());

        // then
        verify(authService).refresh(
            eq("validAt"),
            eq("validRt"),
            any(HttpServletResponse.class)
        );
    }

    @Test
    @DisplayName("리프레시 토큰 쿠키가 없으면 예외 메시지를 반환한다")
    void givenMissingRefreshToken_whenRefresh_thenReturnsBadRequest() throws Exception {
        doThrow(AuthErrorCode.REFRESH_TOKEN_MISSING.defaultException())
            .when(authService).refresh(any(), any(), any());

        mockMvc.perform(post("/api/v1/auth/refresh")
                .cookie(new Cookie(ACCESS_TOKEN, "validAt")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value(REFRESH_TOKEN_MISSING.toString()))
            .andExpect(jsonPath("$.message").value(AuthErrorCode.REFRESH_TOKEN_MISSING.defaultMessage()));
    }

    @Test
    @DisplayName("유효하지 않은 리프레시 토큰이면 인가 예외 메시지를 반환한다")
    void givenInvalidRefreshToken_whenRefresh_thenReturnsUnauthorized() throws Exception {
        // given
        doThrow(AuthErrorCode.REFRESH_TOKEN_INVALID.defaultException())
            .when(authService).refresh(any(), any(), any());

        // when
        var mvcResult = mockMvc.perform(post("/api/v1/auth/refresh")
            .cookie(new Cookie(ACCESS_TOKEN, "validAt"),
                new Cookie(REFRESH_TOKEN, "invalidRt")));

        // then
        mvcResult
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value(REFRESH_TOKEN_INVALID.toString()))
            .andExpect(jsonPath("$.message").value(AuthErrorCode.REFRESH_TOKEN_INVALID.defaultMessage()));
        then(authService).should().refresh(any(), any(), any());
    }

    @Test
    @DisplayName("유효한 엑세스 토큰 쿠키가 있으면 로그아웃을 성공한다.")
    void givenAccessTokenCookie_whenLogout_thenReturnsNoContent() throws Exception {
        // arrange: authService.logout 은 void 이므로 아무 예외 없이 수행되도록 stub
        doNothing().when(authService).logout(eq("validAccessToken"), any());

        // act & assert
        mockMvc.perform(post("/api/v1/auth/logout")
                .cookie(new Cookie(ACCESS_TOKEN, "validAccessToken")))
            .andExpect(status().isNoContent());

        // verify: 서비스가 정확히 호출됐는지 확인
        verify(authService).logout(eq("validAccessToken"), any(HttpServletResponse.class));
    }

    @Test
    @DisplayName("엑세스 토큰 쿠키가 없어도 로그아웃에 성공한다.")
    void givenNoAccessTokenCookie_whenLogout_thenReturnsNoContent() throws Exception {
        // given
        doNothing().when(authService).logout(isNull(), any());

        // when & then
        mockMvc.perform(post("/api/v1/auth/logout"))
            .andExpect(status().isNoContent());

        verify(authService).logout(isNull(), any(HttpServletResponse.class));
    }

    @Test
    @DisplayName("잘못된 엑세스 토큰으로 로그아웃 요청에도 로그아웃은 성공한다.")
    void givenInvalidToken_whenLogout_thenInvokesLogoutAndReturnsNoContent() throws Exception {
        doNothing().when(authService).logout(eq("badToken"), any(HttpServletResponse.class));

        mockMvc.perform(post("/api/v1/auth/logout")
                .cookie(new Cookie(ACCESS_TOKEN, "badToken")))
            .andExpect(status().isNoContent());

        verify(authService).logout(eq("badToken"), any(HttpServletResponse.class));
    }
}
