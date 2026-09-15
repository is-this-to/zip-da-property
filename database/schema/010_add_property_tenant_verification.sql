-- USER 세입자 직접 등록 및 세입자 인증 상태 확장.
-- 기존 001/007 생성 DDL 적용 이후 한 번 실행합니다.

ALTER TABLE `property`
    DROP CHECK `chk_property_publisher_type_enum`,
    ADD CONSTRAINT `chk_property_publisher_type_enum`
        CHECK (`publisher_type` IN (
            'DIRECT_OWNER',
            'DIRECT_TENANT',
            'AGENT_BROKERAGE'
        )),
    DROP CHECK `chk_property_verification_status_enum`,
    ADD CONSTRAINT `chk_property_verification_status_enum`
        CHECK (`verification_status` IN (
            'UNVERIFIED',
            'IN_REVIEW',
            'OWNER_VERIFIED',
            'TENANT_VERIFIED',
            'AGENT_VERIFIED',
            'REJECTED',
            'EXPIRED'
        ));

ALTER TABLE `property_publisher_snapshot`
    DROP CHECK `chk_property_publisher_snapshot_type_enum`,
    ADD CONSTRAINT `chk_property_publisher_snapshot_type_enum`
        CHECK (`publisher_type` IN (
            'DIRECT_OWNER',
            'DIRECT_TENANT',
            'AGENT_BROKERAGE'
        ));

ALTER TABLE `property_verification`
    DROP CHECK `chk_property_verification_type`,
    ADD CONSTRAINT `chk_property_verification_type`
        CHECK (`verification_type` IN (
            'OWNER',
            'TENANT',
            'AGENT_BROKERAGE'
        ));
