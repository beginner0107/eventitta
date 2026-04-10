package com.eventitta.infra.auth.client;

import com.eventitta.domain.auth.exception.AuthErrorCode;
import com.eventitta.domain.auth.dto.KakaoUserInfo;
import com.eventitta.infra.auth.client.dto.KakaoTokenResponse;
import com.eventitta.infra.auth.client.dto.KakaoUserResponse;
import com.eventitta.infra.auth.config.KakaoAuthProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClientException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class KakaoAuthClientAdapterTest {

    @Mock
    private KakaoTokenHttpApi kakaoTokenHttpApi;

    @Mock
    private KakaoUserHttpApi kakaoUserHttpApi;

    @Mock
    private KakaoAuthProperties properties;

    @InjectMocks
    private KakaoAuthClientAdapter kakaoAuthClientAdapter;

    @Test
    @DisplayName("인가 코드 교환 성공 시 access token 을 반환한다")
    void exchangeAuthorizationCode_success() {
        given(properties.getClientId()).willReturn("client-id");
        given(properties.getClientSecret()).willReturn("client-secret");
        given(kakaoTokenHttpApi.exchangeAuthorizationCode("authorization_code", "client-id", "redirect-uri", "code", "client-secret"))
            .willReturn(new KakaoTokenResponse("kakao-access-token"));

        String result = kakaoAuthClientAdapter.exchangeAuthorizationCode("code", "redirect-uri");

        assertThat(result).isEqualTo("kakao-access-token");
    }

    @Test
    @DisplayName("사용자 정보 조회 응답을 도메인용 KakaoUserInfo 로 매핑한다")
    void getUserInfo_success() {
        KakaoUserResponse.Profile profile = new KakaoUserResponse.Profile("tester", "https://image");
        KakaoUserResponse.KakaoAccount account = new KakaoUserResponse.KakaoAccount(true, "user@test.com", profile);
        given(kakaoUserHttpApi.getUserInfo("Bearer kakao-access-token", true, "[\"kakao_account.email\",\"kakao_account.profile\"]"))
            .willReturn(new KakaoUserResponse(123456789L, account));

        KakaoUserInfo result = kakaoAuthClientAdapter.getUserInfo("kakao-access-token");

        assertThat(result.providerUserId()).isEqualTo("123456789");
        assertThat(result.email()).isEqualTo("user@test.com");
        assertThat(result.emailVerified()).isTrue();
        assertThat(result.nickname()).isEqualTo("tester");
    }

    @Test
    @DisplayName("카카오 API 호출 실패는 SOCIAL_AUTH_FAILED 로 변환한다")
    void exchangeAuthorizationCode_failure() {
        given(properties.getClientId()).willReturn("client-id");
        given(properties.getClientSecret()).willReturn("client-secret");
        given(kakaoTokenHttpApi.exchangeAuthorizationCode("authorization_code", "client-id", "redirect-uri", "code", "client-secret"))
            .willThrow(new RestClientException("boom"));

        assertThatThrownBy(() -> kakaoAuthClientAdapter.exchangeAuthorizationCode("code", "redirect-uri"))
            .extracting("errorCode")
            .isEqualTo(AuthErrorCode.SOCIAL_AUTH_FAILED);
    }
}
