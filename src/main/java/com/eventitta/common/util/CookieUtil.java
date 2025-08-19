package com.eventitta.common.util;

import com.eventitta.auth.dto.response.TokenResponse;
import com.eventitta.auth.jwt.JwtTokenProvider;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

import java.time.Duration;

import static com.eventitta.auth.constants.AuthConstants.ACCESS_TOKEN;
import static com.eventitta.auth.constants.AuthConstants.REFRESH_TOKEN;

public final class CookieUtil {

    private final static String SAME_SITE_STRICT = "Strict";

    private CookieUtil() {
    }

    public static ResponseCookie createAccessTokenCookie(
        String accessToken, long validityMs) {
        return ResponseCookie.from(ACCESS_TOKEN, accessToken)
            .httpOnly(true)
            .path("/")
            .sameSite(SAME_SITE_STRICT)
            .maxAge(Duration.ofMillis(validityMs))
            .build();
    }

    public static ResponseCookie createRefreshTokenCookie(
        String refreshToken, long validityMs) {
        return ResponseCookie.from(REFRESH_TOKEN, refreshToken)
            .httpOnly(true)
            .path("/")
            .sameSite(SAME_SITE_STRICT)
            .maxAge(Duration.ofMillis(validityMs))
            .build();
    }

    public static void addTokenCookies(
        HttpServletResponse resp,
        TokenResponse tokens,
        JwtTokenProvider prov) {

        resp.addHeader(
            HttpHeaders.SET_COOKIE,
            createAccessTokenCookie(tokens.accessToken(), prov.getAccessTokenValidityMs())
                .toString()
        );
        resp.addHeader(
            HttpHeaders.SET_COOKIE,
            createRefreshTokenCookie(tokens.refreshToken(), prov.getRefreshTokenValidityMs())
                .toString()
        );
    }

    public static void deleteCookie(HttpServletResponse resp, String name) {
        ResponseCookie c = ResponseCookie.from(name, "")
            .httpOnly(true)
            .path("/")
            .maxAge(0)
            .build();
        resp.addHeader(HttpHeaders.SET_COOKIE, c.toString());
    }
}
