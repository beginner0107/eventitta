package com.eventitta.api.auth.cookie;

import com.eventitta.api.auth.cookie.CookieProperties;
import com.eventitta.api.auth.oauth.kakao.KakaoWebProperties;
import com.eventitta.domain.auth.port.AuthTokenProvider;
import com.eventitta.domain.auth.service.dto.TokenResult;
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
    private AuthTokenProvider authTokenProvider;

    private CookieManager cookieManager;

    @BeforeEach
    void setUp() {
        CookieProperties cookieProperties = new CookieProperties();
        cookieProperties.setSecure(true);
        KakaoWebProperties kakaoWebProperties = new KakaoWebProperties();
        kakaoWebProperties.setStateCookieMaxAgeSeconds(300);
        cookieManager = new CookieManager(authTokenProvider, cookieProperties, kakaoWebProperties);
    }

    @Test
    @DisplayName("access cookie 는 access TTL, refresh cookie 는 refresh TTL 을 사용한다")
    void addTokenCookies_usesSeparateLifetimes() {
        given(authTokenProvider.getAccessTokenValidityMs()).willReturn(3_600_000L);
        given(authTokenProvider.getRefreshTokenValidityMs()).willReturn(86_400_000L);

        MockHttpServletResponse response = new MockHttpServletResponse();
        cookieManager.addTokenCookies(response, new TokenResult("access-token", "refresh-key.refresh-secret"));

        List<String> cookies = response.getHeaders(HttpHeaders.SET_COOKIE);
        assertThat(cookies).anySatisfy(cookie -> {
            assertThat(cookie).contains("access_token=access-token");
            assertThat(cookie).contains("Max-Age=3600");
        });
        assertThat(cookies).anySatisfy(cookie -> {
            assertThat(cookie).contains("refresh_token=refresh-key.refresh-secret");
            assertThat(cookie).contains("Max-Age=86400");
        });
    }

    @Test
    @DisplayName("short-lived cookie 는 지정된 짧은 TTL 을 사용한다")
    void addShortLivedCookie_usesConfiguredTtl() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        cookieManager.addShortLivedCookie(response, "oauth_state", "oauth-state");

        String cookie = response.getHeader(HttpHeaders.SET_COOKIE);
        assertThat(cookie).contains("oauth_state=oauth-state");
        assertThat(cookie).contains("Max-Age=300");
        assertThat(cookie).contains("HttpOnly");
    }
}
