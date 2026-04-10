package com.eventitta.infra.auth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "auth.social.kakao")
public class KakaoAuthProperties {
    private String clientId;
    private String clientSecret;
    private String tokenBaseUrl = "https://kauth.kakao.com";
    private String apiBaseUrl = "https://kapi.kakao.com";
}
