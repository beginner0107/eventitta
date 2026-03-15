package com.eventitta.user.service.dto;

import java.math.BigDecimal;
import java.util.List;

public record UpdateProfileCommand(
    String nickname,
    String profilePictureUrl,
    String selfIntro,
    List<String> interests,
    String address,
    BigDecimal latitude,
    BigDecimal longitude
) {
}
