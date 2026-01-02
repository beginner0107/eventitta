package com.eventitta.notification.domain;

public enum AlertLevel {
    CRITICAL(10, 16711680),   // 빨강
    HIGH(5, 16744448),        // 주황
    MEDIUM(2, 16766720),      // 노랑
    INFO(1, 65280);           // 초록

    private final int alertLimit;
    private final int discordColor;

    AlertLevel(int alertLimit, int discordColor) {
        this.alertLimit = alertLimit;
        this.discordColor = discordColor;
    }

    public int getAlertLimit() {
        return alertLimit;
    }

    public int getDiscordColor() {
        return discordColor;
    }
}
