package com.eventitta.api.auth.security.handler;

import com.eventitta.api.common.logging.RequestActorResolver;
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

    private final RequestActorResolver requestActorResolver = mock(RequestActorResolver.class);
    private final JwtAccessDeniedHandler accessDeniedHandler = new JwtAccessDeniedHandler(new ObjectMapper(), requestActorResolver);

    @Test
    @DisplayName("접근 거부 예외가 발생하면 403 JSON 응답을 반환한다")
    void handle_returnsForbiddenJsonResponse() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/admin/festivals/sync");
        MockHttpServletResponse response = new MockHttpServletResponse();
        given(requestActorResolver.resolveActor(request)).willReturn("anonymous");

        accessDeniedHandler.handle(request, response, new AccessDeniedException("forbidden"));

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("\"error\":\"FORBIDDEN\"");
    }
}
