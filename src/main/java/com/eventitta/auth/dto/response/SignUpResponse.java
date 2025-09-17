package com.eventitta.auth.dto.response;

import com.eventitta.user.domain.User;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "회원가입 응답")
public record SignUpResponse(
    @Schema(description = "이메일", example = "user@example.com")
    String email,
    @Schema(description = "닉네임", example = "johndoe")
    String nickname
) {
    public static SignUpResponse of(User user) {
        return new SignUpResponse(
            user.getEmail(),
            user.getNickname()
        );
    }
}
