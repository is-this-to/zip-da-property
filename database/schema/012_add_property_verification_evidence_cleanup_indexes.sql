-- 검증 종료 후 30일이 지난 증빙을 배치로 조회하고 정리하기 위한 인덱스입니다.
-- 기존 007/010/011 DDL 적용 이후 애플리케이션 배포 전에 한 번 실행합니다.

CREATE INDEX `idx_property_verification_evidence_cleanup`
    ON `property_verification` (
        `status`,
        `deleted_at`,
        `reviewed_at`,
        `property_verification_id`
    );

CREATE INDEX `idx_verification_evidence_active`
    ON `property_verification_evidence` (
        `property_verification_id`,
        `deleted_at`,
        `verification_evidence_id`
    );
