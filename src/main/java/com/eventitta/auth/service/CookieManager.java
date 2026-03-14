package com.eventitta.auth.service;

import com.eventitta.auth.jwt.JwtTokenProvider;
import com.eventitta.auth.service.dto.TokenResult;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

import static com.eventitta.auth.constants.AuthConstants.ACCESS_TOKEN;
import static com.eventitta.auth.constants.AuthConstants.REFRESH_TOKEN;

@Component
public class CookieManager {

    private static final String SAME_SITE_STRICT = "Strict";

    private final JwtTokenProvider tokenProvider;

    public CookieManager(JwtTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    public void addTokenCookies(HttpServletResponse resp, TokenResult tokens) {
        resp.addHeader(
            HttpHeaders.SET_COOKIE,
            createCookie(ACCESS_TOKEN, tokens.accessToken(), tokenProvider.getAccessTokenValidityMs())
                .toString()
        );
        resp.addHeader(
            HttpHeaders.SET_COOKIE,
            createCookie(REFRESH_TOKEN, tokens.refreshToken(), tokenProvider.getRefreshTokenValidityMs())
                .toString()
        );
    }

    public void deleteCookie(HttpServletResponse resp, String name) {
        ResponseCookie c = ResponseCookie.from(name, "")
            .httpOnly(true)
            .path("/")
            .maxAge(0)
            .build();
        resp.addHeader(HttpHeaders.SET_COOKIE, c.toString());
    }

    private ResponseCookie createCookie(String name, String value, long validityMs) {
        return ResponseCookie.from(name, value)
            .httpOnly(true)
            .path("/")
            .sameSite(SAME_SITE_STRICT)
            .maxAge(Duration.ofMillis(validityMs))
            .build();
    }
}
