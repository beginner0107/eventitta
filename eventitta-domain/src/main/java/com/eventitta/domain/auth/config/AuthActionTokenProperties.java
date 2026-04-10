package com.eventitta.domain.auth.config;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuthActionTokenProperties {
    private long emailVerificationTtlSeconds = 86_400L;
    private long passwordResetTtlSeconds = 1_800L;
}
