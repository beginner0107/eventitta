package com.eventitta.domain.auth.dto.request;

import com.eventitta.domain.common.constants.RegexPattern;
import com.eventitta.domain.common.constants.ValidationMessage;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "이메일 기반 인증 요청")
public record EmailRequest(
    @Schema(description = "이메일", example = "user@example.com", pattern = RegexPattern.EMAIL)
    @NotBlank(message = ValidationMessage.EMAIL)
    @Pattern(regexp = RegexPattern.EMAIL, message = ValidationMessage.EMAIL)
    String email
) {
}
