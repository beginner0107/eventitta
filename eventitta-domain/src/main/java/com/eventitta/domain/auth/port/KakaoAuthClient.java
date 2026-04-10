package com.eventitta.domain.auth.port;

import com.eventitta.domain.auth.port.dto.KakaoUserInfo;

public interface KakaoAuthClient {

    String exchangeAuthorizationCode(String code, String redirectUri);

    KakaoUserInfo getUserInfo(String accessToken);
}
