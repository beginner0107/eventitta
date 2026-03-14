package com.eventitta.auth.service;

import com.eventitta.auth.service.dto.SignUpCommand;
import com.eventitta.user.domain.User;
import com.eventitta.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

import static com.eventitta.user.exception.UserErrorCode.CONFLICTED_EMAIL;
import static com.eventitta.user.exception.UserErrorCode.CONFLICTED_NICKNAME;

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
            return userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException ex) {
            throw mapSignupConflict(ex);
        }
    }

    private RuntimeException mapSignupConflict(DataIntegrityViolationException ex) {
        String message = extractExceptionMessage(ex);
        if (message.contains("uk_users_email") || message.contains("users.email")) {
            return CONFLICTED_EMAIL.defaultException(ex);
        }
        if (message.contains("uk_users_nickname") || message.contains("users.nickname")) {
            return CONFLICTED_NICKNAME.defaultException(ex);
        }
        throw ex;
    }

    private String extractExceptionMessage(Throwable throwable) {
        StringBuilder builder = new StringBuilder();
        Throwable current = throwable;

        while (current != null) {
            if (current.getMessage() != null) {
                builder.append(current.getMessage()).append(' ');
            }
            current = current.getCause();
        }

        return builder.toString().toLowerCase(Locale.ROOT);
    }
}
