package com.eventitta.auth.service.dto;

import com.eventitta.user.domain.Provider;
import com.eventitta.user.domain.Role;
import com.eventitta.user.domain.User;
import org.springframework.security.crypto.password.PasswordEncoder;

public record SignUpCommand(
    String email,
    String password,
    String nickname
) {
    public User toEntity(PasswordEncoder encoder) {
        return User.builder()
            .email(email)
            .password(encoder.encode(password))
            .nickname(nickname)
            .role(Role.USER)
            .provider(Provider.LOCAL)
            .build();
    }
}
