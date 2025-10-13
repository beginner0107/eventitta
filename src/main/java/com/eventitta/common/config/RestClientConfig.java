package com.eventitta.common.config;

import com.eventitta.festivals.api.NationalFestivalApi;
import com.eventitta.festivals.api.SeoulFestivalApi;
import com.eventitta.common.interceptor.RestClientLoggingInterceptor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.net.http.HttpClient;
import java.time.Duration;

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

    @Bean
    public HttpClient geocodingHttpClient() {
        return HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    }

    @Bean
    public RestClient geocodingRestClient(
        @Value("${festival.geocoding.base-url}") String baseUrl,
        @Value("${festival.geocoding.user-agent}") String userAgent,
        @Qualifier("geocodingHttpClient") HttpClient httpClient
    ) {
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);

        return RestClient.builder()
            .baseUrl(baseUrl)
            .requestFactory(requestFactory)
            .defaultHeader("User-Agent", userAgent)
            .defaultHeader("Connection", "close")
            .requestInterceptor(new RestClientLoggingInterceptor())
            .build();
    }
}
