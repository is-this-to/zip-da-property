CREATE TABLE `property_option_code`
(
    `option_code_id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '내부 옵션 기준 ID. MySQL AUTO_INCREMENT 생성',
    `option_code`             VARCHAR(50)  NOT NULL COMMENT 'API·DB에서 사용하는 영문 코드',
    `option_name`             VARCHAR(100) NOT NULL COMMENT '사용자 화면 한글 표시명',
    `option_category`         VARCHAR(30)  NOT NULL COMMENT 'APPLIANCE, FURNITURE, SECURITY, STRUCTURE, LIVING, ETC',
    `description`             VARCHAR(500) NULL COMMENT '등록자와 개발자를 위한 정의',
    `is_filterable`           BOOLEAN      NOT NULL COMMENT '검색 필터 제공 여부. 핵심 정형필드와 중복 필터 금지',
    `is_detail_visible`       BOOLEAN      NOT NULL COMMENT '매물 상세 노출 여부',
    `is_registration_enabled` BOOLEAN      NOT NULL COMMENT '등록·수정 화면 선택 가능 여부',
    `display_order`           INT          NOT NULL COMMENT '카테고리 내부 표시 순서',
    `is_active`               BOOLEAN      NOT NULL COMMENT '신규 사용 가능 여부',

    `created_at`              DATETIME(6)  NOT NULL,
    `created_by_member_id`    BIGINT       NULL,
    `created_by_role`         VARCHAR(30)  NULL,
    `updated_at`              DATETIME(6)  NOT NULL,
    `updated_by_member_id`    BIGINT       NULL,
    `updated_by_role`         VARCHAR(30)  NULL,
    `deleted_at`              DATETIME(6)  NULL,
    `deleted_by_member_id`    BIGINT       NULL,
    `deleted_by_role`         VARCHAR(30)  NULL,
    `delete_reason`           VARCHAR(500) NULL,
    `action_source`           VARCHAR(20)  NOT NULL,

    PRIMARY KEY (`option_code_id`),

    CONSTRAINT `uq_property_option_code_01`
        UNIQUE (`option_code`),

    CONSTRAINT `chk_property_option_code_category_enum`
        CHECK (`option_category` IN (
                                     'APPLIANCE',
                                     'FURNITURE',
                                     'SECURITY',
                                     'STRUCTURE',
                                     'LIVING',
                                     'ETC'
            )),

    CONSTRAINT `chk_property_option_code_display_order`
        CHECK (`display_order` >= 0),

    CONSTRAINT `chk_property_option_code_is_filterable_bool`
        CHECK (`is_filterable` IN (0, 1)),

    CONSTRAINT `chk_property_option_code_is_detail_visible_bool`
        CHECK (`is_detail_visible` IN (0, 1)),

    CONSTRAINT `chk_property_option_code_is_registration_enabled_bool`
        CHECK (`is_registration_enabled` IN (0, 1)),

    CONSTRAINT `chk_property_option_code_is_active_bool`
        CHECK (`is_active` IN (0, 1)),

    CONSTRAINT `chk_property_option_code_action_source_enum`
        CHECK (`action_source` IN (
                                   'MEMBER',
                                   'SYSTEM',
                                   'BATCH'
            )),

    INDEX `idx_property_option_code_active`
        (`deleted_at`, `is_active`, `is_registration_enabled`, `display_order`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '매물 옵션 코드 기준정보';


CREATE TABLE `property_type_option`
(
    `property_type_option_id` BIGINT       NOT NULL AUTO_INCREMENT COMMENT '유형-옵션 정책 ID. MySQL AUTO_INCREMENT 생성',
    `property_type`           VARCHAR(30)  NOT NULL COMMENT 'APARTMENT, OFFICETEL, VILLA, ROOM',
    `option_code_id`          BIGINT       NOT NULL COMMENT '허용할 옵션 기준',
    `is_required`             BOOLEAN      NOT NULL COMMENT '해당 유형 등록 시 필수 여부',
    `default_value`           VARCHAR(300) NULL COMMENT '있음 또는 없음. 미확인 시 NULL',
    `display_order`           INT          NOT NULL COMMENT '유형별 등록 화면 순서',

    `created_at`              DATETIME(6)  NOT NULL,
    `created_by_member_id`    BIGINT       NULL,
    `created_by_role`         VARCHAR(30)  NULL,
    `updated_at`              DATETIME(6)  NOT NULL,
    `updated_by_member_id`    BIGINT       NULL,
    `updated_by_role`         VARCHAR(30)  NULL,
    `deleted_at`              DATETIME(6)  NULL,
    `deleted_by_member_id`    BIGINT       NULL,
    `deleted_by_role`         VARCHAR(30)  NULL,
    `delete_reason`           VARCHAR(500) NULL,
    `action_source`           VARCHAR(20)  NOT NULL,

    PRIMARY KEY (`property_type_option_id`),

    CONSTRAINT `chk_property_type_option_property_type_enum`
        CHECK (`property_type` IN (
                                   'APARTMENT',
                                   'OFFICETEL',
                                   'VILLA',
                                   'ROOM'
            )),

    CONSTRAINT `chk_property_type_option_display_order`
        CHECK (`display_order` >= 0),

    CONSTRAINT `chk_property_type_option_is_required_bool`
        CHECK (`is_required` IN (0, 1)),

    CONSTRAINT `chk_property_type_option_default_value`
        CHECK (`default_value` IS NULL OR `default_value` IN ('있음', '없음')),

    CONSTRAINT `chk_property_type_option_action_source_enum`
        CHECK (`action_source` IN (
                                   'MEMBER',
                                   'SYSTEM',
                                   'BATCH'
            )),

    INDEX `idx_property_type_option_active` (`property_type`, `deleted_at`, `display_order`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '매물 유형별 허용 옵션';


CREATE TABLE `property_option`
(
    `property_option_id`     BIGINT        NOT NULL AUTO_INCREMENT COMMENT '옵션 연결 ID. MySQL AUTO_INCREMENT 생성',
    `property_id`            BIGINT        NOT NULL COMMENT '대상 매물',
    `option_code_id`         BIGINT        NOT NULL COMMENT 'property_option_code 기준 ID',

    `option_value`           VARCHAR(300) NOT NULL COMMENT '옵션 값: 있음 또는 없음',

    `display_order`          INT           NOT NULL COMMENT '상세 화면 표시 순서',
    `verified`               BOOLEAN       NOT NULL COMMENT '증빙·관리자 확인 여부',

    `created_at`             DATETIME(6)   NOT NULL,
    `created_by_member_id`   BIGINT        NULL,
    `created_by_role`        VARCHAR(30)   NULL,
    `updated_at`             DATETIME(6)   NOT NULL,
    `updated_by_member_id`   BIGINT        NULL,
    `updated_by_role`        VARCHAR(30)   NULL,
    `deleted_at`             DATETIME(6)   NULL,
    `deleted_by_member_id`   BIGINT        NULL,
    `deleted_by_role`        VARCHAR(30)   NULL,
    `delete_reason`          VARCHAR(500)  NULL,
    `action_source`          VARCHAR(20)   NOT NULL,

    PRIMARY KEY (`property_option_id`),

    CONSTRAINT `chk_property_option_value`
        CHECK (`option_value` IN ('있음', '없음')),

    CONSTRAINT `chk_property_option_display_order`
        CHECK (`display_order` >= 0),

    CONSTRAINT `chk_property_option_verified_bool`
        CHECK (`verified` IN (0, 1)),

    CONSTRAINT `chk_property_option_action_source_enum`
        CHECK (`action_source` IN (
                                   'MEMBER',
                                   'SYSTEM',
                                   'BATCH'
            )),

    INDEX `idx_property_option_active` (`property_id`, `deleted_at`, `display_order`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '매물별 선택 옵션 현재값';


CREATE TABLE `property_option_history`
(
    `property_option_history_id` BIGINT        NOT NULL AUTO_INCREMENT COMMENT '옵션 변경 이력 ID. MySQL AUTO_INCREMENT 생성',
    `property_revision_id`       BIGINT        NOT NULL COMMENT '이 옵션 변경이 포함된 매물 revision',
    `property_option_id`         BIGINT        NOT NULL COMMENT '변경된 property_option 행',
    `option_code_id`             BIGINT        NOT NULL COMMENT '변경 당시 옵션 코드',

    `change_type`                VARCHAR(30)   NOT NULL COMMENT 'CREATE, UPDATE, SOFT_DELETE, RESTORE',
    `changed_fields`            VARCHAR(500) NOT NULL COMMENT '변경 필드명 목록. 쉼표 구분 문자열',

    `before_value`              VARCHAR(300) NULL COMMENT '변경 전 옵션 값: 있음 또는 없음',
    `after_value`               VARCHAR(300) NULL COMMENT '변경 후 옵션 값: 있음 또는 없음',

    `before_display_order`       INT           NULL COMMENT '변경 전 표시 순서',
    `after_display_order`        INT           NULL COMMENT '변경 후 표시 순서',

    `before_verified`            BOOLEAN       NULL COMMENT '변경 전 검증 여부',
    `after_verified`             BOOLEAN       NULL COMMENT '변경 후 검증 여부',

    `before_deleted_at`          DATETIME(6)   NULL COMMENT '변경 전 소프트 삭제 시각',
    `after_deleted_at`           DATETIME(6)   NULL COMMENT '변경 후 소프트 삭제 시각',

    `occurred_at`                DATETIME(6)   NOT NULL COMMENT '옵션 변경이 실제 완료된 UTC 시각',

    `created_at`                 DATETIME(6)   NOT NULL,
    `created_by_member_id`       BIGINT        NULL,
    `created_by_role`            VARCHAR(30)   NULL,
    `updated_at`                 DATETIME(6)   NOT NULL,
    `updated_by_member_id`       BIGINT        NULL,
    `updated_by_role`            VARCHAR(30)   NULL,
    `deleted_at`                 DATETIME(6)   NULL,
    `deleted_by_member_id`       BIGINT        NULL,
    `deleted_by_role`            VARCHAR(30)   NULL,
    `delete_reason`              VARCHAR(500)  NULL,
    `action_source`              VARCHAR(20)   NOT NULL,

    PRIMARY KEY (`property_option_history_id`),

    CONSTRAINT `uq_property_option_history_01` UNIQUE (`property_revision_id`, `property_option_id`),

    CONSTRAINT `chk_property_option_history_before_display_order`
        CHECK (
            `before_display_order` IS NULL
                OR `before_display_order` >= 0
            ),

    CONSTRAINT `chk_property_option_history_after_display_order`
        CHECK (
            `after_display_order` IS NULL
                OR `after_display_order` >= 0
            ),

    CONSTRAINT `chk_property_option_history_before_value`
        CHECK (`before_value` IS NULL OR `before_value` IN ('있음', '없음')),

    CONSTRAINT `chk_property_option_history_after_value`
        CHECK (`after_value` IS NULL OR `after_value` IN ('있음', '없음')),

    CONSTRAINT `chk_property_option_history_before_verified_bool`
        CHECK (
            `before_verified` IS NULL
                OR `before_verified` IN (0, 1)
            ),

    CONSTRAINT `chk_property_option_history_after_verified_bool`
        CHECK (
            `after_verified` IS NULL
                OR `after_verified` IN (0, 1)
            ),

    CONSTRAINT `chk_property_option_history_change_type_enum`
        CHECK (`change_type` IN (
                                 'CREATE',
                                 'UPDATE',
                                 'SOFT_DELETE',
                                 'RESTORE'
            )),

    CONSTRAINT `chk_property_option_history_action_source_enum`
        CHECK (`action_source` IN (
                                   'MEMBER',
                                   'SYSTEM',
                                   'BATCH'
            )),

    INDEX `idx_property_option_history_timeline` (`property_option_id`, `occurred_at`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '매물 옵션 값 변경 전·후 이력';
