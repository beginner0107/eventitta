package com.eventitta.auth.dto.request;

import com.eventitta.common.constants.RegexPattern;
import com.eventitta.common.constants.ValidationMessage;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SignInRequest(
    @NotBlank(message = ValidationMessage.EMAIL)
    @Pattern(regexp = RegexPattern.EMAIL, message = ValidationMessage.EMAIL)
    String email,
    @NotBlank(message = ValidationMessage.PASSWORD)
    @Pattern(regexp = RegexPattern.PASSWORD, message = ValidationMessage.PASSWORD)
    String password
) {
}
