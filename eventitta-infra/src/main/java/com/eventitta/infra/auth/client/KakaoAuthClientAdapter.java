package com.eventitta.infra.auth.client;

import com.eventitta.domain.auth.exception.AuthErrorCode;
import com.eventitta.domain.auth.port.KakaoAuthClient;
import com.eventitta.domain.auth.port.dto.KakaoUserInfo;
import com.eventitta.infra.auth.client.dto.KakaoTokenResponse;
import com.eventitta.infra.auth.client.dto.KakaoUserResponse;
import com.eventitta.infra.auth.config.KakaoAuthProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import static org.springframework.util.StringUtils.hasText;

@Component
@RequiredArgsConstructor
public class KakaoAuthClientAdapter implements KakaoAuthClient {
    private static final String PROPERTY_KEYS = "[\"kakao_account.email\",\"kakao_account.profile\"]";

    private final KakaoTokenHttpApi kakaoTokenHttpApi;
    private final KakaoUserHttpApi kakaoUserHttpApi;
    private final KakaoAuthProperties properties;

    @Override
    public String exchangeAuthorizationCode(String code, String redirectUri) {
        try {
            KakaoTokenResponse response = kakaoTokenHttpApi.exchangeAuthorizationCode(
                "authorization_code",
                properties.getClientId(),
                redirectUri,
                code,
                properties.getClientSecret()
            );
            if (response == null || !hasText(response.accessToken())) {
                throw AuthErrorCode.SOCIAL_AUTH_FAILED.defaultException();
            }
            return response.accessToken();
        } catch (RestClientException ex) {
            throw AuthErrorCode.SOCIAL_AUTH_FAILED.defaultException(ex);
        }
    }

    @Override
    public KakaoUserInfo getUserInfo(String accessToken) {
        try {
            KakaoUserResponse response = kakaoUserHttpApi.getUserInfo(
                "Bearer " + accessToken,
                true,
                PROPERTY_KEYS
            );

            KakaoUserResponse.KakaoAccount account = response != null ? response.kakaoAccount() : null;
            KakaoUserResponse.Profile profile = account != null ? account.profile() : null;

            return new KakaoUserInfo(
                response != null && response.id() != null ? String.valueOf(response.id()) : null,
                account != null ? account.email() : null,
                account != null && Boolean.TRUE.equals(account.isEmailVerified()),
                profile != null ? profile.nickname() : null,
                profile != null ? profile.profileImageUrl() : null
            );
        } catch (RestClientException ex) {
            throw AuthErrorCode.SOCIAL_AUTH_FAILED.defaultException(ex);
        }
    }
}
