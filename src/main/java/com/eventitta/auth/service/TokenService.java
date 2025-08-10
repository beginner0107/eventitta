package com.eventitta.auth.service;

import com.eventitta.auth.domain.RefreshToken;
import com.eventitta.auth.dto.response.TokenResponse;
import com.eventitta.auth.jwt.JwtTokenProvider;
import com.eventitta.auth.repository.RefreshTokenRepository;
import com.eventitta.user.domain.User;
import com.eventitta.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Transactional
public class TokenService {
    private final JwtTokenProvider tokenProvider;
    private final RefreshTokenRepository rtRepo;
    private final Pbkdf2PasswordEncoder rtEncoder;
    private final UserRepository userRepository;

    public TokenResponse issueTokens(Long userId) {
        String at = tokenProvider.createAccessToken(userId);
        String rt = tokenProvider.createRefreshToken();
        persistRefreshToken(userId, rt);
        return new TokenResponse(at, rt);
    }

    private void persistRefreshToken(Long userId, String rawRt) {
        String hash = rtEncoder.encode(rawRt);
        Instant expiresAt = tokenProvider.getRefreshTokenExpiry();

        User u = userRepository.getReferenceById(userId);
        rtRepo.save(new RefreshToken(u, hash, expiresAt));
    }
}
