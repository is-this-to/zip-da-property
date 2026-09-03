CREATE TABLE `property_favorite`
(
    `favorite_id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '찜 ID. MySQL AUTO_INCREMENT 생성',
    `member_id`            BIGINT       NOT NULL COMMENT '논리 참조: Member 서비스 회원 ID',
    `property_id`          BIGINT       NOT NULL COMMENT '논리 참조: property.property_id',
    `active_favorite_key`  VARCHAR(150)
        GENERATED ALWAYS AS (
            CASE
                WHEN `deleted_at` IS NULL
                    THEN CONCAT(
                        CAST(`member_id` AS CHAR),
                        ':',
                        CAST(`property_id` AS CHAR)
                         )
                ELSE NULL
                END
            ) STORED
        COMMENT '활성 회원+매물 찜 조합을 DB가 계산하는 generated key',
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

    PRIMARY KEY (`favorite_id`),

    CONSTRAINT `uq_property_favorite_01`
        UNIQUE (`active_favorite_key`),

    CONSTRAINT `chk_property_favorite_action_source_enum`
        CHECK (`action_source` IN (
                                   'MEMBER',
                                   'SYSTEM',
                                   'BATCH'
            )),

    INDEX `idx_favorite_member_active`
        (`member_id`, `deleted_at`, `created_at` DESC, `property_id` DESC),

    INDEX `idx_favorite_property_active`
        (`property_id`, `deleted_at`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '회원별 매물 찜';