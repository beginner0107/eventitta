package com.eventitta.auth.service;

import com.eventitta.auth.dto.request.SignUpRequest;
import com.eventitta.user.domain.User;
import com.eventitta.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.eventitta.auth.exception.AuthErrorCode.CONFLICTED_EMAIL;
import static com.eventitta.auth.exception.AuthErrorCode.CONFLICTED_NICKNAME;

@Service
@RequiredArgsConstructor
@Transactional
public class SignUpService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public User register(SignUpRequest req) {
        if (userRepository.existsByEmail(req.email())) throw CONFLICTED_EMAIL.defaultException();
        if (userRepository.existsByNickname(req.nickname())) throw CONFLICTED_NICKNAME.defaultException();
        return userRepository.save(req.toEntity(passwordEncoder));
    }
}
