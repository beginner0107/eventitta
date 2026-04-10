package com.eventitta.api.auth.controller.request;
import com.eventitta.domain.common.constants.RegexPattern;
import com.eventitta.domain.common.constants.ValidationMessage;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "회원가입")
public record SignUpRequest(
    @Schema(description = "이메일", example = "user@example.com", pattern = RegexPattern.EMAIL)
    @NotBlank(message = ValidationMessage.EMAIL)
    @Size(min = 3, max = 255)
    @Pattern(regexp = RegexPattern.EMAIL, message = ValidationMessage.EMAIL)
    String email,
    @Schema(description = "비밀번호", example = "P@ssw0rd!", pattern = RegexPattern.PASSWORD)
    @NotBlank(message = ValidationMessage.PASSWORD)
    @Pattern(regexp = RegexPattern.PASSWORD, message = ValidationMessage.PASSWORD)
    String password,
    @Schema(description = "닉네임", example = "johndoe", minLength = 2, maxLength = 20, pattern = RegexPattern.NICKNAME)
    @NotBlank(message = ValidationMessage.NICKNAME)
    @Pattern(regexp = RegexPattern.NICKNAME, message = ValidationMessage.NICKNAME)
    String nickname
) {
}
