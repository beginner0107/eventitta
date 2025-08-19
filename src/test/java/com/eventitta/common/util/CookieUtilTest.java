package com.eventitta.common.util;

import com.eventitta.auth.dto.response.TokenResponse;
import com.eventitta.auth.jwt.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Duration;
import java.util.List;

import static com.eventitta.auth.constants.AuthConstants.ACCESS_TOKEN;
import static com.eventitta.auth.constants.AuthConstants.REFRESH_TOKEN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(SpringExtension.class)
@DisplayName("쿠키 유틸 단위테스트")
class CookieUtilTest {

    @Test
    @DisplayName("액세스 토큰 쿠키는 이름·값·httpOnly·path·sameSite·maxAge가 올바르게 설정된다")
    void givenTokenValue_whenCreateAccessTokenCookie_thenAttributesAreCorrect() {
        // given
        String tokenValue = "access123";
        long validityMs = 5_000L;

        // when
        ResponseCookie cookie = CookieUtil.createAccessTokenCookie(tokenValue, validityMs);

        // then
        assertThat(cookie.getName()).isEqualTo(ACCESS_TOKEN);
        assertThat(cookie.getValue()).isEqualTo(tokenValue);
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getPath()).isEqualTo("/");
        assertThat(cookie.getSameSite()).isEqualTo("Strict");
        assertThat(cookie.getMaxAge()).isEqualTo(Duration.ofMillis(validityMs));
    }

    @Test
    @DisplayName("응답 헤더에 두 개의 엑세스/리프레시 토큰 쿠키를 추가한다")
    void givenTokenResponse_whenAddTokenCookies_thenTwoSetCookieHeadersAreAppended() {
        // given
        TokenResponse tokens = new TokenResponse("at-xyz", "rt-abc");
        JwtTokenProvider mockProvider = org.mockito.Mockito.mock(JwtTokenProvider.class);
        given(mockProvider.getAccessTokenValidityMs()).willReturn(1_000L);
        given(mockProvider.getRefreshTokenValidityMs()).willReturn(2_000L);

        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        CookieUtil.addTokenCookies(response, tokens, mockProvider);

        // then
        List<String> setCookieHeaders = response.getHeaders(HttpHeaders.SET_COOKIE);
        assertThat(setCookieHeaders).hasSize(2);
        assertThat(setCookieHeaders.get(0)).startsWith(ACCESS_TOKEN + "=at-xyz;");
        assertThat(setCookieHeaders.get(1)).startsWith(REFRESH_TOKEN + "=rt-abc;");
    }
}
