package com.eventitta.common.filter;

import jakarta.servlet.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class ResponseTimeFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException {
        long start = System.currentTimeMillis();

        chain.doFilter(request, response);  // 컨트롤러 실행

        long end = System.currentTimeMillis();
        log.info("진짜 API 응답 시간: {}ms", end - start);
    }
}
