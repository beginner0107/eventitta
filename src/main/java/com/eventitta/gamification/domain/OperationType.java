package com.eventitta.gamification.domain;

/**
 * 활동 이벤트의 작업 유형을 나타냅니다.
 */
public enum OperationType {
    /**
     * 활동 기록 (포인트 추가)
     */
    RECORD,

    /**
     * 활동 취소 (포인트 회수)
     */
    REVOKE
}
