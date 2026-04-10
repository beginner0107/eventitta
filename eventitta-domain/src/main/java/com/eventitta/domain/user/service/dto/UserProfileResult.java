package com.eventitta.domain.user.service.dto;

import java.math.BigDecimal;
import java.util.List;

public record UserProfileResult(
    Long id,
    String email,
    String nickname,
    String profilePictureUrl,
    String selfIntro,
    List<String> interests,
    String address,
    BigDecimal latitude,
    BigDecimal longitude
) {
}
