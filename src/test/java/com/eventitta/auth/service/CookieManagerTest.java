package com.eventitta.auth.service;

import com.eventitta.auth.jwt.JwtTokenProvider;
import com.eventitta.auth.properties.CookieProperties;
import com.eventitta.auth.service.dto.TokenResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class CookieManagerTest {

    @Mock
    private JwtTokenProvider tokenProvider;

    private CookieManager cookieManager;

    @BeforeEach
    void setUp() {
        CookieProperties cookieProperties = new CookieProperties();
        cookieProperties.setSecure(true);
        cookieProperties.setAccessTokenRefreshBufferMs(300_000L);
        cookieManager = new CookieManager(tokenProvider, cookieProperties);
    }

    @Test
    @DisplayName("토큰 쿠키를 저장할 때 운영 보안 속성과 access token refresh 버퍼를 적용한다")
    void addTokenCookies_appliesSecureFlagAndAccessTokenBuffer() {
        given(tokenProvider.getAccessTokenValidityMs()).willReturn(3_600_000L);
        given(tokenProvider.getRefreshTokenValidityMs()).willReturn(86_400_000L);

        MockHttpServletResponse response = new MockHttpServletResponse();

        cookieManager.addTokenCookies(response, new TokenResult("access-token", "refresh-token"));

        List<String> cookies = response.getHeaders(HttpHeaders.SET_COOKIE);
        assertThat(cookies).hasSize(2);
        assertThat(cookies).anySatisfy(cookie -> {
            assertThat(cookie).contains("access_token=access-token");
            assertThat(cookie).contains("Max-Age=3900");
            assertThat(cookie).contains("Secure");
            assertThat(cookie).contains("HttpOnly");
            assertThat(cookie).contains("SameSite=Strict");
        });
        assertThat(cookies).anySatisfy(cookie -> {
            assertThat(cookie).contains("refresh_token=refresh-token");
            assertThat(cookie).contains("Max-Age=86400");
            assertThat(cookie).contains("Secure");
            assertThat(cookie).contains("HttpOnly");
            assertThat(cookie).contains("SameSite=Strict");
        });
    }

    @Test
    @DisplayName("쿠키 삭제 시에도 동일한 보안 속성을 유지한다")
    void deleteCookie_preservesSecurityAttributes() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        cookieManager.deleteCookie(response, "access_token");

        String cookie = response.getHeader(HttpHeaders.SET_COOKIE);
        assertThat(cookie).contains("access_token=");
        assertThat(cookie).contains("Max-Age=0");
        assertThat(cookie).contains("Secure");
        assertThat(cookie).contains("HttpOnly");
        assertThat(cookie).contains("SameSite=Strict");
    }
}
