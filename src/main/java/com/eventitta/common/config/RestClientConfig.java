package com.eventitta.common.config;

import com.eventitta.festivals.api.NationalFestivalApi;
import com.eventitta.festivals.api.SeoulFestivalApi;
import com.eventitta.common.interceptor.RestClientLoggingInterceptor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient nationalRestClient(@Value("${festival.national.base-url}") String baseUrl) {
        return RestClient.builder()
            .baseUrl(baseUrl)
            .requestInterceptor(new RestClientLoggingInterceptor())
            .build();
    }

    @Bean
    public NationalFestivalApi nationalFestivalApi(
        @Qualifier("nationalRestClient") RestClient restClient
    ) {
        RestClientAdapter adapter = RestClientAdapter.create(restClient);
        HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(adapter).build();
        return factory.createClient(NationalFestivalApi.class);
    }

    @Bean
    public RestClient seoulRestClient(@Value("${festival.seoul.base-url}") String baseUrl) {
        return RestClient.builder()
            .baseUrl(baseUrl)
            .requestInterceptor(new RestClientLoggingInterceptor())
            .build();
    }

    @Bean
    public SeoulFestivalApi seoulFestivalApi(
        @Qualifier("seoulRestClient") RestClient restClient
    ) {
        RestClientAdapter adapter = RestClientAdapter.create(restClient);
        HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(adapter).build();
        return factory.createClient(SeoulFestivalApi.class);
    }

    @Bean
    public RestClient slackRestClient() {
        return RestClient.builder()
            .requestInterceptor(new RestClientLoggingInterceptor())
            .build();
    }
}
