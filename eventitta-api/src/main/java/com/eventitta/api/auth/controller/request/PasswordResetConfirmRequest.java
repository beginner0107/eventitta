package com.eventitta.api.auth.controller.request;

import com.eventitta.domain.common.constants.RegexPattern;
import com.eventitta.domain.common.constants.ValidationMessage;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "비밀번호 재설정 완료 요청")
public record PasswordResetConfirmRequest(
    @Schema(description = "메일로 전달된 비밀번호 재설정 토큰")
    @NotBlank
    String token,
    @Schema(description = "새 비밀번호", example = "P@ssw0rd!", pattern = RegexPattern.PASSWORD)
    @NotBlank(message = ValidationMessage.PASSWORD)
    @Pattern(regexp = RegexPattern.PASSWORD, message = ValidationMessage.PASSWORD)
    String newPassword
) {
}
