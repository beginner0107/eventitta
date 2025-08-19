package com.eventitta.common.notification.constants;

public final class AlertConstants {

    public static final String CONNECTION_REFUSED_MESSAGE = "Connection refused";

    public static final String CRITICAL_COLOR = "#FF0000"; // Red
    public static final String HIGH_COLOR = "#FF8C00";     // Orange
    public static final String MEDIUM_COLOR = "#FFD700";   // Yellow
    public static final String INFO_COLOR = "#00FF00";     // Green

    public static final String DEFAULT_ALERT_CHANNEL = "#alerts";

    public static final int CRITICAL_ALERT_LIMIT = 10;
    public static final int HIGH_ALERT_LIMIT = 5;
    public static final int MEDIUM_ALERT_LIMIT = 2;
    public static final int INFO_ALERT_LIMIT = 1;

    public static final String ALERT_EMOJI = ":warning:";
    public static final String ALERT_TEXT_FORMAT = "%s %s Alert";

    public static final int RATE_LIMIT_WINDOW_MINUTES = 5;
}
