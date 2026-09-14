CREATE TABLE `property_report`
(
    `report_id`           BIGINT        NOT NULL COMMENT 'Java TSID 생성 신고 ID',
    `property_id`         BIGINT        NOT NULL COMMENT '논리 참조: property.property_id',
    `reporter_member_id`  BIGINT        NOT NULL COMMENT '논리 참조: Member 서비스 신고자 ID',
    `reason_code`         VARCHAR(50)   NOT NULL COMMENT '신고 사유 코드',
    `status`              VARCHAR(30)   NOT NULL COMMENT '신고 처리 상태',
    `detail`              VARCHAR(1000) NOT NULL COMMENT '신고 상세 내용',
    `risk_score`          DECIMAL(5, 2) NULL COMMENT '관리자 판단 위험 점수',
    `assigned_admin_id`   BIGINT        NULL COMMENT '논리 참조: 담당 관리자 ID',
    `version`             BIGINT        NOT NULL COMMENT 'JPA 낙관적 잠금 버전',
    `active_report_key`   VARCHAR(150)
        GENERATED ALWAYS AS (
            CASE
                WHEN `deleted_at` IS NULL
                    AND `status` IN (
                        'RECEIVED',
                        'TRIAGED',
                        'IN_REVIEW',
                        'ACTIONED'
                    )
                    THEN CONCAT(
                        CAST(`reporter_member_id` AS CHAR),
                        ':',
                        CAST(`property_id` AS CHAR),
                        ':',
                        CAST(`reason_code` AS CHAR)
                    )
                ELSE NULL
                END
            ) STORED
        COMMENT '진행 중 신고자+매물+사유 조합을 DB가 계산하는 generated key',

    `created_at`           DATETIME(6)  NOT NULL,
    `created_by_member_id` BIGINT       NULL,
    `created_by_role`      VARCHAR(30)  NULL,
    `updated_at`           DATETIME(6)  NOT NULL,
    `updated_by_member_id` BIGINT       NULL,
    `updated_by_role`      VARCHAR(30)  NULL,
    `deleted_at`           DATETIME(6)  NULL,
    `deleted_by_member_id` BIGINT       NULL,
    `deleted_by_role`      VARCHAR(30)  NULL,
    `delete_reason`        VARCHAR(500) NULL,
    `action_source`        VARCHAR(20)  NOT NULL,

    PRIMARY KEY (`report_id`),

    CONSTRAINT `uq_property_report_01`
        UNIQUE (`active_report_key`),

    CONSTRAINT `chk_property_report_reason_code_enum`
        CHECK (`reason_code` IN (
                                  'FALSE_INFO',
                                  'DUPLICATE',
                                  'UNAVAILABLE',
                                  'PRICE_MISMATCH',
                                  'OTHER'
            )),

    CONSTRAINT `chk_property_report_status_enum`
        CHECK (`status` IN (
                             'RECEIVED',
                             'TRIAGED',
                             'IN_REVIEW',
                             'ACTIONED',
                             'REJECTED',
                             'CLOSED'
            )),

    CONSTRAINT `chk_property_report_action_source_enum`
        CHECK (`action_source` IN (
                                    'MEMBER',
                                    'SYSTEM',
                                    'BATCH'
            )),

    INDEX `idx_property_report_reporter_created`
        (`reporter_member_id`, `created_at`),

    INDEX `idx_property_report_property_created`
        (`property_id`, `created_at` DESC)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '매물 신고 접수 및 처리 상태';
