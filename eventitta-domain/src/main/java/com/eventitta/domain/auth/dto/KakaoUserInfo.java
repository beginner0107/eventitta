package com.eventitta.domain.auth.dto;

public record KakaoUserInfo(
    String providerUserId,
    String email,
    boolean emailVerified,
    String nickname,
    String profileImageUrl
) {
}
