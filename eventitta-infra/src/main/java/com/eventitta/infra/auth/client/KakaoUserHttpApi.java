package com.eventitta.infra.auth.client;

import com.eventitta.infra.auth.client.dto.KakaoUserResponse;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;

public interface KakaoUserHttpApi {

    @GetExchange("/v2/user/me")
    KakaoUserResponse getUserInfo(
        @RequestHeader("Authorization") String authorization,
        @RequestParam("secure_resource") boolean secureResource,
        @RequestParam("property_keys") String propertyKeys
    );
}
