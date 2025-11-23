package com.eventitta.gamification.constants;

public final class GamificationSlackConstants {

    private GamificationSlackConstants() {
    }

    public static final String REQUEST_URI = "EventListener";
    public static final String USER_INFO_FORMAT = "userId=%d";

    public static final String ACTIVITY_RECORD_FAILED_MESSAGE_FORMAT =
        "포인트 기록 실패 - userId: %d, activity: %s, targetId: %d";

    public static final String ACTIVITY_REVOKE_FAILED_MESSAGE_FORMAT =
        "포인트 취소 실패 - userId: %d, activity: %s, targetId: %d";
}

