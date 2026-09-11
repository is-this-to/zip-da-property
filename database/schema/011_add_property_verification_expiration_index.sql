-- 인증 만료 배치의 VERIFIED + 미삭제 + 만료시각 범위 조회를 지원합니다.
-- 기존 007/010 DDL 적용 이후 애플리케이션 배포 전에 한 번 실행합니다.

CREATE INDEX `idx_property_verification_expiration`
    ON `property_verification` (
        `status`,
        `deleted_at`,
        `expires_at`,
        `property_verification_id`
    );
