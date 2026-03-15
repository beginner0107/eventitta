package com.eventitta.auth.service;

import com.eventitta.auth.domain.RefreshToken;
import com.eventitta.auth.jwt.JwtTokenProvider;
import com.eventitta.auth.repository.RefreshTokenRepository;
import com.eventitta.auth.service.dto.RefreshCommand;
import com.eventitta.auth.service.dto.TokenResult;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
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
    private final Clock clock;

    public TokenResult refresh(RefreshCommand command) {
        if (command.refreshToken() == null || command.refreshToken().isBlank()) {
            throw REFRESH_TOKEN_MISSING.defaultException();
        }

        LocalDateTime now = LocalDateTime.now(clock);
        RefreshToken entity = resolveRefreshToken(command);

        if (entity.getExpiresAt().isBefore(now)) {
            throw REFRESH_TOKEN_EXPIRED.defaultException();
        }

        rtRepo.delete(entity);
        return tokenService.issueTokens(entity.getUser().getId());
    }

    public void invalidateByToken(String accessToken, String refreshToken) {
        Long userId = tokenProvider.getUserIdFromExpiredToken(accessToken);

        rtRepo.findAllByUserId(userId)
            .stream()
            .filter(token -> rtEncoder.matches(refreshToken, token.getTokenHash()))
            .findFirst()
            .ifPresent(rtRepo::delete);
    }

    private RefreshToken resolveRefreshToken(RefreshCommand command) {
        if (command.accessToken() == null || command.accessToken().isBlank()) {
            throw REFRESH_TOKEN_INVALID.defaultException();
        }

        Long userId = tokenProvider.getUserIdFromExpiredToken(command.accessToken());
        return rtRepo.findAllByUserId(userId)
            .stream()
            .filter(token -> rtEncoder.matches(command.refreshToken(), token.getTokenHash()))
            .findFirst()
            .orElseThrow(REFRESH_TOKEN_INVALID::defaultException);
    }
}
