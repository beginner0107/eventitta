package com.eventitta.domain.festivals.dto.external.geocoding;

import java.math.BigDecimal;

public record Coordinates(
    BigDecimal latitude,
    BigDecimal longitude
) {
    public boolean isValid() {
        return latitude != null && longitude != null;
    }
}
