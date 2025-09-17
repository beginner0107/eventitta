package com.eventitta.common.notification.domain;

public enum AlertLevel {
    CRITICAL(10, "#FF0000"),
    HIGH(5, "#FF8C00"),
    MEDIUM(2, "#FFD700"),
    INFO(1, "#00FF00");

    private final int alertLimit;
    private final String color;

    AlertLevel(int alertLimit, String color) {
        this.alertLimit = alertLimit;
        this.color = color;
    }

    public int getAlertLimit() {
        return alertLimit;
    }

    public String getColor() {
        return color;
    }
}
