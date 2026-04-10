package com.eventitta.api.auth.jwt;

import com.eventitta.api.auth.jwt.service.UserInfoService;
import com.eventitta.domain.common.exception.CommonErrorCode;
import com.eventitta.api.common.response.ApiErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;
    private final UserInfoService userInfoService;

    @Override
    public void handle(
        HttpServletRequest request,
        HttpServletResponse response,
        AccessDeniedException accessDeniedException
    ) throws IOException {
        log.warn(
            "[Exception] Access denied errorCode={}, status={}, path={}, userInfo={}, exceptionType={}, message={}",
            CommonErrorCode.FORBIDDEN.name(),
            HttpStatus.FORBIDDEN.value(),
            request.getRequestURI(),
            resolveUserInfoSafely(request),
            accessDeniedException.getClass().getName(),
            accessDeniedException.getMessage()
        );
        writeErrorResponse(request, response);
    }

    private void writeErrorResponse(HttpServletRequest request, HttpServletResponse response) throws IOException {
        ApiErrorResponse body = ApiErrorResponse.of(
            CommonErrorCode.FORBIDDEN.name(),
            CommonErrorCode.FORBIDDEN.defaultMessage(),
            HttpStatus.FORBIDDEN.value(),
            request.getRequestURI()
        );
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }

    private String resolveUserInfoSafely(HttpServletRequest request) {
        try {
            return userInfoService.extractUserInfoFromRequest(request);
        } catch (Exception ex) {
            log.warn("요청 기반 사용자 정보 추출 실패. path={}", request.getRequestURI(), ex);
            return "unknown";
        }
    }
}
