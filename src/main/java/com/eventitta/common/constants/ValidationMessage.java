package com.eventitta.common.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ValidationMessage {
    public static final String EMAIL = "올바른 이메일 형식이어야 합니다.";
    public static final String PASSWORD = "비밀번호는 영문, 숫자, 특수문자를 포함해야 합니다.";
    public static final String NICKNAME = "닉네임은 2자 이상 20자 이하로 입력해주세요.";
}
