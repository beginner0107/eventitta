package com.eventitta.api.user.controller.request;

import com.eventitta.domain.common.constants.RegexPattern;
import com.eventitta.domain.common.constants.ValidationMessage;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "로컬 비밀번호 설정 요청")
public record SetLocalPasswordRequest(
    @Schema(description = "새 비밀번호", example = "P@ssw0rd!", pattern = RegexPattern.PASSWORD)
    @NotBlank(message = ValidationMessage.PASSWORD)
    @Pattern(regexp = RegexPattern.PASSWORD, message = ValidationMessage.PASSWORD)
    String newPassword
) {
}
