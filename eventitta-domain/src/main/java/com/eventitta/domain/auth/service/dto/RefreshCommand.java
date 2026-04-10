package com.eventitta.domain.auth.service.dto;

public record RefreshCommand(
    String accessToken,
    String refreshToken,
    ClientSessionMetadata sessionMetadata
) {
    public RefreshCommand(String accessToken, String refreshToken) {
        this(accessToken, refreshToken, null);
    }
}
