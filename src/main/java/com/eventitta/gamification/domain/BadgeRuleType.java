package com.eventitta.gamification.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum BadgeRuleType {
    ACTIVITY_COUNT,
    TOTAL_POINTS,
    STREAK;

    @JsonCreator
    public static BadgeRuleType from(String value) {
        return BadgeRuleType.valueOf(value.toUpperCase());
    }

    @JsonValue
    public String toValue() {
        return name();
    }
}
