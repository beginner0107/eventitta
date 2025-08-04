package com.eventitta.common.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class RestClientLoggingInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(
        HttpRequest request, byte[] body, ClientHttpRequestExecution execution
    ) throws IOException {

        log.info("➡ [{}] {}", request.getMethod(), request.getURI());
        log.debug("➡ 요청 바디: {}", new String(body, StandardCharsets.UTF_8));

        ClientHttpResponse response = execution.execute(request, body);
        ClientHttpResponse buffered = new CustomBufferingClientHttpResponseWrapper(response);

        log.info("⬅ 응답 상태: {}", buffered.getStatusCode());
        log.debug("⬅ 응답 바디: {}", new String(buffered.getBody().readAllBytes(), StandardCharsets.UTF_8));

        return buffered;
    }
}
