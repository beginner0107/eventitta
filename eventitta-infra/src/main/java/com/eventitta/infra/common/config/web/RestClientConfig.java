package com.eventitta.infra.common.config.web;

import com.eventitta.infra.common.interceptor.RestClientLoggingInterceptor;
import com.eventitta.infra.festivals.client.NationalFestivalHttpApi;
import com.eventitta.infra.festivals.client.SeoulFestivalHttpApi;
import com.eventitta.infra.festivals.config.GeocodingProperties;
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
    public NationalFestivalHttpApi nationalFestivalApi(
        @Qualifier("nationalRestClient") RestClient restClient
    ) {
        RestClientAdapter adapter = RestClientAdapter.create(restClient);
        HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(adapter).build();
        return factory.createClient(NationalFestivalHttpApi.class);
    }

    @Bean
    public RestClient seoulRestClient(@Value("${festival.seoul.base-url}") String baseUrl) {
        return RestClient.builder()
            .baseUrl(baseUrl)
            .requestInterceptor(new RestClientLoggingInterceptor())
            .build();
    }

    @Bean
    public SeoulFestivalHttpApi seoulFestivalApi(
        @Qualifier("seoulRestClient") RestClient restClient
    ) {
        RestClientAdapter adapter = RestClientAdapter.create(restClient);
        HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(adapter).build();
        return factory.createClient(SeoulFestivalHttpApi.class);
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
        GeocodingProperties properties,
        @Qualifier("geocodingHttpClient") HttpClient httpClient
    ) {
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);

        return RestClient.builder()
            .baseUrl(properties.getBaseUrl())
            .requestFactory(requestFactory)
            .defaultHeader("User-Agent", properties.getUserAgent())
            .defaultHeader("Connection", "close")
            .requestInterceptor(new RestClientLoggingInterceptor())
            .build();
    }

    @Bean
    @Qualifier("discordRestClient")
    public RestClient discordRestClient() {
        return RestClient.builder()
            .defaultHeader("Content-Type", "application/json")
            .build();
    }
}
