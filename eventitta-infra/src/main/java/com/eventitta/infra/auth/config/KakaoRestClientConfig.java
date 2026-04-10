package com.eventitta.infra.auth.config;

import com.eventitta.infra.auth.client.KakaoTokenHttpApi;
import com.eventitta.infra.auth.client.KakaoUserHttpApi;
import com.eventitta.infra.common.interceptor.RestClientLoggingInterceptor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class KakaoRestClientConfig {

    @Bean
    @Qualifier("kakaoTokenRestClient")
    public RestClient kakaoTokenRestClient(KakaoAuthProperties properties) {
        return RestClient.builder()
            .baseUrl(properties.getTokenBaseUrl())
            .requestInterceptor(new RestClientLoggingInterceptor())
            .build();
    }

    @Bean
    public KakaoTokenHttpApi kakaoTokenHttpApi(@Qualifier("kakaoTokenRestClient") RestClient restClient) {
        RestClientAdapter adapter = RestClientAdapter.create(restClient);
        HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(adapter).build();
        return factory.createClient(KakaoTokenHttpApi.class);
    }

    @Bean
    @Qualifier("kakaoUserRestClient")
    public RestClient kakaoUserRestClient(KakaoAuthProperties properties) {
        return RestClient.builder()
            .baseUrl(properties.getApiBaseUrl())
            .requestInterceptor(new RestClientLoggingInterceptor())
            .build();
    }

    @Bean
    public KakaoUserHttpApi kakaoUserHttpApi(@Qualifier("kakaoUserRestClient") RestClient restClient) {
        RestClientAdapter adapter = RestClientAdapter.create(restClient);
        HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(adapter).build();
        return factory.createClient(KakaoUserHttpApi.class);
    }
}
