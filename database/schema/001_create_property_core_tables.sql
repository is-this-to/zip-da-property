CREATE TABLE `property`
(
    `property_id`             BIGINT         NOT NULL,
    `version`                 BIGINT         NOT NULL,
    `region_id`               BIGINT         NOT NULL,
    `apartment_complex_id`    BIGINT         NULL,
    `author_member_id`        BIGINT         NOT NULL,
    `publisher_type`          VARCHAR(30)    NOT NULL,
    `property_type`           VARCHAR(30)    NOT NULL,
    `transaction_type`        VARCHAR(30)    NOT NULL,
    `sale_price`              BIGINT         NULL,
    `deposit`                 BIGINT         NULL,
    `monthly_rent`            BIGINT         NULL,
    `maintenance_fee`         BIGINT         NULL,
    `supply_area`             DECIMAL(10, 2) NULL,
    `exclusive_area`          DECIMAL(10, 2) NOT NULL,
    `room_count`              INT            NULL,
    `bathroom_count`          INT            NULL,
    `floor`                   INT            NULL,
    `total_floor`             INT            NULL,
    `floor_condition`         VARCHAR(30)    NULL,
    `direction`               VARCHAR(20)    NULL,
    `approval_date`           DATE           NULL,
    `building_use`            VARCHAR(100)   NULL,
    `is_parking_available`    BOOLEAN        NULL,
    `has_elevator`            BOOLEAN        NULL,
    `is_pet_allowed`          BOOLEAN        NULL,
    `title`                   VARCHAR(200)   NOT NULL,
    `description`             TEXT           NOT NULL,
    `publication_status`      VARCHAR(30)    NOT NULL,
    `transaction_status`      VARCHAR(30)    NOT NULL,
    `verification_status`     VARCHAR(30)    NOT NULL,
    `risk_score`              DECIMAL(5, 2)  NULL,
    `published_at`            DATETIME(6)    NULL,
    `created_at`              DATETIME(6)    NOT NULL,
    `created_by_member_id`    BIGINT         NULL,
    `created_by_role`         VARCHAR(30)    NULL,
    `updated_at`              DATETIME(6)    NOT NULL,
    `updated_by_member_id`    BIGINT         NULL,
    `updated_by_role`         VARCHAR(30)    NULL,
    `deleted_at`              DATETIME(6)    NULL,
    `deleted_by_member_id`    BIGINT         NULL,
    `deleted_by_role`         VARCHAR(30)    NULL,
    `delete_reason`           VARCHAR(500)   NULL,
    `action_source`           VARCHAR(20)    NOT NULL,

    PRIMARY KEY (`property_id`),

    CONSTRAINT `chk_property_version`
        CHECK (`version` >= 0),

    CONSTRAINT `chk_property_sale_price`
        CHECK (`sale_price` IS NULL OR `sale_price` >= 0),

    CONSTRAINT `chk_property_deposit`
        CHECK (`deposit` IS NULL OR `deposit` >= 0),

    CONSTRAINT `chk_property_monthly_rent`
        CHECK (`monthly_rent` IS NULL OR `monthly_rent` >= 0),

    CONSTRAINT `chk_property_maintenance_fee`
        CHECK (`maintenance_fee` IS NULL OR `maintenance_fee` >= 0),

    CONSTRAINT `chk_property_supply_area`
        CHECK (`supply_area` IS NULL OR `supply_area` > 0),

    CONSTRAINT `chk_property_exclusive_area`
        CHECK (`exclusive_area` > 0),

    CONSTRAINT `chk_property_room_count`
        CHECK (`room_count` IS NULL OR `room_count` >= 0),

    CONSTRAINT `chk_property_bathroom_count`
        CHECK (`bathroom_count` IS NULL OR `bathroom_count` >= 0),

    CONSTRAINT `chk_property_risk_score`
        CHECK (`risk_score` IS NULL OR `risk_score` >= 0),

    CONSTRAINT `chk_property_transaction_price`
        CHECK (
            (
                `transaction_type` = 'SALE'
                    AND `sale_price` > 0
                    AND `deposit` IS NULL
                    AND `monthly_rent` IS NULL
                )
                OR
            (
                `transaction_type` = 'JEONSE'
                    AND `sale_price` IS NULL
                    AND `deposit` > 0
                    AND `monthly_rent` IS NULL
                )
                OR
            (
                `transaction_type` = 'MONTHLY_RENT'
                    AND `sale_price` IS NULL
                    AND `deposit` IS NOT NULL
                    AND `deposit` >= 0
                    AND `monthly_rent` > 0
                )
            ),

    CONSTRAINT `chk_property_publisher_type_enum`
        CHECK (`publisher_type` IN (
                                    'DIRECT_OWNER',
                                    'AGENT_BROKERAGE'
            )),

    CONSTRAINT `chk_property_property_type_enum`
        CHECK (`property_type` IN (
                                   'APARTMENT',
                                   'OFFICETEL',
                                   'VILLA',
                                   'ROOM'
            )),

    CONSTRAINT `chk_property_transaction_type_enum`
        CHECK (`transaction_type` IN (
                                      'SALE',
                                      'JEONSE',
                                      'MONTHLY_RENT'
            )),

    CONSTRAINT `chk_property_publication_status_enum`
        CHECK (`publication_status` IN (
                                        'IN_REVIEW',
                                        'PUBLISHED',
                                        'HIDDEN',
                                        'REJECTED'
            )),

    CONSTRAINT `chk_property_transaction_status_enum`
        CHECK (`transaction_status` IN (
                                        'AVAILABLE',
                                        'RESERVED',
                                        'COMPLETED'
            )),

    CONSTRAINT `chk_property_verification_status_enum`
        CHECK (`verification_status` IN (
                                         'UNVERIFIED',
                                         'IN_REVIEW',
                                         'OWNER_VERIFIED',
                                         'AGENT_VERIFIED',
                                         'REJECTED',
                                         'EXPIRED'
            )),

    CONSTRAINT `chk_property_action_source_enum`
        CHECK (`action_source` IN (
                                   'MEMBER',
                                   'SYSTEM',
                                   'BATCH'
            )),

    INDEX `idx_property_region_active`
        (`region_id`, `deleted_at`),

    INDEX `idx_property_listing`
        (`publication_status`, `transaction_status`, `deleted_at`, `published_at`),

    INDEX `idx_property_type_transaction`
        (`property_type`, `transaction_type`, `deleted_at`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = '매물 핵심 상태, 가격, 검색 정보';


CREATE TABLE `property_revision`
(
    `property_revision_id`    BIGINT        NOT NULL AUTO_INCREMENT,
    `property_id`             BIGINT        NOT NULL,
    `property_version`        BIGINT        NOT NULL,
    `change_type`             VARCHAR(30)   NOT NULL,
    `change_scope`            VARCHAR(30)   NOT NULL,
    `changed_fields_json`     JSON          NOT NULL,
    `before_snapshot_json`    JSON          NULL,
    `after_snapshot_json`     JSON          NOT NULL,
    `snapshot_schema_version` INT           NOT NULL,
    `actor_member_id`         BIGINT        NULL,
    `actor_role`              VARCHAR(30)   NULL,
    `change_reason`           VARCHAR(1000) NULL,
    `trace_id`                VARCHAR(100)  NOT NULL,
    `occurred_at`             DATETIME(6)   NOT NULL,
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

    PRIMARY KEY (`property_revision_id`),

    CONSTRAINT `uq_property_revision_property_version`
        UNIQUE (`property_id`, `property_version`),

    CONSTRAINT `chk_property_revision_version`
        CHECK (`property_version` >= 0),

    CONSTRAINT `chk_property_revision_schema_version`
        CHECK (`snapshot_schema_version` >= 1),

    CONSTRAINT `chk_property_revision_change_type_enum`
        CHECK (`change_type` IN (
                                 'CREATE',
                                 'UPDATE',
                                 'SOFT_DELETE',
                                 'RESTORE'
            )),

    CONSTRAINT `chk_property_revision_change_scope_enum`
        CHECK (`change_scope` IN (
                                  'PROPERTY',
                                  'OPTION',
                                  'STATUS',
                                  'COMPOSITE'
            )),

    CONSTRAINT `chk_property_revision_action_source_enum`
        CHECK (`action_source` IN (
                                   'MEMBER',
                                   'SYSTEM',
                                   'BATCH'
            )),

    INDEX `idx_property_revision_timeline`
        (`property_id`, `occurred_at`),

    CONSTRAINT `fk_property_revision_property`
        FOREIGN KEY (`property_id`)
            REFERENCES `property` (`property_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = '매물 전체 변경 이력과 변경 전후 스냅샷';


CREATE TABLE `property_status_history`
(
    `property_status_history_id` BIGINT        NOT NULL AUTO_INCREMENT,
    `property_id`                BIGINT        NOT NULL,
    `property_revision_id`       BIGINT        NOT NULL,
    `status_type`                VARCHAR(30)   NOT NULL,
    `before_status`              VARCHAR(30)   NULL,
    `after_status`               VARCHAR(30)   NOT NULL,
    `actor_member_id`            BIGINT        NULL,
    `actor_role`                 VARCHAR(30)   NULL,
    `reason_code`                VARCHAR(50)   NULL,
    `reason`                     VARCHAR(1000) NULL,
    `property_version`           BIGINT        NOT NULL,
    `trace_id`                   VARCHAR(100)  NOT NULL,
    `occurred_at`                DATETIME(6)   NOT NULL,
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

    PRIMARY KEY (`property_status_history_id`),

    CONSTRAINT `uq_property_status_history_revision_type`
        UNIQUE (`property_revision_id`, `status_type`),

    CONSTRAINT `chk_property_status_history_version`
        CHECK (`property_version` >= 0),

    CONSTRAINT `chk_property_status_history_type_enum`
        CHECK (`status_type` IN (
                                 'PUBLICATION',
                                 'TRANSACTION',
                                 'VERIFICATION'
            )),

    CONSTRAINT `chk_property_status_history_action_source_enum`
        CHECK (`action_source` IN (
                                   'MEMBER',
                                   'SYSTEM',
                                   'BATCH'
            )),

    INDEX `idx_property_status_history_timeline`
        (`property_id`, `occurred_at`),

    CONSTRAINT `fk_property_status_history_property`
        FOREIGN KEY (`property_id`)
            REFERENCES `property` (`property_id`),

    CONSTRAINT `fk_property_status_history_revision`
        FOREIGN KEY (`property_revision_id`)
            REFERENCES `property_revision` (`property_revision_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = '매물 상태 변경 이력';


CREATE TABLE `property_publisher_snapshot`
(
    `publisher_snapshot_id` BIGINT       NOT NULL AUTO_INCREMENT,
    `property_id`           BIGINT       NOT NULL,
    `property_revision_id`  BIGINT       NOT NULL,
    `publisher_type`        VARCHAR(30)  NOT NULL,
    `publisher_member_id`   BIGINT       NOT NULL,
    `agent_id`              BIGINT       NULL,
    `agency_id`             BIGINT       NULL,
    `office_name`           VARCHAR(200) NULL,
    `office_address`        VARCHAR(300) NULL,
    `office_contact`        VARCHAR(50)  NULL,
    `registration_number`   VARCHAR(100) NULL,
    `broker_name`           VARCHAR(100) NULL,
    `snapshot_json`         JSON         NULL,
    `captured_at`           DATETIME(6)  NOT NULL,
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

    PRIMARY KEY (`publisher_snapshot_id`),

    CONSTRAINT `uq_property_publisher_snapshot_revision`
        UNIQUE (`property_revision_id`),

    CONSTRAINT `chk_property_publisher_snapshot_type_enum`
        CHECK (`publisher_type` IN (
                                    'DIRECT_OWNER',
                                    'AGENT_BROKERAGE'
            )),

    CONSTRAINT `chk_property_publisher_snapshot_action_source_enum`
        CHECK (`action_source` IN (
                                   'MEMBER',
                                   'SYSTEM',
                                   'BATCH'
            )),

    INDEX `idx_property_publisher_snapshot_timeline`
        (`property_id`, `captured_at`),

    CONSTRAINT `fk_property_publisher_snapshot_property`
        FOREIGN KEY (`property_id`)
            REFERENCES `property` (`property_id`),

    CONSTRAINT `fk_property_publisher_snapshot_revision`
        FOREIGN KEY (`property_revision_id`)
            REFERENCES `property_revision` (`property_revision_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = '매물 등록 주체 정보 스냅샷';
