package com.eventitta.infra.auth.client;

import com.eventitta.infra.auth.client.dto.KakaoTokenResponse;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.PostExchange;

public interface KakaoTokenHttpApi {

    @PostExchange(url = "/oauth/token", contentType = "application/x-www-form-urlencoded;charset=utf-8")
    KakaoTokenResponse exchangeAuthorizationCode(
        @RequestParam("grant_type") String grantType,
        @RequestParam("client_id") String clientId,
        @RequestParam("redirect_uri") String redirectUri,
        @RequestParam("code") String code,
        @RequestParam("client_secret") String clientSecret
    );
}
