package com.eventitta.user.dto;

import com.eventitta.common.constants.RegexPattern;
import com.eventitta.common.constants.ValidationMessage;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "프로필 수정 요청")
public record UpdateProfileRequest(
    @Schema(description = "닉네임", example = "홍길동")
    @NotBlank(message = ValidationMessage.NICKNAME)
    @Pattern(regexp = RegexPattern.NICKNAME, message = ValidationMessage.NICKNAME)
    String nickname,
    @Schema(description = "프로필 이미지 URL")
    String profilePictureUrl,
    @Schema(description = "자기 소개")
    String selfIntro,
    @Schema(description = "관심사")
    List<String> interests,
    @Schema(description = "주소")
    String address,
    @Schema(description = "위도")
    BigDecimal latitude,
    @Schema(description = "경도")
    BigDecimal longitude
) {
}
