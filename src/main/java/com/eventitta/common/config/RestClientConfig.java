package com.eventitta.common.config;

import com.eventitta.event.dto.FestivalApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class RestClientConfig {

    @Value("${festival.api.base-url}")
    private String baseUrl;

    @Bean
    public RestClient restClient() {
        return RestClient.builder()
            .baseUrl(baseUrl)
            .build();
    }

    @Bean
    public FestivalApi festivalApi(RestClient restClient) {
        RestClientAdapter restClientAdapter = RestClientAdapter.create(restClient);
        HttpServiceProxyFactory factory = HttpServiceProxyFactory
            .builderFor(restClientAdapter)
            .build();
        return factory.createClient(FestivalApi.class);
    }

    /**
     * 공공 축제 API 호출을 위한 인터페이스 정의
     * baseUrl 은 RestClient.builder() 에서 지정했으므로, 여기서는 "쿼리 스트링만" 작성
     */
    public interface FestivalApi {

        @GetExchange
        FestivalApiResponse getFestivals(
            @RequestParam("serviceKey") String serviceKey,
            @RequestParam("pageNo") int pageNo,
            @RequestParam("numOfRows") int numOfRows,
            @RequestParam("type") String type
        );
    }
}
