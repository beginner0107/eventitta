package com.eventitta.auth.controller;

import com.eventitta.IntegrationTestSupport;
import com.eventitta.auth.dto.request.SignInRequest;
import com.eventitta.auth.dto.request.SignUpRequest;
import com.eventitta.auth.repository.RefreshTokenRepository;
import com.eventitta.user.domain.Provider;
import com.eventitta.user.domain.Role;
import com.eventitta.user.domain.User;
import com.eventitta.user.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MvcResult;

import static com.eventitta.auth.exception.AuthErrorCode.*;
import static com.eventitta.common.constants.ValidationMessage.*;
import static com.eventitta.common.exception.CommonErrorCode.INVALID_INPUT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Transactional
@DisplayName("회원 통합 테스트")
public class AuthIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @AfterEach
    void tearDown() {
        refreshTokenRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("회원이 이메일, 비밀번호, 닉네임으로 회원가입하면 사용자 정보가 응답된다.")
    void givenValidSignupData_whenSignup_thenReturnsUserInfo() throws Exception {
        // given
        var request = new SignUpRequest("test@example.com", "P@ssw0rd!", "tester1");

        // when & then
        mockMvc.perform(post("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("test@example.com"))
            .andExpect(jsonPath("$.nickname").value("tester1"));
    }

    @Test
    @DisplayName("회원이 유효하지 않은 이메일로 회원가입하면 이메일 형식 오류 메시지가 반환된다.")
    void givenMalformedEmail_whenSignup_thenReturnsBadRequest() throws Exception {
        // given
        var request = new SignUpRequest("invalid-email", "P@ssw0rd!", "tester");

        // when & then
        mockMvc.perform(post("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("email: " + EMAIL))
            .andExpect(jsonPath("$.error").value(INVALID_INPUT.toString()));
    }

    @Test
    @DisplayName("회원이 형식에 맞지 않는 비밀번호로 회원가입하면 비밀번호 정책 위반 메시지가 반환된다.")
    void givenShortPassword_whenSignup_thenReturnsBadRequest() throws Exception {
        // given
        var request = new SignUpRequest("user@example.com", "1234", "tester");

        // when & then
        mockMvc.perform(post("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("password: " + PASSWORD))
            .andExpect(jsonPath("$.error").value(INVALID_INPUT.toString()));
    }

    @Test
    @DisplayName("회원이 허용되지 않은 형식의 닉네임으로 회원가입하면 닉네임 정책 위반 메시지가 반환된다.")
    void givenInvalidNickname_whenSignup_thenReturnsBadRequest() throws Exception {
        // given
        var request = new SignUpRequest("user@example.com", "P@ssw0rd!", "!@#");

        // when & then
        mockMvc.perform(post("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("nickname: " + NICKNAME))
            .andExpect(jsonPath("$.error").value(INVALID_INPUT.toString()));
    }

    @Test
    @DisplayName("유효한 자격으로 로그인하면 액세스/리프레시 토큰 쿠키를 응답한다")
    void givenValidCredentials_whenLogin_thenSetsCookies() throws Exception {
        // given
        userRepository.save(createUser());
        SignInRequest dto = new SignInRequest("test@login.com", "P@ssw0rd!");

        // when
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
            .andExpect(status().isOk())
            .andExpect(cookie().exists("access_token"))
            .andExpect(cookie().exists("refresh_token"));
    }

    @Test
    @DisplayName("로그인에서 존재하지 않는 이메일로 로그인하면 오류 메시지가 반환된다.")
    void givenNonexistentEmail_whenLogin_thenReturnsUnauthorized() throws Exception {
        // given
        SignInRequest dto = new SignInRequest("noone@nowhere.com", "P@ssw0rd!");

        // when & then
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value(INVALID_CREDENTIALS.defaultMessage()))
            .andExpect(jsonPath("$.error").value(INVALID_CREDENTIALS.toString()));
    }

    @Test
    @DisplayName("로그인에서 이메일 형식이 올바르지 않으면 오류 메시지가 반환된다.")
    void givenMalformedEmail_whenLogin_thenReturnsBadRequest() throws Exception {
        // given
        User user = createUser();
        userRepository.save(user);
        SignInRequest dto = new SignInRequest("noonenowherecom", "P@ssw0rd!");

        // when & then
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("email: " + EMAIL))
            .andExpect(jsonPath("$.error").value(INVALID_INPUT.toString()));
    }

    @Test
    @DisplayName("로그인에서 비밀번호 형식이 올바르지 않으면 오류 메시지가 반환된다.")
    void givenInvalidPassword_whenLogin_thenReturnsBadRequest() throws Exception {
        // given
        SignInRequest dto = new SignInRequest("test@login.com", "P!");

        // when & then
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("password: " + PASSWORD))
            .andExpect(jsonPath("$.error").value(INVALID_INPUT.toString()));
    }

    @Test
    @DisplayName("유효한 자격으로 로그인 후 리프레시 토큰 요청 시 새로운 리프레시 토큰을 발급한다")
    void givenValidCredentials_whenLoginAndRefresh_thenIssuesNewRefreshToken() throws Exception {
        // given
        userRepository.save(User.builder()
            .email("refresh@it.com")
            .password(passwordEncoder.encode("P@ssw0rd!"))
            .nickname("refresher")
            .role(Role.USER)
            .provider(Provider.LOCAL)
            .build());

        var loginResult = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new SignInRequest("refresh@it.com", "P@ssw0rd!"))))
            .andExpect(status().isOk())
            .andExpect(cookie().exists("access_token"))
            .andExpect(cookie().exists("refresh_token"))
            .andReturn();

        Cookie oldAt = loginResult.getResponse().getCookie("access_token");
        Cookie oldRt = loginResult.getResponse().getCookie("refresh_token");

        // when & then
        mockMvc.perform(post("/api/v1/auth/refresh")
                .cookie(oldAt, oldRt))
            .andExpect(status().isOk())
            .andExpect(cookie().exists("access_token"))
            .andExpect(cookie().exists("refresh_token"))
            .andExpect(cookie().value("refresh_token", not(oldRt.getValue())));
    }

    @Test
    @DisplayName("리프레시 토큰 쿠키가 없으면 오류 메시지를 반환한다.")
    void givenMissingRefreshCookie_whenRefresh_thenReturnsBadRequest() throws Exception {
        // when & then
        mockMvc.perform(post("/api/v1/auth/refresh")
                .cookie(new Cookie("access_token", "dummyAt")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value(REFRESH_TOKEN_MISSING.toString()))
            .andExpect(jsonPath("$.message").value(REFRESH_TOKEN_MISSING.defaultMessage()));
    }

    @Test
    @DisplayName("유효하지 않은 리프레시 토큰으로 요청하면 오류 메시지를 반환한다.")
    void givenInvalidRefreshCookie_whenRefresh_thenReturnsUnauthorized() throws Exception {
        // given
        userRepository.save(User.builder()
            .email("badrt@it.com")
            .password(passwordEncoder.encode("P@ssw0rd!"))
            .nickname("badrt")
            .role(Role.USER)
            .provider(Provider.LOCAL)
            .build());

        var loginResult = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new SignInRequest("badrt@it.com", "P@ssw0rd!"))))
            .andExpect(status().isOk())
            .andReturn();

        Cookie at = loginResult.getResponse().getCookie("access_token");

        // when & then
        mockMvc.perform(post("/api/v1/auth/refresh")
                .cookie(at, new Cookie("refresh_token", "invalidRt")))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value(REFRESH_TOKEN_INVALID.toString()))
            .andExpect(jsonPath("$.message").value(REFRESH_TOKEN_INVALID.defaultMessage()));
    }

    @Test
    @DisplayName("로그인 후 로그아웃하면 저장된 리프레시 토큰이 삭제되고 쿠키가 만료된다")
    void givenLoggedIn_whenLogout_thenDeletesRefreshTokenAndClearsCookies() throws Exception {
        userRepository.save(User.builder()
            .email("logout@it.com")
            .password(passwordEncoder.encode("P@ssw0rd!"))
            .nickname("logoutUser")
            .role(Role.USER)
            .provider(Provider.LOCAL)
            .build());

        var loginResult = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new SignInRequest("logout@it.com", "P@ssw0rd!"))))
            .andExpect(status().isOk())
            .andExpect(cookie().exists("access_token"))
            .andExpect(cookie().exists("refresh_token"))
            .andReturn();

        String at = loginResult.getResponse().getCookie("access_token").getValue();

        assertThat(refreshTokenRepository.findAll()).hasSize(1);

        mockMvc.perform(post("/api/v1/auth/logout")
                .cookie(new Cookie("access_token", at)))
            .andExpect(status().isNoContent())
            .andExpect(cookie().maxAge("access_token", 0))
            .andExpect(cookie().maxAge("refresh_token", 0));

        assertThat(refreshTokenRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("로그인하지 않은 상태에서 로그아웃 요청해도 쿠키가 만료된다")
    void givenNoLogin_whenLogout_thenReturnsNoContentAndClearsCookies() throws Exception {
        // when
        MvcResult result = mockMvc.perform(post("/api/v1/auth/logout"))
            .andExpect(status().isNoContent())
            .andReturn();

        MockHttpServletResponse resp = result.getResponse();

        Cookie expiredAccess = resp.getCookie("access_token");
        Cookie expiredRefresh = resp.getCookie("refresh_token");

        // then
        assertThat(expiredAccess).isNotNull();
        assertThat(expiredAccess.getMaxAge()).isZero();

        assertThat(expiredRefresh).isNotNull();
        assertThat(expiredRefresh.getMaxAge()).isZero();

        assertThat(refreshTokenRepository.count()).isZero();
    }

    private User createUser() {
        return User.builder()
            .email("test@login.com")
            .password(passwordEncoder.encode("P@ssw0rd!"))
            .nickname("tester")
            .role(Role.USER)
            .provider(Provider.LOCAL)
            .build();
    }
}
