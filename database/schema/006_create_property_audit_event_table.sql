CREATE TABLE `property_audit_event`
(
    `audit_event_id`       BIGINT         NOT NULL AUTO_INCREMENT
        COMMENT '감사 이벤트 내부 ID',
    `target_type`          VARCHAR(50)    NOT NULL
        COMMENT '감사 대상 종류. 예: PROPERTY, REPORT, EXACT_LOCATION',
    `target_id`            VARCHAR(100)   NOT NULL
        COMMENT '감사 대상의 식별자',
    `action_code`          VARCHAR(100)   NOT NULL
        COMMENT '감사 행위 코드. 예: PROPERTY_CREATED, VIEW_EXACT_LOCATION',
    `actor_member_id`      BIGINT         NULL
        COMMENT '행위를 수행한 회원 ID',
    `actor_role`           VARCHAR(30)    NULL
        COMMENT '행위 수행 당시 회원 역할',
    `action_source`        VARCHAR(20)    NOT NULL
        COMMENT '행위 발생 주체: MEMBER, SYSTEM, BATCH',
    `reason`               VARCHAR(1000)  NULL
        COMMENT '행위를 수행한 사유',
    `trace_id`             VARCHAR(100)   NOT NULL
        COMMENT '요청 로그와 연결하기 위한 Trace ID',
    `client_ip_hash`       CHAR(64)       NULL
        COMMENT '필요한 경우 원본 IP 대신 저장하는 SHA-256 해시',
    `occurred_at`          DATETIME(6)    NOT NULL
        COMMENT '감사 대상 행위가 실제로 발생한 UTC 시각',
    `created_at`           DATETIME(6)    NOT NULL
        COMMENT '감사 이벤트 행이 생성된 UTC 시각',
    `created_by_member_id` BIGINT         NULL
        COMMENT '감사 이벤트 행을 생성한 회원 ID',
    `created_by_role`      VARCHAR(30)    NULL
        COMMENT '감사 이벤트 생성 당시 회원 역할',
    `updated_at`           DATETIME(6)    NOT NULL
        COMMENT '감사 이벤트 행의 마지막 수정 UTC 시각',
    `updated_by_member_id` BIGINT         NULL
        COMMENT '감사 이벤트 행을 마지막으로 수정한 회원 ID',
    `updated_by_role`      VARCHAR(30)    NULL
        COMMENT '마지막 수정 당시 회원 역할',
    `deleted_at`           DATETIME(6)    NULL
        COMMENT '소프트 삭제 UTC 시각. NULL이면 삭제되지 않은 상태',
    `deleted_by_member_id` BIGINT         NULL
        COMMENT '소프트 삭제한 회원 ID',
    `deleted_by_role`      VARCHAR(30)    NULL
        COMMENT '소프트 삭제 당시 회원 역할',
    `delete_reason`        VARCHAR(500)   NULL
        COMMENT '소프트 삭제 사유',

    PRIMARY KEY (`audit_event_id`),

    CONSTRAINT `chk_property_audit_event_action_source_enum`
        CHECK (
            `action_source` IN (
                                'MEMBER',
                                'SYSTEM',
                                'BATCH'
                )
            ),

    INDEX `idx_audit_target_timeline`
        (`target_type`, `target_id`, `occurred_at`),

    INDEX `idx_audit_actor_timeline`
        (`actor_member_id`, `occurred_at`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '민감정보 조회 및 운영 행위 감사 기록';
