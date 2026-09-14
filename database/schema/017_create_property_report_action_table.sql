CREATE TABLE `property_report_action`
(
    `action_id`            BIGINT        NOT NULL AUTO_INCREMENT
        COMMENT '신고 운영조치 내부 ID',
    `report_id`            BIGINT        NOT NULL
        COMMENT '논리 참조: property_report.report_id',
    `property_id`          BIGINT        NOT NULL
        COMMENT '논리 참조: property.property_id',
    `action_code`          VARCHAR(50)   NOT NULL
        COMMENT '신고 운영조치 코드',
    `reason`               VARCHAR(1000) NOT NULL
        COMMENT '운영조치 사유',
    `actor_member_id`      BIGINT        NOT NULL
        COMMENT '운영조치를 실행한 관리자 Member ID',
    `actor_role`           VARCHAR(30)   NOT NULL
        COMMENT '운영조치 실행 당시 관리자 역할',
    `trace_id`             VARCHAR(100)  NOT NULL
        COMMENT '요청 로그와 연결하기 위한 Trace ID',
    `executed_at`          DATETIME(6)   NOT NULL
        COMMENT '운영조치가 실행된 UTC 시각',

    `created_at`           DATETIME(6)   NOT NULL,
    `created_by_member_id` BIGINT        NULL,
    `created_by_role`      VARCHAR(30)   NULL,
    `updated_at`           DATETIME(6)   NOT NULL,
    `updated_by_member_id` BIGINT        NULL,
    `updated_by_role`      VARCHAR(30)   NULL,
    `deleted_at`           DATETIME(6)   NULL,
    `deleted_by_member_id` BIGINT        NULL,
    `deleted_by_role`      VARCHAR(30)   NULL,
    `delete_reason`        VARCHAR(500)  NULL,
    `action_source`        VARCHAR(20)   NOT NULL,

    PRIMARY KEY (`action_id`),

    CONSTRAINT `chk_property_report_action_code`
        CHECK (`action_code` IN (
                                 'HIDE_PROPERTY',
                                 'RESTORE_PROPERTY',
                                 'REQUEST_CORRECTION',
                                 'REQUEST_MEMBER_SANCTION'
            )),

    CONSTRAINT `chk_property_report_action_source`
        CHECK (`action_source` IN (
                                    'MEMBER',
                                    'SYSTEM',
                                    'BATCH'
            )),

    INDEX `idx_property_report_action_report_timeline`
        (`report_id`, `executed_at`, `action_id`),

    INDEX `idx_property_report_action_property_timeline`
        (`property_id`, `executed_at`, `action_id`),

    INDEX `idx_property_report_action_actor_timeline`
        (`actor_member_id`, `executed_at`, `action_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = 'PR-046 신고 운영조치 append-only 이력';
