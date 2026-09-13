ALTER TABLE `property_idempotency`
    ADD COLUMN `active_idempotency_key` VARCHAR(100)
        GENERATED ALWAYS AS (
            CASE
                WHEN `deleted_at` IS NULL THEN `idempotency_key`
                ELSE NULL
            END
        ) STORED
        COMMENT '활성 멱등 요청에서만 유일성을 적용하는 generated key'
        AFTER `idempotency_key`,
    DROP INDEX `uq_property_idempotency_01`,
    ADD CONSTRAINT `uq_property_idempotency_01`
        UNIQUE (
            `member_id`,
            `endpoint_key`,
            `active_idempotency_key`
        ),
    ADD INDEX `idx_idempotency_cleanup`
        (`deleted_at`, `expires_at`, `idempotency_id`);
