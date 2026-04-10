package com.eventitta.domain.auth.service;

import com.eventitta.domain.auth.config.AuthActionTokenProperties;
import com.eventitta.domain.auth.domain.AuthActionToken;
import com.eventitta.domain.auth.domain.AuthActionTokenPurpose;
import com.eventitta.domain.auth.repository.AuthActionTokenRepository;
import com.eventitta.domain.auth.service.dto.ClientSessionMetadata;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.LocalDateTime;

import static com.eventitta.domain.auth.exception.AuthErrorCode.ACTION_TOKEN_EXPIRED;
import static com.eventitta.domain.auth.exception.AuthErrorCode.ACTION_TOKEN_INVALID;
import static com.eventitta.domain.auth.exception.AuthErrorCode.ACTION_TOKEN_USED;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthActionTokenService {

    private final AuthActionTokenRepository authActionTokenRepository;
    private final Pbkdf2PasswordEncoder refreshTokenEncoder;
    private final AuthActionTokenProperties properties;
    private final Clock clock;

    public String issue(Long userId, String targetEmail, AuthActionTokenPurpose purpose, ClientSessionMetadata sessionMetadata) {
        authActionTokenRepository.deleteByUserIdAndPurpose(userId, purpose);

        String tokenKey = TokenKeyGenerator.newKey();
        String tokenSecret = TokenKeyGenerator.newSecret();
        String tokenHash = refreshTokenEncoder.encode(tokenSecret);
        LocalDateTime expiresAt = LocalDateTime.now(clock).plusSeconds(ttlSecondsFor(purpose));

        authActionTokenRepository.save(AuthActionToken.issue(
            userId,
            purpose,
            tokenKey,
            tokenHash,
            targetEmail,
            expiresAt,
            sessionMetadata != null ? sessionMetadata.maskedIp() : null,
            sessionMetadata != null ? sessionMetadata.userAgent() : null
        ));

        return tokenKey + "." + tokenSecret;
    }

    public AuthActionToken consume(AuthActionTokenPurpose purpose, String rawToken) {
        TokenParts parts = parse(rawToken);
        AuthActionToken token = authActionTokenRepository.findByTokenKeyForUpdate(parts.tokenKey())
            .orElseThrow(ACTION_TOKEN_INVALID::defaultException);

        if (token.getPurpose() != purpose) {
            throw ACTION_TOKEN_INVALID.defaultException();
        }
        if (token.isUsed()) {
            throw ACTION_TOKEN_USED.defaultException();
        }
        if (token.getExpiresAt().isBefore(LocalDateTime.now(clock))) {
            throw ACTION_TOKEN_EXPIRED.defaultException();
        }
        if (!refreshTokenEncoder.matches(parts.secret(), token.getTokenHash())) {
            throw ACTION_TOKEN_INVALID.defaultException();
        }

        token.markUsed(LocalDateTime.now(clock));
        authActionTokenRepository.save(token);
        return token;
    }

    private long ttlSecondsFor(AuthActionTokenPurpose purpose) {
        return switch (purpose) {
            case EMAIL_VERIFICATION -> properties.getEmailVerificationTtlSeconds();
            case PASSWORD_RESET -> properties.getPasswordResetTtlSeconds();
        };
    }

    private TokenParts parse(String rawToken) {
        if (!StringUtils.hasText(rawToken)) {
            throw ACTION_TOKEN_INVALID.defaultException();
        }
        String[] parts = rawToken.split("\\.", 2);
        if (parts.length != 2 || !StringUtils.hasText(parts[0]) || !StringUtils.hasText(parts[1])) {
            throw ACTION_TOKEN_INVALID.defaultException();
        }
        return new TokenParts(parts[0], parts[1]);
    }

    private record TokenParts(String tokenKey, String secret) {
    }
}
