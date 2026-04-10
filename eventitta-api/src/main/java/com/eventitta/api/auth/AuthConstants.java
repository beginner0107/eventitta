package com.eventitta.api.auth;

public final class AuthConstants {

    // JWT 토큰 관련
    public static final String ACCESS_TOKEN = "access_token";
    public static final String REFRESH_TOKEN = "refresh_token";
    public static final String OAUTH_STATE = "oauth_state";
    public static final String CLAIM_EMAIL = "email";
    public static final String CLAIM_ROLE = "role";
    public static final String CLAIM_AUTH_VERSION = "authVersion";
    public static final String CLAIM_SESSION_ID = "sessionId";

    // HTTP 인증 관련
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";
}
