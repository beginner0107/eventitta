package com.eventitta.common.config;

import com.eventitta.event.dto.FestivalApiResponse;
import com.eventitta.event.dto.SeoulApiResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient nationalRestClient(@Value("${festival.national.base-url}") String baseUrl) {
        return RestClient.builder()
            .baseUrl(baseUrl)
            .build();
    }

    @Bean
    public RestClientConfig.FestivalApi nationalFestivalApi(
        @Qualifier("nationalRestClient") RestClient restClient) {
        RestClientAdapter adapter = RestClientAdapter.create(restClient);
        HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(adapter).build();
        return factory.createClient(FestivalApi.class);
    }

    public interface FestivalApi {
        @GetExchange
        FestivalApiResponse getFestivals(
            @RequestParam("serviceKey") String serviceKey,
            @RequestParam("pageNo") int pageNo,
            @RequestParam("numOfRows") int numOfRows,
            @RequestParam("type") String type
        );
    }


    @Bean
    public RestClient seoulRestClient(@Value("${festival.seoul.base-url}") String baseUrl) {
        return RestClient.builder()
            .baseUrl(baseUrl)
            .build();
    }

    @Bean
    public RestClientConfig.SeoulApi seoulFestivalApi(
        @Qualifier("seoulRestClient") RestClient restClient) {
        RestClientAdapter adapter = RestClientAdapter.create(restClient);
        HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(adapter).build();
        return factory.createClient(SeoulApi.class);
    }

    public interface SeoulApi {

        @GetExchange(
            url = "/{serviceKey}/{fileType}/{serviceName}/{startIndex}/{endIndex}/{unused1}/{unused2}/{dateParam}"
        )
        SeoulApiResponse getSeoulEvents(
            @PathVariable("serviceKey") String serviceKey,
            @PathVariable("fileType") String fileType,
            @PathVariable("serviceName") String serviceName,
            @PathVariable("startIndex") int startIndex,
            @PathVariable("endIndex") int endIndex,
            @PathVariable("unused1") String unused1,
            @PathVariable("unused2") String unused2,
            @PathVariable("dateParam") String dateParam
        );
    }
}
