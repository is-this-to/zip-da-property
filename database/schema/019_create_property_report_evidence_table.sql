CREATE TABLE `property_report_evidence`
(
    `report_evidence_id`    BIGINT       NOT NULL AUTO_INCREMENT
        COMMENT '신고 증빙 연결 ID. MySQL AUTO_INCREMENT 생성',
    `report_id`             BIGINT       NOT NULL
        COMMENT '논리 참조: property_report.report_id',
    `property_file_id`      BIGINT       NOT NULL
        COMMENT '논리 참조: property_file.property_file_id',
    `evidence_type`         VARCHAR(30)  NOT NULL
        COMMENT '현재 이미지 증빙 유형(SCREENSHOT)',
    `sort_order`            INT          NOT NULL
        COMMENT '요청 evidenceFileIds 순서. 0부터 시작',
    `active_report_file_key` VARCHAR(50)
        GENERATED ALWAYS AS (
            CASE
                WHEN `deleted_at` IS NULL
                    THEN CONCAT(
                        CAST(`report_id` AS CHAR),
                        ':',
                        CAST(`property_file_id` AS CHAR)
                    )
                ELSE NULL
            END
        ) STORED
        COMMENT '활성 신고+파일 연결을 DB가 계산하는 generated key',

    `created_at`            DATETIME(6)  NOT NULL,
    `created_by_member_id`  BIGINT       NULL,
    `created_by_role`       VARCHAR(30)  NULL,
    `updated_at`            DATETIME(6)  NOT NULL,
    `updated_by_member_id`  BIGINT       NULL,
    `updated_by_role`       VARCHAR(30)  NULL,
    `deleted_at`            DATETIME(6)  NULL,
    `deleted_by_member_id`  BIGINT       NULL,
    `deleted_by_role`       VARCHAR(30)  NULL,
    `delete_reason`         VARCHAR(500) NULL,
    `action_source`         VARCHAR(20)  NOT NULL,

    PRIMARY KEY (`report_evidence_id`),

    CONSTRAINT `uq_property_report_evidence_01`
        UNIQUE (`active_report_file_key`),

    CONSTRAINT `chk_property_report_evidence_type_enum`
        CHECK (`evidence_type` IN ('SCREENSHOT')),

    CONSTRAINT `chk_property_report_evidence_sort_order`
        CHECK (`sort_order` >= 0),

    CONSTRAINT `chk_property_report_evidence_action_source_enum`
        CHECK (`action_source` IN ('MEMBER', 'SYSTEM', 'BATCH')),

    INDEX `idx_property_report_evidence_report_active`
        (`report_id`, `deleted_at`, `sort_order`),

    INDEX `idx_property_report_evidence_file_active`
        (`property_file_id`, `deleted_at`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '매물 신고에 첨부한 증빙 파일 연결';
