package com.eventitta.domain.auth.service;

import com.eventitta.domain.auth.domain.RefreshToken;
import com.eventitta.domain.auth.port.AuthTokenProvider;
import com.eventitta.domain.auth.repository.RefreshTokenRepository;
import com.eventitta.domain.auth.service.dto.RefreshCommand;
import com.eventitta.domain.auth.service.dto.TokenResult;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.LocalDateTime;

import static com.eventitta.domain.auth.exception.AuthErrorCode.*;

@Service
@RequiredArgsConstructor
@Transactional
public class RefreshTokenService {
    private final AuthTokenProvider authTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final Pbkdf2PasswordEncoder refreshTokenEncoder;
    private final TokenService tokenService;
    private final Clock clock;

    public TokenResult refresh(RefreshCommand command) {
        RefreshTokenParts refreshTokenParts = parseRefreshToken(command.refreshToken());
        RefreshToken refreshToken = refreshTokenRepository.findByTokenKeyForUpdate(refreshTokenParts.tokenKey())
            .orElseThrow(REFRESH_TOKEN_INVALID::defaultException);

        validateAccessTokenOwner(command.accessToken(), refreshToken.getUserId());
        validateRefreshToken(refreshToken, refreshTokenParts.secret());

        return tokenService.rotateTokens(refreshToken, command.sessionMetadata());
    }

    public void invalidate(String rawRefreshToken) {
        RefreshTokenParts refreshTokenParts = parseRefreshToken(rawRefreshToken);
        refreshTokenRepository.findByTokenKey(refreshTokenParts.tokenKey())
            .ifPresent(refreshToken -> invalidateIfMatched(refreshToken, refreshTokenParts.secret()));
    }

    @Transactional(readOnly = true)
    public String resolveSessionId(String rawRefreshToken) {
        try {
            RefreshTokenParts refreshTokenParts = parseRefreshToken(rawRefreshToken);
            return refreshTokenRepository.findByTokenKey(refreshTokenParts.tokenKey())
                .filter(refreshToken -> refreshTokenEncoder.matches(refreshTokenParts.secret(), refreshToken.getTokenHash()))
                .map(RefreshToken::getSessionId)
                .orElse(null);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    @Transactional(readOnly = true)
    public String resolveTokenKey(String rawRefreshToken) {
        try {
            return parseRefreshToken(rawRefreshToken).tokenKey();
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private void invalidateIfMatched(RefreshToken refreshToken, String rawSecret) {
        if (refreshTokenEncoder.matches(rawSecret, refreshToken.getTokenHash())) {
            refreshTokenRepository.delete(refreshToken);
            return;
        }
        refreshTokenRepository.delete(refreshToken);
        throw REFRESH_TOKEN_INVALID.defaultException();
    }

    private void validateAccessTokenOwner(String accessToken, Long userId) {
        if (!StringUtils.hasText(accessToken)) {
            return;
        }
        Long tokenUserId = authTokenProvider.getUserIdFromExpiredToken(accessToken);
        if (!userId.equals(tokenUserId)) {
            throw ACCESS_TOKEN_INVALID.defaultException();
        }
    }

    private void validateRefreshToken(RefreshToken refreshToken, String rawSecret) {
        LocalDateTime now = LocalDateTime.now(clock);
        if (refreshToken.getExpiresAt().isBefore(now)) {
            refreshTokenRepository.delete(refreshToken);
            throw REFRESH_TOKEN_EXPIRED.defaultException();
        }

        if (!refreshTokenEncoder.matches(rawSecret, refreshToken.getTokenHash())) {
            refreshTokenRepository.delete(refreshToken);
            throw REFRESH_TOKEN_INVALID.defaultException();
        }
    }

    private RefreshTokenParts parseRefreshToken(String rawRefreshToken) {
        if (!StringUtils.hasText(rawRefreshToken)) {
            throw REFRESH_TOKEN_MISSING.defaultException();
        }

        String[] parts = rawRefreshToken.split("\\.", 2);
        if (parts.length != 2 || !StringUtils.hasText(parts[0]) || !StringUtils.hasText(parts[1])) {
            throw REFRESH_TOKEN_INVALID.defaultException();
        }

        return new RefreshTokenParts(parts[0], parts[1]);
    }

    private record RefreshTokenParts(String tokenKey, String secret) {
    }
}
