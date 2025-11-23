-- V11: Create table for persisting failed activity events

CREATE TABLE failed_activity_events (
          id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '고유 식별자(PK)',
          user_id BIGINT NOT NULL COMMENT '이벤트가 발생한 사용자 ID',
          activity_type VARCHAR(50) NOT NULL COMMENT '활동 유형(예: LIKE, COMMENT, FOLLOW 등)',
          target_id BIGINT COMMENT '대상 리소스 ID(활동이 가리키는 엔터티, 없을 수 있음)',
          failed_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '실패가 기록된 시각',
          retry_count INT NOT NULL DEFAULT 0 COMMENT '재시도 횟수',
          error_message VARCHAR(1000) COMMENT '실패 사유 및 에러 메시지',
          status VARCHAR(50) NOT NULL DEFAULT 'PENDING' COMMENT '처리 상태(PENDING: 대기, PROCESSING: 처리 중, PROCESSED: 처리 완료, FAILED: 영구 실패)',
          processed_at DATETIME(6) COMMENT '처리 완료 시각(성공 또는 영구 실패 시점)',

          created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '레코드 생성 시각',
          updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '레코드 수정 시각',

          INDEX idx_failed_event_status (status),
          INDEX idx_failed_event_user (user_id),
          INDEX idx_failed_event_created (created_at)
) COMMENT='실패한 활동 이벤트를 저장하고 재처리 상태를 관리하는 테이블';
