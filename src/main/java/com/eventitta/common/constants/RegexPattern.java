package com.eventitta.common.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RegexPattern {
    public static final String EMAIL = "^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,6}$";
    public static final String PASSWORD = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[!@#$%^&*()\\-_=+{};:,<.>]).{8,20}$";
    public static final String NICKNAME = "^[가-힣a-zA-Z0-9]{2,20}$";
}
