package com.eventitta.domain.auth.service.dto;

public record TokenResult(
    String accessToken,
    String refreshToken
) {
}
