package com.eventitta.domain.auth.service.dto;

public record LogoutCommand(
    String accessToken,
    String refreshToken
) {
}
