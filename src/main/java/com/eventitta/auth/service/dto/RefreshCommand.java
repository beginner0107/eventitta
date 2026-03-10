package com.eventitta.auth.service.dto;

public record RefreshCommand(
    String accessToken,
    String refreshToken
) {
}
