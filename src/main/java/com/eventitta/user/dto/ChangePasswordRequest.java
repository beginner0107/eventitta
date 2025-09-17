package com.eventitta.user.dto;

import com.eventitta.common.constants.RegexPattern;
import com.eventitta.common.constants.ValidationMessage;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "비밀번호 변경 요청")
public record ChangePasswordRequest(
    @Schema(description = "현재 비밀번호")
    @NotBlank(message = ValidationMessage.PASSWORD)
    String currentPassword,

    @Schema(description = "새 비밀번호", example = "P@ssw0rd!", pattern = RegexPattern.PASSWORD)
    @NotBlank(message = ValidationMessage.PASSWORD)
    @Pattern(regexp = RegexPattern.PASSWORD, message = ValidationMessage.PASSWORD)
    String newPassword
) {
}
