package com.eventitta.auth.jwt;

import com.eventitta.auth.exception.AuthErrorCode;
import com.eventitta.common.response.ApiErrorResponse;
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

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException {
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
}
