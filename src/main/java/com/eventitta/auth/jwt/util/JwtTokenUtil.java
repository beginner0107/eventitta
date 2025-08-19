package com.eventitta.auth.jwt.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Arrays;
import java.util.Optional;

import static com.eventitta.auth.constants.AuthConstants.ACCESS_TOKEN;
import static com.eventitta.auth.constants.AuthConstants.AUTHORIZATION_HEADER;
import static com.eventitta.auth.constants.AuthConstants.BEARER_PREFIX;

public final class JwtTokenUtil {

    public static String extractTokenFromRequest(HttpServletRequest request) {
        String tokenFromHeader = extractTokenFromHeader(request);
        if (tokenFromHeader != null) {
            return tokenFromHeader;
        }
        return extractTokenFromCookies(request);
    }

    public static String extractTokenFromHeader(HttpServletRequest request) {
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            return null;
        }
        return authHeader.substring(BEARER_PREFIX.length());
    }

    public static String extractTokenFromCookies(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        return Arrays.stream(Optional.ofNullable(cookies).orElse(new Cookie[0]))
            .filter(c -> ACCESS_TOKEN.equals(c.getName()))
            .map(Cookie::getValue)
            .findFirst()
            .orElse(null);
    }
}
