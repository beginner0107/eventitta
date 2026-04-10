package com.eventitta.domain.auth.service;

import com.eventitta.domain.auth.domain.RefreshToken;
import com.eventitta.domain.auth.port.AuthTokenProvider;
import com.eventitta.domain.auth.repository.RefreshTokenRepository;
import com.eventitta.domain.auth.dto.ClientSessionMetadata;
import com.eventitta.domain.auth.dto.TokenResult;
import com.eventitta.domain.user.api.internal.facade.UserInternalFacade;
import com.eventitta.domain.user.api.internal.view.UserAuthView;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

import static com.eventitta.domain.user.exception.UserErrorCode.NOT_FOUND_USER_ID;

@Service
@RequiredArgsConstructor
@Transactional
public class TokenService {
    private final AuthTokenProvider authTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final Pbkdf2PasswordEncoder refreshTokenEncoder;
    private final UserInternalFacade userInternalFacade;
    private final Clock clock;

    public TokenResult issueTokens(Long userId) {
        return issueTokens(userId, null);
    }

    public TokenResult issueTokens(Long userId, ClientSessionMetadata sessionMetadata) {
        UserAuthView user = userInternalFacade.findActiveUserById(userId)
            .orElseThrow(NOT_FOUND_USER_ID::defaultException);

        return issueTokens(user, sessionMetadata, null);
    }

    public TokenResult rotateTokens(RefreshToken session, ClientSessionMetadata sessionMetadata) {
        UserAuthView user = userInternalFacade.findActiveUserById(session.getUserId())
            .orElseThrow(NOT_FOUND_USER_ID::defaultException);

        return issueTokens(user, sessionMetadata, session);
    }

    private TokenResult issueTokens(UserAuthView user, ClientSessionMetadata sessionMetadata, RefreshToken existingSession) {
        String sessionId = existingSession != null ? existingSession.getSessionId() : UUID.randomUUID().toString();
        String accessToken = authTokenProvider.createAccessToken(
            user.userId(),
            user.email(),
            user.role(),
            user.authVersion(),
            sessionId
        );
        String refreshTokenKey = authTokenProvider.createRefreshTokenKey();
        String refreshTokenSecret = authTokenProvider.createRefreshTokenSecret();
        String tokenHash = refreshTokenEncoder.encode(refreshTokenSecret);
        Instant expiresAt = authTokenProvider.getRefreshTokenExpiry();
        LocalDateTime expiresAtInAppZone = LocalDateTime.ofInstant(expiresAt, clock.getZone());
        LocalDateTime observedAtInAppZone = resolveObservedAt(sessionMetadata);
        String maskedIp = sessionMetadata != null ? sessionMetadata.maskedIp() : null;
        String userAgent = sessionMetadata != null ? sessionMetadata.userAgent() : null;

        if (existingSession == null) {
            refreshTokenRepository.save(RefreshToken.issue(
                user.userId(),
                sessionId,
                refreshTokenKey,
                tokenHash,
                expiresAtInAppZone,
                observedAtInAppZone,
                maskedIp,
                userAgent
            ));
        } else {
            existingSession.rotate(
                refreshTokenKey,
                tokenHash,
                expiresAtInAppZone,
                observedAtInAppZone,
                maskedIp,
                userAgent
            );
            refreshTokenRepository.save(existingSession);
        }

        return new TokenResult(accessToken, refreshTokenKey + "." + refreshTokenSecret);
    }

    private LocalDateTime resolveObservedAt(ClientSessionMetadata sessionMetadata) {
        Instant observedAt = sessionMetadata != null && sessionMetadata.observedAt() != null
            ? sessionMetadata.observedAt()
            : clock.instant();
        return LocalDateTime.ofInstant(observedAt, clock.getZone());
    }
}
