package com.eventitta.domain.user.service.dto;

import java.math.BigDecimal;
import java.util.List;

public record UpdateProfileCommand(
    String nickname,
    Long profileImageMediaId,
    String selfIntro,
    List<String> interests,
    String address,
    BigDecimal latitude,
    BigDecimal longitude
) {
}
