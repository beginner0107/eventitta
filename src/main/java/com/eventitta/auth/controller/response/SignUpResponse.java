package com.eventitta.auth.controller.response;

import com.eventitta.auth.service.dto.SignUpResult;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "회원가입 응답")
public record SignUpResponse(
    @Schema(description = "이메일", example = "user@example.com")
    String email,
    @Schema(description = "닉네임", example = "johndoe")
    String nickname
) {
    public static SignUpResponse of(SignUpResult result) {
        return new SignUpResponse(
            result.email(),
            result.nickname()
        );
    }
}
