package com.eventitta;

import com.eventitta.auth.jwt.JwtTokenProvider;
import com.eventitta.common.config.redis.MockRedisConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static com.eventitta.auth.constants.AuthConstants.ACCESS_TOKEN;


@AutoConfigureMockMvc
@ActiveProfiles("test")
@SpringBootTest
@Import(MockRedisConfig.class)
public abstract class IntegrationTestSupport {

    @Autowired
    protected MockMvc mockMvc;
    @Autowired
    protected ObjectMapper objectMapper;
    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    protected Cookie buildAccessTokenCookie(Long userId) {
        return buildAccessTokenCookie(userId, "test@example.com", "USER");
    }

    protected Cookie buildAccessTokenCookie(Long userId, String email, String role) {
        String token = jwtTokenProvider.createAccessToken(userId, email, role);
        Cookie cookie = new Cookie(ACCESS_TOKEN, token);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        return cookie;
    }
}
