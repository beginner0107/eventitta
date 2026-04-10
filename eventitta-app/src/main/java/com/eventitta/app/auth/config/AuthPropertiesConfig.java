package com.eventitta.app.auth.config;

import com.eventitta.domain.auth.config.AuthActionTokenProperties;
import com.eventitta.domain.auth.config.AuthRateLimitProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AuthPropertiesConfig {

    @Bean
    @ConfigurationProperties(prefix = "auth.action-token")
    public AuthActionTokenProperties authActionTokenProperties() {
        return new AuthActionTokenProperties();
    }

    @Bean
    @ConfigurationProperties(prefix = "auth.rate-limit")
    public AuthRateLimitProperties authRateLimitProperties() {
        return new AuthRateLimitProperties();
    }
}
