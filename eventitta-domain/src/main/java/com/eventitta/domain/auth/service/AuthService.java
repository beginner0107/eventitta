package com.eventitta.domain.auth.service;

import com.eventitta.domain.auth.domain.AuthActionToken;
import com.eventitta.domain.auth.domain.AuthActionTokenPurpose;
import com.eventitta.domain.auth.domain.AuthIdentity;
import com.eventitta.domain.auth.domain.AuthProvider;
import com.eventitta.domain.auth.domain.AuthSecurityEventOutcome;
import com.eventitta.domain.auth.domain.AuthSecurityEventType;
import com.eventitta.domain.auth.exception.AuthException;
import com.eventitta.domain.auth.exception.AuthErrorCode;
import com.eventitta.domain.auth.port.AuthMailSender;
import com.eventitta.domain.auth.port.AuthTokenProvider;
import com.eventitta.domain.auth.port.CredentialAuthenticator;
import com.eventitta.domain.auth.port.KakaoAuthClient;
import com.eventitta.domain.auth.dto.KakaoUserInfo;
import com.eventitta.domain.auth.repository.AuthIdentityRepository;
import com.eventitta.domain.auth.repository.RefreshTokenRepository;
import com.eventitta.domain.auth.dto.AuthSessionMetadata;
import com.eventitta.domain.auth.dto.ClientSessionMetadata;
import com.eventitta.domain.auth.dto.KakaoLinkCommand;
import com.eventitta.domain.auth.dto.KakaoLoginCommand;
import com.eventitta.domain.auth.dto.LogoutCommand;
import com.eventitta.domain.auth.dto.RefreshCommand;
import com.eventitta.domain.auth.dto.SignInCommand;
import com.eventitta.domain.auth.dto.SignUpCommand;
import com.eventitta.domain.auth.dto.SignUpResult;
import com.eventitta.domain.auth.dto.TokenResult;
import com.eventitta.domain.user.api.internal.command.RegisterLocalUserCommand;
import com.eventitta.domain.user.api.internal.command.RegisterSocialUserCommand;
import com.eventitta.domain.user.api.internal.facade.UserInternalFacade;
import com.eventitta.domain.user.api.internal.result.RegisteredUserResult;
import com.eventitta.domain.user.api.internal.view.UserAuthView;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

