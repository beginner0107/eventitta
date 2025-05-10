package com.eventitta.auth.service;

import com.eventitta.auth.domain.RefreshToken;
import com.eventitta.auth.domain.UserPrincipal;
import com.eventitta.auth.dto.request.SignUpRequest;
import com.eventitta.auth.dto.response.TokenResponse;
import com.eventitta.auth.exception.SignUpErrorCode;
import com.eventitta.auth.jwt.JwtTokenProvider;
import com.eventitta.auth.repository.RefreshTokenRepository;
import com.eventitta.user.domain.User;
import com.eventitta.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@Transactional
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Pbkdf2PasswordEncoder rtEncoder;
    private final AuthenticationManager authManager;
    private final JwtTokenProvider tokenProvider;
    private final RefreshTokenRepository rtRepo;

    public User signUp(SignUpRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw SignUpErrorCode.CONFLICTED_EMAIL.defaultException();
        }
        if (userRepository.existsByNickname(request.nickname())) {
            throw SignUpErrorCode.CONFLICTED_NICKNAME.defaultException();
        }
        return userRepository.save(request.toEntity(passwordEncoder));
    }

    public TokenResponse login(String email, String rawPassword) {
        Authentication auth = new UsernamePasswordAuthenticationToken(email, rawPassword);
        authManager.authenticate(auth);
        Long userId = ((UserPrincipal) auth.getPrincipal()).getId();
        User user = userRepository.getReferenceById(userId);

        String at = tokenProvider.createAccessToken(userId);
        String rt = tokenProvider.createRefreshToken();

        String rtHash = rtEncoder.encode(rt);
        Instant expiresAt = tokenProvider.getRefreshTokenExpiry();

        rtRepo.findByUserId(userId)
            .ifPresentOrElse(
                existing -> {
                    existing.updateToken(rtHash, expiresAt);
                    rtRepo.save(existing);
                },
                () -> rtRepo.save(new RefreshToken(user, rtHash, expiresAt))
            );

        return new TokenResponse(at, rt);
    }
}
