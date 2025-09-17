package com.eventitta.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.eventitta.auth.constants.AuthConstants.ACCESS_TOKEN;
import static com.eventitta.auth.constants.AuthConstants.REFRESH_TOKEN;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Eventitta API 명세")
                .version("v1.0.0")
                .description("Eventitta 프로젝트의 REST API 명세서입니다.")
            )
            .components(new Components()
                .addResponses("TokensSetCookies", new ApiResponse()
                    .description("로그인/토큰 재발급 성공 시 `" + ACCESS_TOKEN + "`, `" + REFRESH_TOKEN + "` 쿠키 설정")
                    .addHeaderObject("Set-Cookie", new Header()
                        .description(ACCESS_TOKEN + "=...; HttpOnly; Path=/; SameSite=Strict")
                        .schema(new StringSchema()._default(ACCESS_TOKEN + "=eyJ...; HttpOnly; Path=/; SameSite=Strict"))
                    )
                    .addHeaderObject("Set-Cookie", new Header()
                        .description(REFRESH_TOKEN + "=...; HttpOnly; Path=/; SameSite=Strict")
                        .schema(new StringSchema()._default(REFRESH_TOKEN + "=abc...; HttpOnly; Path=/; SameSite=Strict"))
                    )
                )
            );
    }
}
