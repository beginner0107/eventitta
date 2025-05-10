package com.eventitta.auth.service;

import com.eventitta.auth.dto.request.SignUpRequest;
import com.eventitta.auth.exception.SignUpErrorCode;
import com.eventitta.user.domain.User;
import com.eventitta.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@Transactional
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public User signUp(SignUpRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw SignUpErrorCode.CONFLICTED_EMAIL.defaultException();
        }
        if (userRepository.existsByNickname(request.nickname())) {
            throw SignUpErrorCode.CONFLICTED_NICKNAME.defaultException();
        }
        return userRepository.save(request.toEntity(passwordEncoder));
    }
}
