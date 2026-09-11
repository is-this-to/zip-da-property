CREATE TABLE `property_image`
(
    `property_image_id`        BIGINT       NOT NULL AUTO_INCREMENT
        COMMENT '매물 이미지 연결 ID. MySQL AUTO_INCREMENT 생성',

    `property_id`              BIGINT       NOT NULL
        COMMENT '논리 참조: property.property_id',

    `property_file_id`         BIGINT       NOT NULL
        COMMENT '논리 참조: property_file.property_file_id',

    `sort_order`               INT          NOT NULL
        COMMENT '이미지 표시 순서. 0부터 시작',

    `is_representative`        BOOLEAN      NOT NULL
        COMMENT '대표 이미지 여부',

    `alt_text`                 VARCHAR(300) NULL
        COMMENT '접근성 대체 텍스트',

    `active_representative_key` BIGINT
        GENERATED ALWAYS AS (
            CASE
                WHEN `deleted_at` IS NULL
                    AND `is_representative` = 1
                    THEN `property_id`
                ELSE NULL
                END
            ) STORED
        COMMENT '활성 대표 이미지만 property_id를 가지는 generated key',

    `active_property_file_key` BIGINT
        GENERATED ALWAYS AS (
            CASE
                WHEN `deleted_at` IS NULL
                    THEN `property_file_id`
                ELSE NULL
                END
            ) STORED
        COMMENT '활성 이미지에서 유일한 property_file_id generated key',

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

    PRIMARY KEY (`property_image_id`),

    CONSTRAINT `uq_property_image_01`
        UNIQUE (`active_representative_key`),

    CONSTRAINT `uq_property_image_02`
        UNIQUE (`active_property_file_key`),

    CONSTRAINT `chk_property_image_sort_order`
        CHECK (`sort_order` >= 0),

    CONSTRAINT `chk_property_image_is_representative_bool`
        CHECK (`is_representative` IN (0, 1)),

    CONSTRAINT `chk_property_image_action_source_enum`
        CHECK (
            `action_source` IN (
                                'MEMBER',
                                'SYSTEM',
                                'BATCH'
                )
            ),

    INDEX `idx_property_image_property_active`
        (`property_id`, `deleted_at`, `sort_order`),

    INDEX `idx_property_image_file_active`
        (`property_file_id`, `deleted_at`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '매물 이미지 파일 연결과 표시 순서';
