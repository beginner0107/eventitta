package com.eventitta.common.util;

import com.eventitta.auth.dto.response.TokenResponse;
import com.eventitta.auth.jwt.JwtTokenProvider;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

import java.time.Duration;

public final class CookieUtil {
    private CookieUtil() {
    }

    public static ResponseCookie createAccessTokenCookie(
        String accessToken, long validityMs) {
        return ResponseCookie.from("access_token", accessToken)
            .httpOnly(true)
            .path("/")
            .sameSite("Strict")
            .maxAge(Duration.ofMillis(validityMs))
            .build();
    }

    public static ResponseCookie createRefreshTokenCookie(
        String refreshToken, long validityMs) {
        return ResponseCookie.from("refresh_token", refreshToken)
            .httpOnly(true)
            .path("/")
            .sameSite("Strict")
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
}
