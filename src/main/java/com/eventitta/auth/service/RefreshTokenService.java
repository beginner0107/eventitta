package com.eventitta.auth.service;

import com.eventitta.auth.domain.RefreshToken;
import com.eventitta.auth.dto.response.TokenResponse;
import com.eventitta.auth.jwt.JwtTokenProvider;
import com.eventitta.auth.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static com.eventitta.auth.exception.AuthErrorCode.*;

@Service
@RequiredArgsConstructor
@Transactional
public class RefreshTokenService {
    private final JwtTokenProvider tokenProvider;
    private final RefreshTokenRepository rtRepo;
    private final Pbkdf2PasswordEncoder rtEncoder;
    private final TokenService tokenService;

    public TokenResponse refresh(String expiredAt, String rawRt) {
        if (rawRt == null) throw REFRESH_TOKEN_MISSING.defaultException();

        Long userId = tokenProvider.getUserIdFromExpiredToken(expiredAt);
        RefreshToken entity = rtRepo.findByUserId(userId)
            .orElseThrow(REFRESH_TOKEN_INVALID::defaultException);

        if (!rtEncoder.matches(rawRt, entity.getTokenHash()))
            throw REFRESH_TOKEN_INVALID.defaultException();

        if (entity.getExpiresAt().isBefore(LocalDateTime.now()))
            throw REFRESH_TOKEN_EXPIRED.defaultException();

        return tokenService.issueTokens(userId);
    }

    public void invalidateByAccessToken(String accessToken) {
        Long userId = tokenProvider.getUserIdFromExpiredToken(accessToken);
        rtRepo.deleteByUserId(userId);
    }
}
