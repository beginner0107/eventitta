package com.eventitta.auth.service;

import com.eventitta.auth.jwt.JwtTokenProvider;
import com.eventitta.auth.properties.CookieProperties;
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
    private final CookieProperties cookieProperties;

    public CookieManager(JwtTokenProvider tokenProvider, CookieProperties cookieProperties) {
        this.tokenProvider = tokenProvider;
        this.cookieProperties = cookieProperties;
    }

    public void addTokenCookies(HttpServletResponse resp, TokenResult tokens) {
        resp.addHeader(
            HttpHeaders.SET_COOKIE,
            createAccessTokenCookie(tokens.accessToken())
                .toString()
        );
        resp.addHeader(
            HttpHeaders.SET_COOKIE,
            createRefreshTokenCookie(tokens.refreshToken())
                .toString()
        );
    }

    public void deleteCookie(HttpServletResponse resp, String name) {
        ResponseCookie c = ResponseCookie.from(name, "")
            .httpOnly(true)
            .secure(cookieProperties.isSecure())
            .path("/")
            .sameSite(SAME_SITE_STRICT)
            .maxAge(0)
            .build();
        resp.addHeader(HttpHeaders.SET_COOKIE, c.toString());
    }

    private ResponseCookie createAccessTokenCookie(String value) {
        return createCookie(ACCESS_TOKEN, value, tokenProvider.getRefreshTokenValidityMs());
    }

    private ResponseCookie createRefreshTokenCookie(String value) {
        return createCookie(REFRESH_TOKEN, value, tokenProvider.getRefreshTokenValidityMs());
    }

    private ResponseCookie createCookie(String name, String value, long validityMs) {
        return ResponseCookie.from(name, value)
            .httpOnly(true)
            .secure(cookieProperties.isSecure())
            .path("/")
            .sameSite(SAME_SITE_STRICT)
            .maxAge(Duration.ofMillis(validityMs))
            .build();
    }
}
