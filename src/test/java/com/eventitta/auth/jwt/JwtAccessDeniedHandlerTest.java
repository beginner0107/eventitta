package com.eventitta.auth.jwt;

import com.eventitta.auth.jwt.service.UserInfoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class JwtAccessDeniedHandlerTest {

    private final UserInfoService userInfoService = mock(UserInfoService.class);
    private final JwtAccessDeniedHandler accessDeniedHandler = new JwtAccessDeniedHandler(new ObjectMapper(), userInfoService);

    @Test
    @DisplayName("접근 거부 예외가 발생하면 403 JSON 응답을 반환한다")
    void handle_returnsForbiddenJsonResponse() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/admin/festivals/sync");
        MockHttpServletResponse response = new MockHttpServletResponse();
        given(userInfoService.extractUserInfoFromRequest(request)).willReturn("anonymous");

        accessDeniedHandler.handle(request, response, new AccessDeniedException("forbidden"));

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentType()).startsWith("application/json");
        assertThat(response.getContentAsString()).contains("\"error\":\"FORBIDDEN\"");
        assertThat(response.getContentAsString()).contains("\"message\":\"해당 리소스에 대한 접근 권한이 없습니다.\"");
        assertThat(response.getContentAsString()).contains("\"path\":\"/api/v1/admin/festivals/sync\"");
    }

    @Test
    @DisplayName("사용자 정보 추출에 실패해도 403 JSON 응답은 유지한다")
    void handle_returnsForbiddenJsonResponse_whenUserInfoResolutionFails() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/admin/festivals/sync");
        MockHttpServletResponse response = new MockHttpServletResponse();
        given(userInfoService.extractUserInfoFromRequest(request)).willThrow(new IllegalStateException("boom"));

        accessDeniedHandler.handle(request, response, new AccessDeniedException("forbidden"));

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentType()).startsWith("application/json");
        assertThat(response.getContentAsString()).contains("\"error\":\"FORBIDDEN\"");
        assertThat(response.getContentAsString()).contains("\"path\":\"/api/v1/admin/festivals/sync\"");
    }
}