import static com.eventitta.domain.auth.exception.AuthErrorCode.*;
import static com.eventitta.domain.user.exception.UserErrorCode.NOT_FOUND_USER_ID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {
    private static final String KAKAO_PROVIDER_NAME = "KAKAO";
    private static final String LOGIN_FLOW = "login";
    private static final String SOCIAL_LOGIN_FLOW = "social-login";
    private static final String REFRESH_FLOW = "refresh";

    private final UserInternalFacade userInternalFacade;
    private final PasswordEncoder passwordEncoder;
    private final CredentialAuthenticator credentialAuthenticator;
    private final TokenService tokenService;
    private final RefreshTokenService refreshTokenService;
    private final KakaoAuthClient kakaoAuthClient;
    private final AuthIdentityRepository authIdentityRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuthActionTokenService authActionTokenService;
    private final AuthMailSender authMailSender;
    private final AuthAttemptGuard authAttemptGuard;
    private final AuthSecurityEventService authSecurityEventService;
    private final AuthTokenProvider authTokenProvider;

    public SignUpResult signUp(SignUpCommand command) {
        RegisteredUserResult user = userInternalFacade.registerLocalUser(new RegisterLocalUserCommand(
            command.email(),
            passwordEncoder.encode(command.password()),
            command.nickname()
        ));
        String rawToken = authActionTokenService.issue(
            user.userId(),
            user.email(),
            AuthActionTokenPurpose.EMAIL_VERIFICATION,
            null
        );
        authMailSender.sendEmailVerification(user.email(), rawToken);
        authSecurityEventService.record(
            user.userId(),
            AuthSecurityEventType.EMAIL_VERIFICATION_REQUEST,
            AuthSecurityEventOutcome.SUCCESS,
            null,
            user.email(),
            null,
            null,
            "signup verification token issued"
        );
        return new SignUpResult(user.email(), user.nickname());
    }

    public TokenResult login(SignInCommand command) {
        try {
            authAttemptGuard.assertAllowed(LOGIN_FLOW, command.email(), maskedIp(command.sessionMetadata()));
            Long userId = credentialAuthenticator.authenticate(command.email(), command.password());
            TokenResult result = tokenService.issueTokens(userId, command.sessionMetadata());
            authAttemptGuard.recordSuccess(LOGIN_FLOW, command.email(), maskedIp(command.sessionMetadata()));
            authSecurityEventService.record(
                userId,
                AuthSecurityEventType.LOGIN,
                AuthSecurityEventOutcome.SUCCESS,
                refreshTokenService.resolveSessionId(result.refreshToken()),
                command.email(),
                maskedIp(command.sessionMetadata()),
                userAgent(command.sessionMetadata()),
                "password login succeeded"
            );
            return result;
        } catch (RuntimeException ex) {
            if (!isRateLimited(ex)) {
                authAttemptGuard.recordFailure(LOGIN_FLOW, command.email(), maskedIp(command.sessionMetadata()));
            }
            authSecurityEventService.record(
                null,
                AuthSecurityEventType.LOGIN,
                AuthSecurityEventOutcome.FAILURE,
                null,
                command.email(),
                maskedIp(command.sessionMetadata()),
                userAgent(command.sessionMetadata()),
                ex.getMessage()
            );
            throw ex;
        }
    }

    public TokenResult refresh(RefreshCommand command) {
        String refreshKey = refreshTokenService.resolveTokenKey(command.refreshToken());
        try {
            authAttemptGuard.assertAllowed(REFRESH_FLOW, refreshKey, maskedIp(command.sessionMetadata()));
            TokenResult result = refreshTokenService.refresh(command);
            authAttemptGuard.recordSuccess(REFRESH_FLOW, refreshKey, maskedIp(command.sessionMetadata()));
            authSecurityEventService.record(
                authTokenProvider.getUserId(result.accessToken()),
                AuthSecurityEventType.REFRESH,
                AuthSecurityEventOutcome.SUCCESS,
                refreshTokenService.resolveSessionId(result.refreshToken()),
                refreshKey,
                maskedIp(command.sessionMetadata()),
                userAgent(command.sessionMetadata()),
                "refresh rotation succeeded"
            );
            return result;
        } catch (RuntimeException ex) {
            if (!isRateLimited(ex)) {
                authAttemptGuard.recordFailure(REFRESH_FLOW, refreshKey, maskedIp(command.sessionMetadata()));
            }
            authSecurityEventService.record(
                null,
                AuthSecurityEventType.REFRESH,
                AuthSecurityEventOutcome.FAILURE,
                null,
                refreshKey,
                maskedIp(command.sessionMetadata()),
                userAgent(command.sessionMetadata()),
                ex.getMessage()
            );
            throw ex;
        }
    }

    public void logout(LogoutCommand command) {
        if (!StringUtils.hasText(command.refreshToken())) {
            return;
        }

        try {
            refreshTokenService.invalidate(command.refreshToken());
        } catch (AuthException ex) {
            log.debug("[로그아웃] refresh token 무효화 오류 무시", ex);
        }
    }

    public TokenResult loginWithKakao(KakaoLoginCommand command) {
        String guardIdentifier = "kakao";
        try {
            authAttemptGuard.assertAllowed(SOCIAL_LOGIN_FLOW, guardIdentifier, maskedIp(command.sessionMetadata()));
            KakaoUserInfo kakaoUserInfo = loadVerifiedKakaoUser(command.code(), command.redirectUri());
            TokenResult result = authIdentityRepository.findByProviderAndProviderUserId(AuthProvider.KAKAO, kakaoUserInfo.providerUserId())
                .map(authIdentity -> tokenService.issueTokens(authIdentity.getUserId(), command.sessionMetadata()))
                .orElseGet(() -> registerOrRequireLink(kakaoUserInfo, command.sessionMetadata()));
            authAttemptGuard.recordSuccess(SOCIAL_LOGIN_FLOW, guardIdentifier, maskedIp(command.sessionMetadata()));
            authSecurityEventService.record(
                authTokenProvider.getUserId(result.accessToken()),
                AuthSecurityEventType.SOCIAL_LOGIN,
                AuthSecurityEventOutcome.SUCCESS,
                refreshTokenService.resolveSessionId(result.refreshToken()),
                kakaoUserInfo.email(),
                maskedIp(command.sessionMetadata()),
                userAgent(command.sessionMetadata()),
                "kakao login succeeded"
            );
            return result;
        } catch (RuntimeException ex) {
            if (!isRateLimited(ex)) {
                authAttemptGuard.recordFailure(SOCIAL_LOGIN_FLOW, guardIdentifier, maskedIp(command.sessionMetadata()));
            }
            authSecurityEventService.record(
                null,
                AuthSecurityEventType.SOCIAL_LOGIN,
                AuthSecurityEventOutcome.FAILURE,
                null,
                guardIdentifier,
                maskedIp(command.sessionMetadata()),
                userAgent(command.sessionMetadata()),
                ex.getMessage()
            );
            throw ex;
        }
    }

    public void linkKakao(Long userId, KakaoLinkCommand command) {
        UserAuthView currentUser = userInternalFacade.findActiveUserById(userId)
            .orElseThrow(NOT_FOUND_USER_ID::defaultException);

        KakaoUserInfo kakaoUserInfo = loadVerifiedKakaoUser(command.code(), command.redirectUri());

        if (authIdentityRepository.findByProviderAndProviderUserId(AuthProvider.KAKAO, kakaoUserInfo.providerUserId()).isPresent()
            || authIdentityRepository.findByUserIdAndProvider(userId, AuthProvider.KAKAO).isPresent()) {
            throw SOCIAL_ACCOUNT_ALREADY_LINKED.defaultException();
        }

        if (!currentUser.email().equalsIgnoreCase(kakaoUserInfo.email())) {
            throw SOCIAL_LINK_REQUIRED.defaultException();
        }

        authIdentityRepository.save(AuthIdentity.create(
            userId,
            AuthProvider.KAKAO,
            kakaoUserInfo.providerUserId(),
            kakaoUserInfo.email(),
            true
        ));
        authSecurityEventService.record(
            userId,
            AuthSecurityEventType.SOCIAL_LINK,
            AuthSecurityEventOutcome.SUCCESS,
            null,
            kakaoUserInfo.email(),
            null,
            null,
            "kakao linked to current account"
        );
    }

    public void unlinkKakao(Long userId) {
        UserAuthView user = userInternalFacade.findActiveUserById(userId)
            .orElseThrow(NOT_FOUND_USER_ID::defaultException);
        if (authIdentityRepository.findByUserIdAndProvider(userId, AuthProvider.KAKAO).isEmpty()) {
            throw SOCIAL_ACCOUNT_NOT_LINKED.defaultException();
        }
        if (user.encodedPassword() == null && authIdentityRepository.countByUserId(userId) <= 1) {
            throw SOCIAL_UNLINK_NOT_ALLOWED.defaultException();
        }

        authIdentityRepository.deleteByUserIdAndProvider(userId, AuthProvider.KAKAO);
        UserAuthView updatedUser = userInternalFacade.unlinkKakaoProvider(userId);
        refreshTokenRepository.deleteByUserId(userId);
        authSecurityEventService.record(
            userId,
            AuthSecurityEventType.SOCIAL_UNLINK,
            AuthSecurityEventOutcome.SUCCESS,
            null,
            updatedUser.email(),
            null,
            null,
            "kakao unlinked and sessions invalidated"
        );
    }

    public void requestEmailVerification(String email, ClientSessionMetadata sessionMetadata) {
        userInternalFacade.findActiveUserByEmail(email)
            .filter(user -> user.encodedPassword() != null)
            .filter(user -> !user.emailVerified())
            .ifPresent(user -> {
                String rawToken = authActionTokenService.issue(
                    user.userId(),
                    user.email(),
                    AuthActionTokenPurpose.EMAIL_VERIFICATION,
                    sessionMetadata
                );
                authMailSender.sendEmailVerification(user.email(), rawToken);
                authSecurityEventService.record(
                    user.userId(),
                    AuthSecurityEventType.EMAIL_VERIFICATION_REQUEST,
                    AuthSecurityEventOutcome.SUCCESS,
                    null,
                    user.email(),
                    maskedIp(sessionMetadata),
                    userAgent(sessionMetadata),
                    "verification token re-issued"
                );
            });
    }

    public void confirmEmailVerification(String rawToken) {
        AuthActionToken token = authActionTokenService.consume(AuthActionTokenPurpose.EMAIL_VERIFICATION, rawToken);
        UserAuthView user = userInternalFacade.markEmailVerified(token.getUserId());
        authSecurityEventService.record(
            user.userId(),
            AuthSecurityEventType.EMAIL_VERIFICATION_CONFIRM,
            AuthSecurityEventOutcome.SUCCESS,
            null,
            user.email(),
            null,
            null,
            "email verified"
        );
    }

    public void requestPasswordReset(String email, ClientSessionMetadata sessionMetadata) {
        userInternalFacade.findActiveUserByEmail(email)
            .filter(user -> user.encodedPassword() != null)
            .ifPresent(user -> {
                String rawToken = authActionTokenService.issue(
                    user.userId(),
                    user.email(),
                    AuthActionTokenPurpose.PASSWORD_RESET,
                    sessionMetadata
                );
                authMailSender.sendPasswordReset(user.email(), rawToken);
                authSecurityEventService.record(
                    user.userId(),
                    AuthSecurityEventType.PASSWORD_RESET_REQUEST,
                    AuthSecurityEventOutcome.SUCCESS,
                    null,
                    user.email(),
                    maskedIp(sessionMetadata),
                    userAgent(sessionMetadata),
                    "password reset token issued"
                );
            });
    }

    public void confirmPasswordReset(String rawToken, String newPassword) {
        AuthActionToken token = authActionTokenService.consume(AuthActionTokenPurpose.PASSWORD_RESET, rawToken);
        UserAuthView user = userInternalFacade.resetPassword(token.getUserId(), passwordEncoder.encode(newPassword));
        refreshTokenRepository.deleteByUserId(user.userId());
        authSecurityEventService.record(
            user.userId(),
            AuthSecurityEventType.PASSWORD_RESET_CONFIRM,
            AuthSecurityEventOutcome.SUCCESS,
            null,
            user.email(),
            null,
            null,
            "password reset completed and sessions invalidated"
        );
    }

    public void setLocalPassword(Long userId, String newPassword) {
        UserAuthView user = userInternalFacade.findActiveUserById(userId)
            .orElseThrow(NOT_FOUND_USER_ID::defaultException);
        if (user.suspended()) {
            throw ACCOUNT_SUSPENDED.defaultException();
        }
        if (user.encodedPassword() != null) {
            throw LOCAL_PASSWORD_ALREADY_SET.defaultException();
        }
        UserAuthView updatedUser = userInternalFacade.setLocalPassword(userId, passwordEncoder.encode(newPassword));
        refreshTokenRepository.deleteByUserId(userId);
        authSecurityEventService.record(
            userId,
            AuthSecurityEventType.SET_LOCAL_PASSWORD,
            AuthSecurityEventOutcome.SUCCESS,
            null,
            updatedUser.email(),
            null,
            null,
            "local password set and sessions invalidated"
        );
    }

    @Transactional(readOnly = true)
    public List<AuthSessionMetadata> getSessions(Long userId, String currentSessionId) {
        return refreshTokenRepository.findAllByUserIdOrderByLastSeenAtDesc(userId).stream()
            .map(session -> new AuthSessionMetadata(
                session.getSessionId(),
                session.getCreatedAt(),
                session.getLastSeenAt(),
                session.getExpiresAt(),
                session.getIssuedIpMasked(),
                session.getIssuedUserAgent(),
                session.getLastSeenIpMasked(),
                session.getLastSeenUserAgent(),
                session.getSessionId().equals(currentSessionId)
            ))
            .toList();
    }

    public boolean revokeSession(Long userId, String sessionId, String currentSessionId) {
        refreshTokenRepository.findByUserIdAndSessionId(userId, sessionId)
            .orElseThrow(REFRESH_TOKEN_INVALID::defaultException);
        refreshTokenRepository.deleteByUserIdAndSessionId(userId, sessionId);
        return sessionId.equals(currentSessionId);
    }

    public boolean revokeAllSessions(Long userId, String currentSessionId) {
        UserAuthView user = userInternalFacade.bumpAuthVersion(userId);
        refreshTokenRepository.deleteByUserId(userId);
        authSecurityEventService.record(
            userId,
            AuthSecurityEventType.LOGOUT_ALL,
            AuthSecurityEventOutcome.SUCCESS,
            currentSessionId,
            user.email(),
            null,
            null,
            "all sessions revoked"
        );
        return currentSessionId != null;
    }

    private TokenResult registerOrRequireLink(KakaoUserInfo kakaoUserInfo, ClientSessionMetadata sessionMetadata) {
        if (userInternalFacade.findActiveUserByEmail(kakaoUserInfo.email()).isPresent()) {
            throw SOCIAL_LINK_REQUIRED.defaultException();
        }

        RegisteredUserResult registeredUser = userInternalFacade.registerSocialUser(new RegisterSocialUserCommand(
            kakaoUserInfo.email(),
            kakaoUserInfo.nickname(),
            kakaoUserInfo.profileImageUrl(),
            KAKAO_PROVIDER_NAME,
            kakaoUserInfo.providerUserId()
        ));

        authIdentityRepository.save(AuthIdentity.create(
            registeredUser.userId(),
            AuthProvider.KAKAO,
            kakaoUserInfo.providerUserId(),
            kakaoUserInfo.email(),
            true
        ));

        return tokenService.issueTokens(registeredUser.userId(), sessionMetadata);
    }

    private KakaoUserInfo loadVerifiedKakaoUser(String code, String redirectUri) {
        String kakaoAccessToken = kakaoAuthClient.exchangeAuthorizationCode(code, redirectUri);
        KakaoUserInfo kakaoUserInfo = kakaoAuthClient.getUserInfo(kakaoAccessToken);

        if (!StringUtils.hasText(kakaoUserInfo.providerUserId())) {
            throw SOCIAL_AUTH_FAILED.defaultException();
        }
        if (!StringUtils.hasText(kakaoUserInfo.email())) {
            throw SOCIAL_EMAIL_REQUIRED.defaultException();
        }
        if (!kakaoUserInfo.emailVerified()) {
            throw SOCIAL_EMAIL_NOT_VERIFIED.defaultException();
        }

        return kakaoUserInfo;
    }

    private String maskedIp(ClientSessionMetadata sessionMetadata) {
        return sessionMetadata != null ? sessionMetadata.maskedIp() : null;
    }

    private String userAgent(ClientSessionMetadata sessionMetadata) {
        return sessionMetadata != null ? sessionMetadata.userAgent() : null;
    }

    private boolean isRateLimited(RuntimeException ex) {
        return ex instanceof AuthException authException
            && authException.getErrorCode() == AuthErrorCode.AUTH_RATE_LIMITED;
    }
}
