package com.eventitta.api.auth.cookie;

import com.eventitta.api.auth.cookie.CookieProperties;
import com.eventitta.api.auth.oauth.kakao.KakaoWebProperties;
import com.eventitta.domain.auth.port.AuthTokenProvider;
import com.eventitta.domain.auth.dto.TokenResult;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

import static com.eventitta.api.auth.AuthConstants.ACCESS_TOKEN;
import static com.eventitta.api.auth.AuthConstants.REFRESH_TOKEN;

@Component
public class CookieManager {

    private static final String SAME_SITE_STRICT = "Strict";
    private static final String ROOT_PATH = "/";
    private static final String AUTH_PATH = "/api/v1/auth";
    private static final String KAKAO_AUTH_PATH = "/api/v1/auth/social/kakao";

    private final AuthTokenProvider authTokenProvider;
    private final CookieProperties cookieProperties;
    private final KakaoWebProperties kakaoWebProperties;

    public CookieManager(
        AuthTokenProvider authTokenProvider,
        CookieProperties cookieProperties,
        KakaoWebProperties kakaoWebProperties
    ) {
        this.authTokenProvider = authTokenProvider;
        this.cookieProperties = cookieProperties;
        this.kakaoWebProperties = kakaoWebProperties;
    }

    public void addTokenCookies(HttpServletResponse response, TokenResult tokens) {
        response.addHeader(HttpHeaders.SET_COOKIE, createAccessTokenCookie(tokens.accessToken()).toString());
        response.addHeader(HttpHeaders.SET_COOKIE, createRefreshTokenCookie(tokens.refreshToken()).toString());
    }

    public void addShortLivedCookie(HttpServletResponse response, String name, String value) {
        ResponseCookie cookie = createCookie(
            name,
            value,
            Duration.ofSeconds(kakaoWebProperties.getStateCookieMaxAgeSeconds()),
            pathFor(name)
        );
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public void deleteCookie(HttpServletResponse response, String name) {
        response.addHeader(HttpHeaders.SET_COOKIE, expiredCookie(name, pathFor(name)).toString());
    }

    public void deleteTokenCookies(HttpServletResponse response) {
        deleteCookie(response, ACCESS_TOKEN);
        deleteCookie(response, REFRESH_TOKEN);
    }

    public void deleteAccessTokenCookie(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, expiredCookie(ACCESS_TOKEN, ROOT_PATH).toString());
    }

    public void deleteRefreshTokenCookie(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, expiredCookie(REFRESH_TOKEN, AUTH_PATH).toString());
    }

    private ResponseCookie expiredCookie(String name, String path) {
        ResponseCookie cookie = ResponseCookie.from(name, "")
            .httpOnly(true)
            .secure(cookieProperties.isSecure())
            .path(path)
            .sameSite(SAME_SITE_STRICT)
            .maxAge(0)
            .build();
        return cookie;
    }

    private ResponseCookie createAccessTokenCookie(String value) {
        return createCookie(ACCESS_TOKEN, value, Duration.ofMillis(authTokenProvider.getAccessTokenValidityMs()), ROOT_PATH);
    }

    private ResponseCookie createRefreshTokenCookie(String value) {
        return createCookie(REFRESH_TOKEN, value, Duration.ofMillis(authTokenProvider.getRefreshTokenValidityMs()), AUTH_PATH);
    }

    private ResponseCookie createCookie(String name, String value, Duration maxAge, String path) {
        return ResponseCookie.from(name, value)
            .httpOnly(true)
            .secure(cookieProperties.isSecure())
            .path(path)
            .sameSite(SAME_SITE_STRICT)
            .maxAge(maxAge)
            .build();
    }

    private String pathFor(String name) {
        return switch (name) {
            case ACCESS_TOKEN -> ROOT_PATH;
            case REFRESH_TOKEN -> AUTH_PATH;
            case com.eventitta.api.auth.AuthConstants.OAUTH_STATE -> KAKAO_AUTH_PATH;
            default -> ROOT_PATH;
        };
    }
}
