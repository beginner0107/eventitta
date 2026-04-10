package com.eventitta.api.auth.security.handler;

import com.eventitta.domain.auth.exception.AuthErrorCode;
import com.eventitta.api.common.logging.RequestActorResolver;
import com.eventitta.api.common.response.ApiErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;
    private final RequestActorResolver requestActorResolver;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException {
        log.warn(
            "[Exception] Authentication failure errorCode={}, status={}, path={}, userInfo={}, exceptionType={}, message={}",
            AuthErrorCode.ACCESS_TOKEN_INVALID.name(),
            HttpStatus.UNAUTHORIZED.value(),
            request.getRequestURI(),
            resolveUserInfoSafely(request),
            authException.getClass().getName(),
            authException.getMessage()
        );
        writeErrorResponse(request, response);
    }

    private void writeErrorResponse(HttpServletRequest request, HttpServletResponse response) throws IOException {
        ApiErrorResponse body = createErrorResponseBody(request);
        setResponseHeaders(response);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }

    private ApiErrorResponse createErrorResponseBody(HttpServletRequest request) {
        return ApiErrorResponse.of(
            AuthErrorCode.ACCESS_TOKEN_INVALID.name(),
            AuthErrorCode.ACCESS_TOKEN_INVALID.defaultMessage(),
            HttpStatus.UNAUTHORIZED.value(),
            request.getRequestURI()
        );
    }

    private void setResponseHeaders(HttpServletResponse response) {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
    }

    private String resolveUserInfoSafely(HttpServletRequest request) {
        try {
            return requestActorResolver.resolveActor(request);
        } catch (Exception ex) {
            log.warn("요청 기반 사용자 정보 추출 실패. path={}", request.getRequestURI(), ex);
            return "unknown";
        }
    }
}
