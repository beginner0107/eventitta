package com.eventitta.domain.auth.service;

import com.eventitta.domain.auth.domain.AuthIdentity;
import com.eventitta.domain.auth.domain.AuthActionTokenPurpose;
import com.eventitta.domain.auth.domain.AuthProvider;
import com.eventitta.domain.auth.port.AuthMailSender;
import com.eventitta.domain.auth.port.AuthTokenProvider;
import com.eventitta.domain.auth.port.CredentialAuthenticator;
import com.eventitta.domain.auth.port.KakaoAuthClient;
import com.eventitta.domain.auth.port.dto.KakaoUserInfo;
import com.eventitta.domain.auth.repository.AuthIdentityRepository;
import com.eventitta.domain.auth.repository.RefreshTokenRepository;
import com.eventitta.domain.auth.service.dto.*;
import com.eventitta.domain.user.api.internal.command.RegisterLocalUserCommand;
import com.eventitta.domain.user.api.internal.command.RegisterSocialUserCommand;
import com.eventitta.domain.user.api.internal.facade.UserInternalFacade;
import com.eventitta.domain.user.api.internal.result.RegisteredUserResult;
import com.eventitta.domain.user.api.internal.view.UserAuthView;
import com.eventitta.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static com.eventitta.domain.auth.exception.AuthErrorCode.SOCIAL_ACCOUNT_ALREADY_LINKED;
import static com.eventitta.domain.auth.exception.AuthErrorCode.SOCIAL_EMAIL_NOT_VERIFIED;
import static com.eventitta.domain.auth.exception.AuthErrorCode.SOCIAL_LINK_REQUIRED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserInternalFacade userInternalFacade;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private CredentialAuthenticator credentialAuthenticator;

    @Mock
    private TokenService tokenService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private KakaoAuthClient kakaoAuthClient;

    @Mock
    private AuthIdentityRepository authIdentityRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private AuthActionTokenService authActionTokenService;

    @Mock
    private AuthMailSender authMailSender;

    @Mock
    private AuthAttemptGuard authAttemptGuard;

    @Mock
    private AuthSecurityEventService authSecurityEventService;

    @Mock
    private AuthTokenProvider authTokenProvider;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("로컬 회원가입은 비밀번호를 인코딩하고 user internal facade 에 위임한다")
    void signUp_success() {
        SignUpCommand command = new SignUpCommand("user@test.com", "password1234!@@", "tester");
        given(passwordEncoder.encode(command.password())).willReturn("encoded-password");
        given(userInternalFacade.registerLocalUser(new RegisterLocalUserCommand("user@test.com", "encoded-password", "tester")))
            .willReturn(new RegisteredUserResult(1L, "user@test.com", "tester"));
        given(authActionTokenService.issue(1L, "user@test.com", AuthActionTokenPurpose.EMAIL_VERIFICATION, null))
            .willReturn("verify.token");

        SignUpResult result = authService.signUp(command);

        assertThat(result.email()).isEqualTo("user@test.com");
        assertThat(result.nickname()).isEqualTo("tester");
    }

    @Test
    @DisplayName("로컬 로그인 성공 시 인증된 userId 기준으로 토큰을 발급한다")
    void login_success() {
        ClientSessionMetadata sessionMetadata = new ClientSessionMetadata("192.168.0.0/24", "Mozilla/5.0", Instant.parse("2026-03-27T10:00:00Z"));
        given(credentialAuthenticator.authenticate("user@test.com", "password1234!@@")).willReturn(1L);
        given(tokenService.issueTokens(1L, sessionMetadata)).willReturn(new TokenResult("access", "refresh"));
        given(refreshTokenService.resolveSessionId("refresh")).willReturn("session-1");

        TokenResult result = authService.login(new SignInCommand("user@test.com", "password1234!@@", sessionMetadata));

        assertThat(result.accessToken()).isEqualTo("access");
    }

    @Test
    @DisplayName("기존 카카오 identity 가 있으면 바로 로그인한다")
    void loginWithKakao_existingIdentity() {
        ClientSessionMetadata sessionMetadata = new ClientSessionMetadata("192.168.0.0/24", "Mozilla/5.0", Instant.parse("2026-03-27T10:00:00Z"));
        given(kakaoAuthClient.exchangeAuthorizationCode("code", "redirect")).willReturn("kakao-access-token");
        given(kakaoAuthClient.getUserInfo("kakao-access-token"))
            .willReturn(new KakaoUserInfo("kakao-user-id", "user@test.com", true, "tester", null));
        given(authIdentityRepository.findByProviderAndProviderUserId(AuthProvider.KAKAO, "kakao-user-id"))
            .willReturn(Optional.of(AuthIdentity.create(1L, AuthProvider.KAKAO, "kakao-user-id", "user@test.com", true)));
        given(tokenService.issueTokens(1L, sessionMetadata)).willReturn(new TokenResult("access", "refresh"));
        given(authTokenProvider.getUserId("access")).willReturn(1L);
        given(refreshTokenService.resolveSessionId("refresh")).willReturn("session-1");

        TokenResult result = authService.loginWithKakao(new KakaoLoginCommand("code", "redirect", sessionMetadata));

        assertThat(result.accessToken()).isEqualTo("access");
    }

    @Test
    @DisplayName("카카오 신규 로그인이고 기존 이메일 계정이 없으면 소셜 유저를 생성한다")
    void loginWithKakao_registersSocialUser() {
        ClientSessionMetadata sessionMetadata = new ClientSessionMetadata("192.168.0.0/24", "Mozilla/5.0", Instant.parse("2026-03-27T10:00:00Z"));
        given(kakaoAuthClient.exchangeAuthorizationCode("code", "redirect")).willReturn("kakao-access-token");
        given(kakaoAuthClient.getUserInfo("kakao-access-token"))
            .willReturn(new KakaoUserInfo("kakao-user-id", "social@test.com", true, "tester", "https://image"));
        given(authIdentityRepository.findByProviderAndProviderUserId(AuthProvider.KAKAO, "kakao-user-id"))
            .willReturn(Optional.empty());
        given(userInternalFacade.findActiveUserByEmail("social@test.com")).willReturn(Optional.empty());
        given(userInternalFacade.registerSocialUser(any(RegisterSocialUserCommand.class)))
            .willReturn(new RegisteredUserResult(3L, "social@test.com", "tester"));
        given(tokenService.issueTokens(3L, sessionMetadata)).willReturn(new TokenResult("access", "refresh"));
        given(authTokenProvider.getUserId("access")).willReturn(3L);
        given(refreshTokenService.resolveSessionId("refresh")).willReturn("session-3");

        TokenResult result = authService.loginWithKakao(new KakaoLoginCommand("code", "redirect", sessionMetadata));

        assertThat(result.refreshToken()).isEqualTo("refresh");
        ArgumentCaptor<RegisterSocialUserCommand> captor = ArgumentCaptor.forClass(RegisterSocialUserCommand.class);
        then(userInternalFacade).should().registerSocialUser(captor.capture());
        assertThat(captor.getValue().provider()).isEqualTo("KAKAO");
        then(authIdentityRepository).should().save(any(AuthIdentity.class));
    }

    @Test
    @DisplayName("카카오 이메일이 기존 로컬 계정과 충돌하면 링크 필요 예외를 던진다")
    void loginWithKakao_requiresLinkWhenExistingLocalUserExists() {
        given(kakaoAuthClient.exchangeAuthorizationCode("code", "redirect")).willReturn("kakao-access-token");
        given(kakaoAuthClient.getUserInfo("kakao-access-token"))
            .willReturn(new KakaoUserInfo("kakao-user-id", "user@test.com", true, "tester", null));
        given(authIdentityRepository.findByProviderAndProviderUserId(AuthProvider.KAKAO, "kakao-user-id"))
            .willReturn(Optional.empty());
        given(userInternalFacade.findActiveUserByEmail("user@test.com"))
            .willReturn(Optional.of(new UserAuthView(1L, "user@test.com", "encoded", "USER", true, false, 0L, "tester", null)));

        assertThatThrownBy(() -> authService.loginWithKakao(new KakaoLoginCommand("code", "redirect")))
            .extracting("errorCode")
            .isEqualTo(SOCIAL_LINK_REQUIRED);
    }

    @Test
    @DisplayName("카카오 이메일 인증이 안 되어 있으면 연결을 거부한다")
    void loginWithKakao_requiresVerifiedEmail() {
        given(kakaoAuthClient.exchangeAuthorizationCode("code", "redirect")).willReturn("kakao-access-token");
        given(kakaoAuthClient.getUserInfo("kakao-access-token"))
            .willReturn(new KakaoUserInfo("kakao-user-id", "user@test.com", false, "tester", null));

        assertThatThrownBy(() -> authService.loginWithKakao(new KakaoLoginCommand("code", "redirect")))
            .extracting("errorCode")
            .isEqualTo(SOCIAL_EMAIL_NOT_VERIFIED);
    }

    @Test
    @DisplayName("현재 로그인된 계정과 카카오 verified email 이 같으면 identity 를 연결한다")
    void linkKakao_success() {
        given(userInternalFacade.findActiveUserById(1L))
            .willReturn(Optional.of(new UserAuthView(1L, "user@test.com", "encoded", "USER", true, false, 0L, "tester", null)));
        given(kakaoAuthClient.exchangeAuthorizationCode("code", "redirect")).willReturn("kakao-access-token");
        given(kakaoAuthClient.getUserInfo("kakao-access-token"))
            .willReturn(new KakaoUserInfo("kakao-user-id", "user@test.com", true, "tester", null));
        given(authIdentityRepository.findByProviderAndProviderUserId(AuthProvider.KAKAO, "kakao-user-id"))
            .willReturn(Optional.empty());
        given(authIdentityRepository.findByUserIdAndProvider(1L, AuthProvider.KAKAO))
            .willReturn(Optional.empty());

        authService.linkKakao(1L, new KakaoLinkCommand("code", "redirect"));

        then(authIdentityRepository).should().save(any(AuthIdentity.class));
    }

    @Test
    @DisplayName("이미 연결된 카카오 계정이면 중복 연결을 거부한다")
    void linkKakao_alreadyLinked() {
        given(userInternalFacade.findActiveUserById(1L))
            .willReturn(Optional.of(new UserAuthView(1L, "user@test.com", "encoded", "USER", true, false, 0L, "tester", null)));
        given(kakaoAuthClient.exchangeAuthorizationCode("code", "redirect")).willReturn("kakao-access-token");
        given(kakaoAuthClient.getUserInfo("kakao-access-token"))
            .willReturn(new KakaoUserInfo("kakao-user-id", "user@test.com", true, "tester", null));
        given(authIdentityRepository.findByProviderAndProviderUserId(AuthProvider.KAKAO, "kakao-user-id"))
            .willReturn(Optional.of(AuthIdentity.create(2L, AuthProvider.KAKAO, "kakao-user-id", "user@test.com", true)));

        assertThatThrownBy(() -> authService.linkKakao(1L, new KakaoLinkCommand("code", "redirect")))
            .extracting("errorCode")
            .isEqualTo(SOCIAL_ACCOUNT_ALREADY_LINKED);
    }
}
