package com.eventitta.domain.auth.service.dto;

public record KakaoLinkCommand(
    String code,
    String redirectUri
) {
}
