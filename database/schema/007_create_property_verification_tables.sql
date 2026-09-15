CREATE TABLE `property_verification`
(
    `property_verification_id` BIGINT        NOT NULL,
    `property_id`              BIGINT        NOT NULL,
    `verification_type`        VARCHAR(30)   NOT NULL,
    `applicant_member_id`      BIGINT        NOT NULL,
    `status`                   VARCHAR(30)   NOT NULL,
    `submitted_at`             DATETIME(6)   NOT NULL,
    `reviewer_member_id`       BIGINT        NULL,
    `reviewer_role`            VARCHAR(30)   NULL,
    `reviewed_at`              DATETIME(6)   NULL,
    `verified_at`              DATETIME(6)   NULL,
    `expires_at`               DATETIME(6)   NULL,
    `result_code`              VARCHAR(50)   NULL,
    `result_reason`            VARCHAR(1000) NULL,
    `verification_version`     INT           NOT NULL,
    `created_at`               DATETIME(6)   NOT NULL,
    `created_by_member_id`     BIGINT        NULL,
    `created_by_role`          VARCHAR(30)   NULL,
    `updated_at`               DATETIME(6)   NOT NULL,
    `updated_by_member_id`     BIGINT        NULL,
    `updated_by_role`          VARCHAR(30)   NULL,
    `deleted_at`               DATETIME(6)   NULL,
    `deleted_by_member_id`     BIGINT        NULL,
    `deleted_by_role`          VARCHAR(30)   NULL,
    `delete_reason`            VARCHAR(500)  NULL,
    `action_source`            VARCHAR(20)   NOT NULL,
    `active_verification_key`  VARCHAR(100)
        GENERATED ALWAYS AS (
            CASE
                WHEN `deleted_at` IS NULL AND `status` = 'IN_REVIEW'
                    THEN CONCAT(`property_id`, ':', `verification_type`)
                ELSE NULL
            END
        ) STORED,

    PRIMARY KEY (`property_verification_id`),
    UNIQUE KEY `uq_property_verification_active` (`active_verification_key`),
    UNIQUE KEY `uq_property_verification_version`
        (`property_id`, `verification_type`, `verification_version`),
    INDEX `idx_property_verification_applicant`
        (`applicant_member_id`, `submitted_at`),
    INDEX `idx_property_verification_review_queue`
        (`status`, `submitted_at`),

    CONSTRAINT `chk_property_verification_type`
        CHECK (`verification_type` IN ('OWNER', 'AGENT_BROKERAGE')),
    CONSTRAINT `chk_property_verification_status`
        CHECK (`status` IN ('IN_REVIEW', 'VERIFIED', 'REJECTED', 'EXPIRED')),
    CONSTRAINT `chk_property_verification_version`
        CHECK (`verification_version` >= 1),
    CONSTRAINT `chk_property_verification_action_source`
        CHECK (`action_source` IN ('MEMBER', 'SYSTEM', 'BATCH'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '매물 소유자 또는 중개사 검증 신청과 심사 결과';

CREATE TABLE `property_verification_evidence`
(
    `verification_evidence_id` BIGINT       NOT NULL,
    `property_verification_id` BIGINT       NOT NULL,
    `property_file_id`         BIGINT       NOT NULL,
    `evidence_type`            VARCHAR(50)  NOT NULL,
    `sort_order`               INT          NOT NULL,
    `created_at`               DATETIME(6)  NOT NULL,
    `created_by_member_id`     BIGINT       NULL,
    `created_by_role`          VARCHAR(30)  NULL,
    `updated_at`               DATETIME(6)  NOT NULL,
    `updated_by_member_id`     BIGINT       NULL,
    `updated_by_role`          VARCHAR(30)  NULL,
    `deleted_at`               DATETIME(6)  NULL,
    `deleted_by_member_id`     BIGINT       NULL,
    `deleted_by_role`          VARCHAR(30)  NULL,
    `delete_reason`            VARCHAR(500) NULL,
    `action_source`            VARCHAR(20)  NOT NULL,

    PRIMARY KEY (`verification_evidence_id`),
    UNIQUE KEY `uq_property_verification_evidence_file`
        (`property_verification_id`, `property_file_id`),
    INDEX `idx_property_verification_evidence_file` (`property_file_id`),

    CONSTRAINT `chk_property_verification_evidence_type`
        CHECK (`evidence_type` IN (
            'REGISTRY_DOCUMENT',
            'OWNERSHIP_CONTRACT',
            'BROKERAGE_REGISTRATION',
            'OTHER'
        )),
    CONSTRAINT `chk_property_verification_evidence_sort_order`
        CHECK (`sort_order` >= 0),
    CONSTRAINT `chk_property_verification_evidence_action_source`
        CHECK (`action_source` IN ('MEMBER', 'SYSTEM', 'BATCH'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '매물 검증 신청에 첨부한 증빙 파일';
