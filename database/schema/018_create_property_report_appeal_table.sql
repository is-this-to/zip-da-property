CREATE TABLE `property_report_appeal`
(
    `appeal_id`            BIGINT        NOT NULL AUTO_INCREMENT
        COMMENT '운영조치 이의신청 내부 ID',
    `report_id`            BIGINT        NOT NULL
        COMMENT '논리 참조: property_report.report_id',
    `appellant_member_id`  BIGINT        NOT NULL
        COMMENT '논리 참조: Member 서비스 신청자 ID',
    `detail`               VARCHAR(2000) NOT NULL
        COMMENT '이의신청 상세 내용',
    `status`               VARCHAR(30)   NOT NULL
        COMMENT '이의신청 처리 상태',
    `reviewer_member_id`   BIGINT        NULL
        COMMENT '논리 참조: Member 서비스 검토자 ID',
    `review_reason`        VARCHAR(1000) NULL
        COMMENT '검토 사유',
    `reviewed_at`          DATETIME(6)   NULL
        COMMENT '검토 완료 UTC 시각',
    `version`              BIGINT        NOT NULL
        COMMENT 'JPA 낙관적 잠금 버전',

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

    PRIMARY KEY (`appeal_id`),

    CONSTRAINT `uq_property_report_appeal_report`
        UNIQUE (`report_id`),

    CONSTRAINT `chk_property_report_appeal_status`
        CHECK (`status` IN (
                            'SUBMITTED',
                            'IN_REVIEW',
                            'ACCEPTED',
                            'REJECTED'
            )),

    CONSTRAINT `chk_property_report_appeal_action_source`
        CHECK (`action_source` IN (
                                    'MEMBER',
                                    'SYSTEM',
                                    'BATCH'
            )),

    INDEX `idx_property_report_appeal_report_status`
        (`report_id`, `status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = 'PR-042 운영조치 이의신청';
