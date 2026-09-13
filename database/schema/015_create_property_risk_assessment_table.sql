CREATE TABLE `property_risk_assessment`
(
    `risk_assessment_id`      BIGINT        NOT NULL AUTO_INCREMENT
        COMMENT '등록 전 위험검사 이력 ID',
    `property_id`             BIGINT        NOT NULL
        COMMENT '등록 시도에 미리 발급한 매물 TSID. 차단 시 property 행은 생성되지 않을 수 있음',
    `publisher_member_id`     BIGINT        NOT NULL
        COMMENT '등록을 시도한 Member 외부 ID',
    `normalized_address_hash` CHAR(64)      NOT NULL
        COMMENT '정규화 표준주소 SHA-256. 공개 DTO와 로그에 노출 금지',
    `matched_rule_codes_json` JSON          NOT NULL
        COMMENT '자동판정에 일치한 규칙 코드 목록',
    `risk_score`              DECIMAL(5, 2) NOT NULL
        COMMENT '0 이상 100 이하의 등록 위험점수',
    `decision`                VARCHAR(30)   NOT NULL
        COMMENT 'PASS, IN_REVIEW, BLOCKED',
    `duplicate_property_id`   BIGINT        NULL
        COMMENT '대표 중복 후보 매물 ID',
    `evaluated_at`            DATETIME(6)   NOT NULL
        COMMENT '자동 위험검사 완료 UTC 시각',
    `created_at`              DATETIME(6)   NOT NULL,
    `created_by_member_id`    BIGINT        NULL,
    `created_by_role`         VARCHAR(30)   NULL,
    `updated_at`              DATETIME(6)   NOT NULL,
    `updated_by_member_id`    BIGINT        NULL,
    `updated_by_role`         VARCHAR(30)   NULL,
    `deleted_at`              DATETIME(6)   NULL,
    `deleted_by_member_id`    BIGINT        NULL,
    `deleted_by_role`         VARCHAR(30)   NULL,
    `delete_reason`           VARCHAR(500)  NULL,
    `action_source`           VARCHAR(20)   NOT NULL,

    PRIMARY KEY (`risk_assessment_id`),

    CONSTRAINT `chk_property_risk_assessment_score`
        CHECK (`risk_score` >= 0 AND `risk_score` <= 100),

    CONSTRAINT `chk_property_risk_assessment_decision`
        CHECK (`decision` IN ('PASS', 'IN_REVIEW', 'BLOCKED')),

    CONSTRAINT `chk_property_risk_assessment_action_source`
        CHECK (`action_source` IN ('MEMBER', 'SYSTEM', 'BATCH')),

    INDEX `idx_property_risk_assessment_property`
        (`property_id`, `evaluated_at`),

    INDEX `idx_property_risk_assessment_publisher`
        (`publisher_member_id`, `evaluated_at`),

    INDEX `idx_property_risk_assessment_decision`
        (`decision`, `evaluated_at`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = 'FR-PR-038 등록 전 허위·중복 위험검사 append-only 이력';
