package com.eventitta.auth.dto.request;

import com.eventitta.common.constants.RegexPattern;
import com.eventitta.common.constants.ValidationMessage;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "로그인")
public record SignInRequest(
    @Schema(description = "이메일", example = "user@example.com", pattern = RegexPattern.EMAIL)
    @NotBlank(message = ValidationMessage.EMAIL)
    @Pattern(regexp = RegexPattern.EMAIL, message = ValidationMessage.EMAIL)
    String email,
    @Schema(description = "비밀번호", example = "P@ssw0rd!", pattern = RegexPattern.PASSWORD)
    @NotBlank(message = ValidationMessage.PASSWORD)
    @Pattern(regexp = RegexPattern.PASSWORD, message = ValidationMessage.PASSWORD)
    String password
) {
}
