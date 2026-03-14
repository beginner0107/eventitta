package com.eventitta.common.exception;

import com.eventitta.auth.exception.AuthErrorCode;
import com.eventitta.notification.domain.AlertLevel;
import com.eventitta.notification.resolver.AlertLevelResolver;
import com.eventitta.notification.service.DiscordNotificationService;
import com.eventitta.common.response.ApiErrorResponse;
import com.eventitta.auth.jwt.service.UserInfoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
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

import java.sql.SQLException;

@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GlobalExceptionHandler {

    private final HttpServletRequest request;
    private final DiscordNotificationService discordNotificationService;
    private final AlertLevelResolver alertLevelResolver;
    private final UserInfoService userInfoService;

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiErrorResponse> handleCustom(CustomException ex) {
        ResponseEntity<ApiErrorResponse> response = toResponse(ex.getErrorCode());
        logException("Custom exception", ex.getErrorCode(), ex, resolveCurrentUserInfoSafely());
        sendDiscordNotification(ex, ex.getErrorCode().name(), ex.getErrorCode().defaultMessage());
        return response;
    }

    @ExceptionHandler({
        InternalAuthenticationServiceException.class,
        BadCredentialsException.class
    })
    public ResponseEntity<ApiErrorResponse> handleBadCredentials(Exception ex) {
        logException("Authentication failure", AuthErrorCode.INVALID_CREDENTIALS, ex, resolveUserInfoFromRequestSafely());
        return toResponse(AuthErrorCode.INVALID_CREDENTIALS);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthenticationFailure(AuthenticationException ex) {
        logException("Authentication failure", AuthErrorCode.ACCESS_TOKEN_INVALID, ex, resolveUserInfoFromRequestSafely());
        sendDiscordNotification(ex, AuthErrorCode.ACCESS_TOKEN_INVALID.name(), AuthErrorCode.ACCESS_TOKEN_INVALID.defaultMessage());
        return toResponse(AuthErrorCode.ACCESS_TOKEN_INVALID);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(NoResourceFoundException ex) {
        logException("Resource not found", CommonErrorCode.RESOURCE_NOT_FOUND, ex, resolveCurrentUserInfoSafely());
        return toResponse(CommonErrorCode.RESOURCE_NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
            .findFirst()
            .map(err -> err.getField() + ": " + err.getDefaultMessage())
            .orElse(CommonErrorCode.INVALID_INPUT.defaultMessage());

        logException("Validation failure", CommonErrorCode.INVALID_INPUT, ex, resolveCurrentUserInfoSafely(), message);
        return toResponse(CommonErrorCode.INVALID_INPUT, message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraint(ConstraintViolationException ex) {
        logException("Constraint violation", CommonErrorCode.INVALID_CONSTRAINT, ex, resolveCurrentUserInfoSafely());
        return toResponse(CommonErrorCode.INVALID_CONSTRAINT, ex.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleBadRequest(HttpMessageNotReadableException ex) {
        logException("Malformed JSON request", CommonErrorCode.INVALID_JSON, ex, resolveCurrentUserInfoSafely());
        return toResponse(CommonErrorCode.INVALID_JSON);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingParam(MissingServletRequestParameterException ex) {
        String message = ex.getParameterName() + " 파라미터가 필요합니다.";
        logException("Missing request parameter", CommonErrorCode.MISSING_PARAMETER, ex, resolveCurrentUserInfoSafely(), message);
        return toResponse(CommonErrorCode.MISSING_PARAMETER, message);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex) {
        logException("Method not allowed", CommonErrorCode.METHOD_NOT_ALLOWED, ex, resolveCurrentUserInfoSafely());
        return toResponse(CommonErrorCode.METHOD_NOT_ALLOWED);
    }

    @ExceptionHandler(PessimisticLockingFailureException.class)
    public ResponseEntity<ApiErrorResponse> handleLockTimeout(PessimisticLockingFailureException ex) {
        logException("Lock timeout", CommonErrorCode.LOCK_TIMEOUT, ex, resolveCurrentUserInfoSafely());
        return toResponse(CommonErrorCode.LOCK_TIMEOUT);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        logDataIntegrityViolation(ex);
        sendHighPriorityNotification(ex, CommonErrorCode.INTERNAL_ERROR.name(), CommonErrorCode.INTERNAL_ERROR.defaultMessage());
        return toResponse(CommonErrorCode.INTERNAL_ERROR);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnknown(Exception ex) {
        logException("Internal server error", CommonErrorCode.INTERNAL_ERROR, ex, resolveCurrentUserInfoSafely());
        sendHighPriorityNotification(ex, CommonErrorCode.INTERNAL_ERROR.name(), CommonErrorCode.INTERNAL_ERROR.defaultMessage());
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

    private void sendDiscordNotification(Exception exception, String errorCode, String message) {
        sendDiscordNotificationSafely(exception, errorCode, message, false);
    }

    private void sendHighPriorityNotification(Exception exception, String errorCode, String message) {
        sendDiscordNotificationSafely(exception, errorCode, message, true);
    }

    private void logDataIntegrityViolation(DataIntegrityViolationException ex) {
        org.hibernate.exception.ConstraintViolationException constraintViolation =
            findCause(ex, org.hibernate.exception.ConstraintViolationException.class);
        SQLException sqlException = findCause(ex, SQLException.class);
        Throwable mostSpecificCause = ex.getMostSpecificCause();
        Throwable rootCause = ex.getRootCause();
        String userInfo = resolveCurrentUserInfoSafely();

        log.error(
            "[Exception] Data integrity violation errorCode={}, status={}, path={}, userInfo={}, exceptionType={}, constraintName={}, sqlState={}, vendorCode={}, rootCauseType={}, rootCauseMessage={}, mostSpecificCauseType={}, mostSpecificCauseMessage={}",
            CommonErrorCode.INTERNAL_ERROR.name(),
            CommonErrorCode.INTERNAL_ERROR.defaultHttpStatus().value(),
            request.getRequestURI(),
            userInfo,
            ex.getClass().getName(),
            constraintViolation != null ? constraintViolation.getConstraintName() : "unknown",
            sqlException != null ? sqlException.getSQLState() : "unknown",
            sqlException != null ? sqlException.getErrorCode() : "unknown",
            rootCause != null ? rootCause.getClass().getName() : ex.getClass().getName(),
            rootCause != null ? rootCause.getMessage() : ex.getMessage(),
            mostSpecificCause.getClass().getName(),
            mostSpecificCause.getMessage(),
            ex
        );
    }

    private <T extends Throwable> T findCause(Throwable throwable, Class<T> type) {
        Throwable current = throwable;
        while (current != null) {
            if (type.isInstance(current)) {
                return type.cast(current);
            }
            current = current.getCause();
        }
        return null;
    }

    private void sendDiscordNotificationSafely(Exception exception, String errorCode, String message, boolean forceHighLevel) {
        try {
            AlertLevel level = forceHighLevel ? AlertLevel.HIGH : alertLevelResolver.resolveLevel(exception);
            if (level.ordinal() < AlertLevel.HIGH.ordinal()) {
                return;
            }
            String userInfo = userInfoService.getCurrentUserInfo();

            discordNotificationService.sendAlert(
                level,
                errorCode,
                message,
                request.getRequestURI(),
                userInfo,
                exception
            );
        } catch (Exception notificationException) {
            log.error(
                "예외 알림 전송 실패. originalExceptionType={}, notificationExceptionType={}, path={}",
                exception.getClass().getName(),
                notificationException.getClass().getName(),
                request.getRequestURI(),
                notificationException
            );
        }
    }

    private void logException(String category, ErrorCode code, Exception exception, String userInfo) {
        logException(category, code, exception, userInfo, exception.getMessage());
    }

    private void logException(String category, ErrorCode code, Exception exception, String userInfo, String message) {
        Throwable rootCause = rootCauseOf(exception);
        HttpStatus status = code.defaultHttpStatus();
        String logMessage =
            "[Exception] {} errorCode={}, status={}, path={}, userInfo={}, exceptionType={}, rootCauseType={}, rootCauseMessage={}, message={}";

        if (status.is5xxServerError()) {
            log.error(
                logMessage,
                category,
                code.name(),
                status.value(),
                request.getRequestURI(),
                userInfo,
                exception.getClass().getName(),
                rootCause.getClass().getName(),
                rootCause.getMessage(),
                message,
                exception
            );
            return;
        }

        if (status == HttpStatus.NOT_FOUND) {
            log.info(
                logMessage,
                category,
                code.name(),
                status.value(),
                request.getRequestURI(),
                userInfo,
                exception.getClass().getName(),
                rootCause.getClass().getName(),
                rootCause.getMessage(),
                message
            );
            return;
        }

        log.warn(
            logMessage,
            category,
            code.name(),
            status.value(),
            request.getRequestURI(),
            userInfo,
            exception.getClass().getName(),
            rootCause.getClass().getName(),
            rootCause.getMessage(),
            message
        );
    }

    private Throwable rootCauseOf(Throwable throwable) {
        Throwable rootCause = throwable;
        while (rootCause.getCause() != null) {
            rootCause = rootCause.getCause();
        }
        return rootCause;
    }

    private String resolveCurrentUserInfoSafely() {
        try {
            return userInfoService.getCurrentUserInfo();
        } catch (Exception ex) {
            log.warn("현재 사용자 정보 추출 실패. path={}", request.getRequestURI(), ex);
            return "unknown";
        }
    }

    private String resolveUserInfoFromRequestSafely() {
        try {
            return userInfoService.extractUserInfoFromRequest(request);
        } catch (Exception ex) {
            log.warn("요청 기반 사용자 정보 추출 실패. path={}", request.getRequestURI(), ex);
            return "unknown";
        }
    }
}
