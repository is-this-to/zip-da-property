CREATE TABLE `property_file`
(
    `property_file_id`      BIGINT       NOT NULL
        COMMENT '애플리케이션에서 생성한 파일 TSID',
    `upload_session_id`     VARCHAR(36)  NOT NULL
        COMMENT '업로드 요청 단위 세션 식별자',
    `owner_member_id`       BIGINT       NOT NULL
        COMMENT '파일 소유 회원 ID',
    `original_file_name`    VARCHAR(255) NOT NULL
        COMMENT '경로 정보가 제거된 원본 파일명',
    `file_size`             BIGINT       NOT NULL
        COMMENT '요청된 파일 크기(byte)',
    `object_key`            VARCHAR(500) NOT NULL
        COMMENT 'MinIO 객체 키',
    `checksum`              VARCHAR(64)  NULL
        COMMENT '업로드 완료 검증 시 저장할 체크섬',
    `expires_at`            DATETIME(6)  NOT NULL
        COMMENT '업로드 세션 만료 시각',
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

    PRIMARY KEY (`property_file_id`),

    CONSTRAINT `uq_property_file_object_key`
        UNIQUE (`object_key`),

    CONSTRAINT `chk_property_file_size`
        CHECK (`file_size` > 0),

    CONSTRAINT `chk_property_file_expires_at`
        CHECK (`expires_at` >= `created_at`),

    CONSTRAINT `chk_property_file_action_source_enum`
        CHECK (
            `action_source` IN (
                                'MEMBER',
                                'SYSTEM',
                                'BATCH'
                )
            ),

    INDEX `idx_property_file_upload_session`
        (`upload_session_id`),
    INDEX `idx_property_file_owner_member`
        (`owner_member_id`),
    INDEX `idx_property_file_expires_at`
        (`expires_at`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '매물 파일 업로드 세션 메타데이터';
