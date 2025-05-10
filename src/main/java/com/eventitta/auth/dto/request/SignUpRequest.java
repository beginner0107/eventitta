package com.eventitta.auth.dto.request;

import com.eventitta.common.constants.RegexPattern;
import com.eventitta.common.constants.ValidationMessage;
import com.eventitta.user.domain.Provider;
import com.eventitta.user.domain.Role;
import com.eventitta.user.domain.User;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.security.crypto.password.PasswordEncoder;

@Schema(description = "회원가입")
public record SignUpRequest(
    @Schema(description = "이메일", example = "user@example.com", pattern = RegexPattern.EMAIL)
    @NotBlank(message = ValidationMessage.EMAIL)
    @Pattern(regexp = RegexPattern.EMAIL, message = ValidationMessage.EMAIL)
    String email,
    @Schema(description = "비밀번호", example = "P@ssw0rd!", pattern = RegexPattern.PASSWORD)
    @NotBlank(message = ValidationMessage.PASSWORD)
    @Pattern(regexp = RegexPattern.PASSWORD, message = ValidationMessage.PASSWORD)
    String password,
    @Schema(description = "닉네임", example = "johndoe", minLength = 2, maxLength = 20, pattern = RegexPattern.NICKNAME)
    @NotBlank(message = ValidationMessage.NICKNAME)
    @Size(min = 2, max = 20, message = ValidationMessage.NICKNAME)
    @Pattern(regexp = RegexPattern.NICKNAME, message = ValidationMessage.NICKNAME)
    String nickname

) {
    public User toEntity(PasswordEncoder encoder) {
        return User.builder()
            .email(email)
            .password(encoder.encode(password))
            .nickname(nickname)
            .role(Role.USER)
            .provider(Provider.LOCAL)
            .build();
    }
}
