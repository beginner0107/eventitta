package com.eventitta.dashboard.enums;

public enum RankingPeriod {
    WEEKLY,
    ALL;

    public static RankingPeriod from(String value) {
        if (value == null) {
            return ALL;
        }
        return switch (value.toLowerCase()) {
            case "weekly" -> WEEKLY;
            default -> ALL;
        };
    }
}
