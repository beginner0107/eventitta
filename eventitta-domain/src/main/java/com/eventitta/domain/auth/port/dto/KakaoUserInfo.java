package com.eventitta.domain.auth.port.dto;

public record KakaoUserInfo(
    String providerUserId,
    String email,
    boolean emailVerified,
    String nickname,
    String profileImageUrl
) {
}
