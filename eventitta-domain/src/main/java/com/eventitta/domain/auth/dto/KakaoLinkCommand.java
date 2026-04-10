package com.eventitta.domain.auth.dto;

public record KakaoLinkCommand(
    String code,
    String redirectUri
) {
}
