package com.eventitta;

import com.eventitta.api.auth.jwt.JwtTokenProvider;
import com.eventitta.domain.auth.domain.RefreshToken;
import com.eventitta.domain.auth.repository.RefreshTokenRepository;
import com.eventitta.infra.common.config.redis.MockRedisConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static com.eventitta.api.auth.constants.AuthConstants.ACCESS_TOKEN;


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
    @Autowired
    private RefreshTokenRepository refreshTokenRepository;
    @Autowired
    private Pbkdf2PasswordEncoder refreshTokenEncoder;

    protected Cookie buildAccessTokenCookie(Long userId) {
        return buildAccessTokenCookie(userId, "test@example.com", "USER");
    }

    protected Cookie buildAccessTokenCookie(Long userId, String email, String role) {
        String sessionId = UUID.randomUUID().toString();
        refreshTokenRepository.save(RefreshToken.issue(
            userId,
            sessionId,
            "test-key-" + UUID.randomUUID(),
            refreshTokenEncoder.encode("test-secret"),
            LocalDateTime.now().plusDays(1),
            LocalDateTime.now(),
            "127.0.0.0/24",
            "integration-test"
        ));
        String token = jwtTokenProvider.createAccessToken(userId, email, role, 0L, sessionId);
        Cookie cookie = new Cookie(ACCESS_TOKEN, token);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        return cookie;
    }
}
