package com.eventitta.domain.auth.exception;

import com.eventitta.domain.common.exception.ErrorCode;
import com.eventitta.domain.common.exception.ErrorStatus;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {
    CONFLICTED_EMAIL("이미 사용 중인 이메일입니다.", ErrorStatus.CONFLICT),
    CONFLICTED_NICKNAME("이미 사용 중인 닉네임입니다.", ErrorStatus.CONFLICT),

    NOT_FOUND_USER_EMAIL("해당 이메일의 사용자를 찾을 수 없습니다", ErrorStatus.NOT_FOUND),

    ACCESS_TOKEN_INVALID("잘못된 액세스 토큰입니다.", ErrorStatus.UNAUTHORIZED),
    ACCESS_TOKEN_EXPIRED("액세스 토큰이 만료되었습니다.", ErrorStatus.UNAUTHORIZED),
    AUTH_SESSION_INVALIDATED("세션이 만료되었거나 재인증이 필요합니다.", ErrorStatus.UNAUTHORIZED),
    REFRESH_TOKEN_MISSING("리프레시 토큰이 없습니다.", ErrorStatus.BAD_REQUEST),
    REFRESH_TOKEN_INVALID("잘못된 리프레시 토큰입니다.", ErrorStatus.UNAUTHORIZED),
    REFRESH_TOKEN_EXPIRED("리프레시 토큰이 만료되었습니다. 다시 로그인하세요.", ErrorStatus.UNAUTHORIZED),
    AUTH_RATE_LIMITED("인증 시도가 너무 많습니다. 잠시 후 다시 시도해주세요.", ErrorStatus.TOO_MANY_REQUESTS),

    INVALID_CREDENTIALS("이메일 또는 비밀번호가 올바르지 않습니다.", ErrorStatus.UNAUTHORIZED),
    EMAIL_VERIFICATION_REQUIRED("이메일 인증이 필요합니다.", ErrorStatus.FORBIDDEN),
    ACCOUNT_SUSPENDED("정지된 계정입니다.", ErrorStatus.FORBIDDEN),
    OAUTH_STATE_INVALID("소셜 로그인 state 검증에 실패했습니다.", ErrorStatus.UNAUTHORIZED),
    SOCIAL_EMAIL_REQUIRED("소셜 계정의 이메일 정보가 필요합니다.", ErrorStatus.BAD_REQUEST),
    SOCIAL_EMAIL_NOT_VERIFIED("소셜 계정의 이메일 인증이 필요합니다.", ErrorStatus.BAD_REQUEST),
    SOCIAL_LINK_REQUIRED("기존 계정과 연결이 필요합니다.", ErrorStatus.CONFLICT),
    SOCIAL_ACCOUNT_ALREADY_LINKED("이미 연결된 소셜 계정입니다.", ErrorStatus.CONFLICT),
    SOCIAL_ACCOUNT_NOT_LINKED("연결된 소셜 계정을 찾을 수 없습니다.", ErrorStatus.NOT_FOUND),
    SOCIAL_UNLINK_NOT_ALLOWED("다른 로그인 수단이 없어 소셜 계정을 해제할 수 없습니다.", ErrorStatus.CONFLICT),
    SOCIAL_AUTH_FAILED("소셜 로그인 인증에 실패했습니다.", ErrorStatus.UNAUTHORIZED),
    LOCAL_PASSWORD_ALREADY_SET("이미 로컬 비밀번호가 설정된 계정입니다.", ErrorStatus.CONFLICT),
    ACTION_TOKEN_INVALID("잘못된 인증 토큰입니다.", ErrorStatus.BAD_REQUEST),
    ACTION_TOKEN_EXPIRED("인증 토큰이 만료되었습니다.", ErrorStatus.BAD_REQUEST),
    ACTION_TOKEN_USED("이미 사용된 인증 토큰입니다.", ErrorStatus.CONFLICT),
    NOT_FOUND_USER_ID("사용자를 찾을 수 없습니다.", ErrorStatus.NOT_FOUND),
    DEFAULT("예상치 못한 서버 오류가 발생했습니다", ErrorStatus.INTERNAL_SERVER_ERROR);

    private final String message;
    private final ErrorStatus status;

    @Override
    public String defaultMessage() {
        return message;
    }

    @Override
    public ErrorStatus defaultHttpStatus() {
        return status;
    }

    @Override
    public AuthException defaultException() {
        return new AuthException(this);
    }

    @Override
    public AuthException defaultException(Throwable cause) {
        return new AuthException(this, cause);
    }
}
