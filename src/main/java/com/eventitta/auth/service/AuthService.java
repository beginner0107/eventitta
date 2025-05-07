package com.eventitta.auth.service;

import com.eventitta.auth.exception.SignUpErrorCode;
import com.eventitta.user.domain.User;
import com.eventitta.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@Transactional
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;

    public User signUp(User user) {
        if (userRepository.existsByEmail(user.getEmail())) {
            throw SignUpErrorCode.CONFLICTED_EMAIL.defaultException();
        }
        if (userRepository.existsByNickname(user.getNickname())) {
            throw SignUpErrorCode.CONFLICTED_NICKNAME.defaultException();
        }
        return userRepository.save(user);
    }
}
