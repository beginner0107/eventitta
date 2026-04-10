package com.eventitta.domain.auth.dto;

public record KakaoLoginCommand(
    String code,
    String redirectUri,
    ClientSessionMetadata sessionMetadata
) {
    public KakaoLoginCommand(String code, String redirectUri) {
        this(code, redirectUri, null);
    }
}
