CREATE TABLE `property_idempotency`
(
    `idempotency_id`      BIGINT       NOT NULL AUTO_INCREMENT
        COMMENT '멱등 요청 기록 내부 ID',
    `member_id`           BIGINT       NOT NULL
        COMMENT '요청한 Member 서비스 회원 ID',
    `endpoint_key`        VARCHAR(200) NOT NULL
        COMMENT '요청 API 식별자. 예: POST:/api/property/properties',
    `idempotency_key`     VARCHAR(100) NOT NULL
        COMMENT '클라이언트가 전달한 Idempotency-Key 헤더값',
    `request_hash`        CHAR(64)     NOT NULL
        COMMENT '요청 본문을 정규화한 SHA-256 해시',
    `processing_status`   VARCHAR(20)  NOT NULL
        COMMENT '요청 처리 상태: PROCESSING, COMPLETED, FAILED',
    `resource_type`       VARCHAR(50)  NULL
        COMMENT '생성된 리소스 종류. 예: PROPERTY',
    `resource_id`         BIGINT       NULL
        COMMENT '요청 처리로 생성된 리소스 ID',
    `response_status`     SMALLINT     NULL
        COMMENT '중복 요청에 재사용할 HTTP 상태 코드',
    `response_body_json`  JSON         NULL
        COMMENT '중복 요청에 재사용할 최소 응답 JSON',
    `expires_at`          DATETIME(6)  NOT NULL
        COMMENT '멱등 키 보존 만료 시각',
    `created_at`          DATETIME(6)  NOT NULL,
    `created_by_member_id` BIGINT      NULL,
    `created_by_role`     VARCHAR(30)  NULL,
    `updated_at`          DATETIME(6)  NOT NULL,
    `updated_by_member_id` BIGINT      NULL,
    `updated_by_role`     VARCHAR(30)  NULL,
    `deleted_at`          DATETIME(6)  NULL,
    `deleted_by_member_id` BIGINT      NULL,
    `deleted_by_role`     VARCHAR(30)  NULL,
    `delete_reason`       VARCHAR(500) NULL,
    `action_source`       VARCHAR(20)  NOT NULL,

    PRIMARY KEY (`idempotency_id`),

    CONSTRAINT `uq_property_idempotency_01`
        UNIQUE (
                `member_id`,
                `endpoint_key`,
                `idempotency_key`
            ),

    CONSTRAINT `chk_idempotency_response_status`
        CHECK (
            `response_status` IS NULL
                OR `response_status` BETWEEN 100 AND 599
            ),

    CONSTRAINT `chk_idempotency_expires_at`
        CHECK (`expires_at` >= `created_at`),

    CONSTRAINT `chk_property_idempotency_processing_status_enum`
        CHECK (
            `processing_status` IN (
                                    'PROCESSING',
                                    'COMPLETED',
                                    'FAILED'
                )
            ),

    CONSTRAINT `chk_property_idempotency_action_source_enum`
        CHECK (
            `action_source` IN (
                                'MEMBER',
                                'SYSTEM',
                                'BATCH'
                )
            ),

    INDEX `idx_idempotency_expires`
        (`expires_at`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = 'Property API 멱등 요청 및 응답 기록';
