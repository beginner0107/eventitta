package com.eventitta.auth.constants;

public final class AuthConstants {

    // JWT 토큰 관련
    public static final String ACCESS_TOKEN = "access_token";
    public static final String REFRESH_TOKEN = "refresh_token";
    public static final String CLAIM_EMAIL = "email";
    public static final String CLAIM_ROLE = "role";

    // HTTP 인증 관련
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";

    // 사용자 관련
    public static final String ANONYMOUS_USER = "anonymousUser";
    public static final String ANONYMOUS = "anonymous";
    public static final String USER_INFO_FORMAT = "%s (ID: %d)";
    public static final String UNKNOWN_USER_FORMAT = "Unknown User (ID: %d)";
    
    // JWT 클레임 관련
    public static final String JWT_SUBJECT_CLAIM = "sub";
    
    // JWT 구조 관련
    public static final int JWT_PARTS_COUNT = 3;
    
    // Base64 패딩 관련
    public static final int BASE64_PADDING_BLOCK_SIZE = 4;
    public static final String BASE64_PADDING_CHAR = "=";
}
