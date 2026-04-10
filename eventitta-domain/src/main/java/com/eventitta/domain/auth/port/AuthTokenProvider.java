package com.eventitta.domain.auth.port;

import java.time.Instant;

public interface AuthTokenProvider {

    String createAccessToken(Long userId, String email, String role, long authVersion, String sessionId);

    String createRefreshTokenKey();

    String createRefreshTokenSecret();

    Instant getRefreshTokenExpiry();

    long getAccessTokenValidityMs();

    long getRefreshTokenValidityMs();

    Long getUserId(String token);

    Long getUserIdFromExpiredToken(String token);

    void validateAccessToken(String token);

    String getEmail(String token);

    String getRole(String token);
}
