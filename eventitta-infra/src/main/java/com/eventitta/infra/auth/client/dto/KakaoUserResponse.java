package com.eventitta.infra.auth.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KakaoUserResponse(
    Long id,
    @JsonProperty("kakao_account")
    KakaoAccount kakaoAccount
) {
    public record KakaoAccount(
        @JsonProperty("is_email_verified")
        Boolean isEmailVerified,
        String email,
        Profile profile
    ) {
    }

    public record Profile(
        String nickname,
        @JsonProperty("profile_image_url")
        String profileImageUrl
    ) {
    }
}
