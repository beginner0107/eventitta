package com.eventitta.api.auth.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "auth.social.kakao")
public class KakaoWebProperties {
    private String clientId;
    private String authorizeBaseUrl = "https://kauth.kakao.com/oauth/authorize";
    private List<String> allowedRedirectUris = new ArrayList<>();
    private List<String> scopes = new ArrayList<>(List.of(
        "account_email",
        "profile_nickname",
        "profile_image"
    ));
    private long stateCookieMaxAgeSeconds = 300;
}
