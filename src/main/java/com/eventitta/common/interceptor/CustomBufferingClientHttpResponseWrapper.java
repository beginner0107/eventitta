package com.eventitta.common.interceptor;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class CustomBufferingClientHttpResponseWrapper implements ClientHttpResponse {

    private final ClientHttpResponse original;
    private byte[] cachedBody;

    public CustomBufferingClientHttpResponseWrapper(ClientHttpResponse response) throws IOException {
        this.original = response;
        this.cachedBody = response.getBody().readAllBytes(); // body 캐싱
    }

    @Override
    public InputStream getBody() {
        return new ByteArrayInputStream(cachedBody);
    }

    @Override
    public HttpStatusCode getStatusCode() throws IOException {
        return original.getStatusCode();
    }

    @Override
    public String getStatusText() throws IOException {
        return original.getStatusText();
    }

    @Override
    public HttpHeaders getHeaders() {
        return original.getHeaders();
    }

    @Override
    public void close() {
        original.close();
    }
}
