package com.eventitta.domain.auth.config;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuthRateLimitProperties {
    private int loginMaxFailures = 5;
    private int socialLoginMaxFailures = 5;
    private int refreshMaxFailures = 8;
    private long windowSeconds = 300L;
    private long lockSeconds = 900L;
}
