package com.eventitta.user.dto;

import com.eventitta.user.domain.User;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Schema(description = "사용자 프로필 응답")
public record UserProfileResponse(
    @Schema(description = "사용자 ID")
    Long id,
    @Schema(description = "이메일")
    String email,
    @Schema(description = "닉네임")
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
    public UserProfileResponse {
        profilePictureUrl = Objects.requireNonNullElse(profilePictureUrl, "");
        selfIntro = Objects.requireNonNullElse(selfIntro, "");
        interests = interests != null ? interests : new ArrayList<>();
        address = Objects.requireNonNullElse(address, "");
        latitude = Objects.requireNonNullElse(latitude, BigDecimal.ZERO);
        longitude = Objects.requireNonNullElse(longitude, BigDecimal.ZERO);
    }

    public static UserProfileResponse from(User user) {
        return new UserProfileResponse(
            user.getId(),
            user.getEmail(),
            user.getNickname(),
            user.getProfilePictureUrl(),
            user.getSelfIntro(),
            user.getInterests(),
            user.getAddress(),
            user.getLatitude(),
            user.getLongitude()
        );
    }
}
