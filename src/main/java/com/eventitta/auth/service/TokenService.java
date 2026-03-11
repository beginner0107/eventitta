package com.eventitta.auth.service;

import com.eventitta.auth.domain.RefreshToken;
import com.eventitta.auth.jwt.JwtTokenProvider;
import com.eventitta.auth.repository.RefreshTokenRepository;
import com.eventitta.auth.service.dto.TokenResult;
import com.eventitta.user.domain.User;
import com.eventitta.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static com.eventitta.user.exception.UserErrorCode.NOT_FOUND_USER_ID;

@Service
@RequiredArgsConstructor
@Transactional
public class TokenService {
    private final JwtTokenProvider tokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final Pbkdf2PasswordEncoder pbkdf2PasswordEncoder;
    private final UserRepository userRepository;

    public TokenResult issueTokens(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(NOT_FOUND_USER_ID::defaultException);

        String at = tokenProvider.createAccessToken(userId, user.getEmail(), user.getRole().name());
        String rt = tokenProvider.createRefreshToken();
        persistRefreshToken(user, rt);
        return new TokenResult(at, rt);
    }

    private void persistRefreshToken(User user, String rawRt) {
        String hash = pbkdf2PasswordEncoder.encode(rawRt);
        Instant expiresAt = tokenProvider.getRefreshTokenExpiry();

        refreshTokenRepository.save(new RefreshToken(user, hash, expiresAt));
    }
}
