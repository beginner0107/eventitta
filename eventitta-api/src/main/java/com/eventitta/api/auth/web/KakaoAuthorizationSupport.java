package com.eventitta.api.auth.web;

import com.eventitta.api.auth.properties.KakaoWebProperties;
import com.eventitta.domain.auth.exception.AuthErrorCode;
import com.eventitta.domain.common.exception.CommonErrorCode;
import org.springframework.security.crypto.keygen.KeyGenerators;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Base64;

@Component
public class KakaoAuthorizationSupport {
    private final KakaoWebProperties properties;

    public KakaoAuthorizationSupport(KakaoWebProperties properties) {
        this.properties = properties;
    }

    public String generateState() {
        return Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(KeyGenerators.secureRandom(32).generateKey());
    }

    public String buildAuthorizeUrl(String redirectUri, String state) {
        validateRedirectUri(redirectUri);

        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(properties.getAuthorizeBaseUrl())
            .queryParam("response_type", "code")
            .queryParam("client_id", properties.getClientId())
            .queryParam("redirect_uri", redirectUri)
            .queryParam("state", state);

        if (!properties.getScopes().isEmpty()) {
            builder.queryParam("scope", String.join(",", properties.getScopes()));
        }

        return builder.build(true).toUriString();
    }

    public void validateState(String storedState, String requestedState) {
        if (!StringUtils.hasText(storedState) || !storedState.equals(requestedState)) {
            throw AuthErrorCode.OAUTH_STATE_INVALID.defaultException();
        }
    }

    private void validateRedirectUri(String redirectUri) {
        if (!StringUtils.hasText(redirectUri) || !properties.getAllowedRedirectUris().contains(redirectUri)) {
            throw CommonErrorCode.INVALID_INPUT.defaultException();
        }
    }
}
