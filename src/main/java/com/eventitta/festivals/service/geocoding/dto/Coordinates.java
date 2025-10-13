package com.eventitta.festivals.service.geocoding.dto;

import java.math.BigDecimal;

public record Coordinates(
    BigDecimal latitude,
    BigDecimal longitude
) {
    public boolean isValid() {
        return latitude != null && longitude != null;
    }
}
