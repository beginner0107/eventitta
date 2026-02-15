package com.eventitta.gamification.constants;

public final class GamificationRetryConstants {

    private GamificationRetryConstants() {
    }

    // 실패 이벤트 관련 재시도 최대 횟수
    public static final int FAILED_EVENT_MAX_RETRY_COUNT = 5;

    // 실패 이벤트 관련 에러 메시지 최대 길이
    public static final int FAILED_EVENT_MAX_ERROR_MESSAGE_LENGTH = 1000;

    // 실패 이벤트 재처리 배치 크기
    public static final int FAILED_EVENT_RETRY_BATCH_SIZE = 100;

    // 실패 이벤트 재처리 스케줄 주기 (ms 단위)
    public static final long FAILED_EVENT_RETRY_FIXED_DELAY_MS = 60_000L;

    // PROCESSING 상태 스턱 감지 타임아웃 (분)
    public static final int STUCK_PROCESSING_TIMEOUT_MINUTES = 5;

    // PROCESSING 상태 스턱 복구 스케줄 주기 (ms 단위)
    public static final long STUCK_PROCESSING_RECOVERY_FIXED_DELAY_MS = 300_000L;

    // === Outbox 릴레이 관련 ===

    // Outbox 릴레이 폴링 주기 (ms 단위)
    public static final long OUTBOX_RELAY_FIXED_DELAY_MS = 5_000L;

    // Outbox 릴레이 배치 크기
    public static final int OUTBOX_RELAY_BATCH_SIZE = 100;

    // Outbox 최대 재시도 횟수
    public static final int OUTBOX_MAX_RETRY_COUNT = 5;

    // Outbox 처리 완료 레코드 보관 기간 (일)
    public static final int OUTBOX_CLEANUP_RETENTION_DAYS = 7;
}
