package com.eventitta.domain.auth.service.dto;

public record SignUpCommand(
    String email,
    String password,
    String nickname
) {
}
