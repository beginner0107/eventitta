package com.eventitta.domain.auth.dto;

public record TokenResult(
    String accessToken,
    String refreshToken
) {
}
