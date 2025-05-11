package com.eventitta.common.exception;

import com.eventitta.auth.exception.AuthErrorCode;
import com.eventitta.auth.exception.AuthException;
import com.eventitta.common.response.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
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

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiErrorResponse> handleCustom(CustomException ex) {
        return toResponse(ex.getErrorCode());
    }

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ApiErrorResponse> handleAuth(AuthException ex) {
        return toResponse(ex.getErrorCode());
    }

    @ExceptionHandler({
        InternalAuthenticationServiceException.class,
        BadCredentialsException.class
    })
    public ResponseEntity<ApiErrorResponse> handleBadCredentials(Exception ex) {
        return toResponse(AuthErrorCode.INVALID_CREDENTIALS);
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
}
