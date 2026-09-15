-- 인증 만료 7일 전 재인증 안내의 중복 발행 방지 시각과 조회 인덱스를 추가합니다.
-- 기존 007/010/011/012 DDL 적용 이후 애플리케이션 배포 전에 한 번 실행합니다.

ALTER TABLE `property_verification`
    ADD COLUMN `renewal_notified_at` DATETIME(6) NULL
        COMMENT '현재 인증 주기의 재인증 안내 발행 시각'
        AFTER `expires_at`;

CREATE INDEX `idx_property_verification_renewal_notice`
    ON `property_verification` (
        `status`,
        `deleted_at`,
        `renewal_notified_at`,
        `expires_at`,
        `property_verification_id`
    );
