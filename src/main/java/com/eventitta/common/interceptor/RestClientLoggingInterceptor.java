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

        long startTime = System.currentTimeMillis();

        ClientHttpResponse response = execution.execute(request, body);
        ClientHttpResponse buffered = new CustomBufferingClientHttpResponseWrapper(response);

        long duration = System.currentTimeMillis() - startTime;

        // INFO: 외부 API 호출 요약 (1줄)
        log.info("[External API] {} {} - {} ({}ms)",
            request.getMethod(),
            request.getURI(),
            buffered.getStatusCode(),
            duration);

        // DEBUG: 요청/응답 상세 (local에서만 출력)
        if (log.isDebugEnabled()) {
            log.debug("➡ Request Body: {}", new String(body, StandardCharsets.UTF_8));
            log.debug("⬅ Response Body: {}",
                new String(buffered.getBody().readAllBytes(), StandardCharsets.UTF_8));
        }

        return buffered;
    }
}
