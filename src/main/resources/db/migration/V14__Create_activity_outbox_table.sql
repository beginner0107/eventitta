-- V14: Transactional Outbox 패턴을 위한 activity_outbox 테이블 생성
-- 비즈니스 트랜잭션과 동일한 TX 내에서 이벤트를 영속화하여 JVM 크래시 시 이벤트 유실 방지

CREATE TABLE activity_outbox (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '고유 식별자(PK)',
    idempotency_key VARCHAR(100) NOT NULL COMMENT '멱등키 (중복 처리 방지)',
    user_id         BIGINT       NOT NULL COMMENT '이벤트 대상 사용자 ID',
    activity_type   VARCHAR(50)  NOT NULL COMMENT '활동 유형 (CREATE_POST, LIKE_POST 등)',
    operation_type  VARCHAR(20)  NOT NULL COMMENT '작업 유형 (RECORD, REVOKE)',
    target_id       BIGINT                COMMENT '대상 리소스 ID (없을 수 있음)',
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT '처리 상태 (PENDING, PROCESSING, DONE, FAILED)',
    retry_count     INT          NOT NULL DEFAULT 0 COMMENT '재시도 횟수',
    error_message   VARCHAR(1000)         COMMENT '실패 사유 및 에러 메시지',
    processed_at    DATETIME(6)           COMMENT '처리 완료 시각',

    created_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '레코드 생성 시각',
    updated_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '레코드 수정 시각',

    UNIQUE KEY uk_outbox_idempotency (idempotency_key),
    INDEX idx_outbox_status_created (status, created_at),
    INDEX idx_outbox_user (user_id)
) COMMENT='Transactional Outbox - 비즈니스 TX와 동일 TX 내에서 이벤트를 영속화';
