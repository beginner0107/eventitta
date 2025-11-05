package com.eventitta.common.exception;

import com.eventitta.auth.exception.AuthErrorCode;
import com.eventitta.notification.domain.AlertLevel;
import com.eventitta.notification.resolver.AlertLevelResolver;
import com.eventitta.notification.service.SlackNotificationService;
import com.eventitta.common.response.ApiErrorResponse;
import com.eventitta.auth.jwt.service.UserInfoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final HttpServletRequest request;
    private final SlackNotificationService slackNotificationService;
    private final AlertLevelResolver alertLevelResolver;
    private final UserInfoService userInfoService;

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiErrorResponse> handleCustom(CustomException ex) {
        ResponseEntity<ApiErrorResponse> response = toResponse(ex.getErrorCode());
        sendSlackNotification(ex, ex.getErrorCode().name(), ex.getErrorCode().defaultMessage());
        return response;
    }

    @ExceptionHandler({
        InternalAuthenticationServiceException.class,
        BadCredentialsException.class
    })
    public ResponseEntity<ApiErrorResponse> handleBadCredentials(Exception ex) {
        return toResponse(AuthErrorCode.INVALID_CREDENTIALS);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthenticationFailure(AuthenticationException ex) {
        sendAuthenticationFailureNotification(ex);
        return toResponse(AuthErrorCode.ACCESS_TOKEN_INVALID);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(NoResourceFoundException ex) {
        return toResponse(CommonErrorCode.RESOURCE_NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
            .findFirst()
            .map(err -> err.getField() + ": " + err.getDefaultMessage())
            .orElse(CommonErrorCode.INVALID_INPUT.defaultMessage());

        return toResponse(CommonErrorCode.INVALID_INPUT, message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraint(ConstraintViolationException ex) {
        return toResponse(CommonErrorCode.INVALID_CONSTRAINT, ex.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleBadRequest(HttpMessageNotReadableException ex) {
        return toResponse(CommonErrorCode.INVALID_JSON);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingParam(MissingServletRequestParameterException ex) {
        String message = ex.getParameterName() + " 파라미터가 필요합니다.";
        return toResponse(CommonErrorCode.MISSING_PARAMETER, message);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex) {
        return toResponse(CommonErrorCode.METHOD_NOT_ALLOWED);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnknown(Exception ex) {
        AlertLevel level = alertLevelResolver.resolveLevel(ex);
        if (level.ordinal() >= AlertLevel.HIGH.ordinal()) {
            sendSlackNotification(ex,
                CommonErrorCode.INTERNAL_ERROR.name(),
                CommonErrorCode.INTERNAL_ERROR.defaultMessage());
        }
        return toResponse(CommonErrorCode.INTERNAL_ERROR);
    }

    private ResponseEntity<ApiErrorResponse> toResponse(ErrorCode code) {
        return ResponseEntity.status(code.defaultHttpStatus())
            .body(ApiErrorResponse.of(code.name(), code.defaultMessage(), code.defaultHttpStatus().value(), request.getRequestURI()));
    }

    private ResponseEntity<ApiErrorResponse> toResponse(ErrorCode code, String overrideMessage) {
        return ResponseEntity.status(code.defaultHttpStatus())
            .body(ApiErrorResponse.of(code.name(), overrideMessage, code.defaultHttpStatus().value(), request.getRequestURI()));
    }

    private void sendSlackNotification(Exception exception, String errorCode, String message) {
        AlertLevel level = alertLevelResolver.resolveLevel(exception);
        String userInfo = userInfoService.getCurrentUserInfo();

        slackNotificationService.sendAlert(
            level,
            errorCode,
            message,
            request.getRequestURI(),
            userInfo,
            exception
        );
    }

    private void sendAuthenticationFailureNotification(AuthenticationException authException) {
        try {
            String userInfo = userInfoService.extractUserInfoFromRequest(request);
            slackNotificationService.sendAlert(
                AlertLevel.HIGH,
                AuthErrorCode.ACCESS_TOKEN_INVALID.name(),
                AuthErrorCode.ACCESS_TOKEN_INVALID.defaultMessage(),
                request.getRequestURI(),
                userInfo,
                authException
            );
        } catch (Exception e) {
            // 슬랙 알림 실패는 로그만 남기고 원래 예외 처리 진행
        }
    }
}
