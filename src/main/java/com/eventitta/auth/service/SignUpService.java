package com.eventitta.auth.service;

import com.eventitta.auth.service.dto.SignUpCommand;
import com.eventitta.user.domain.User;
import com.eventitta.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
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

    public User register(SignUpCommand command) {
        if (userRepository.existsByEmail(command.email())) throw CONFLICTED_EMAIL.defaultException();
        if (userRepository.existsByNickname(command.nickname())) throw CONFLICTED_NICKNAME.defaultException();
        User user = command.toEntity(passwordEncoder);
        try {
            return userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            throw CONFLICTED_EMAIL.defaultException();
        }
    }
}
