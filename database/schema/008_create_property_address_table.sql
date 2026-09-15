CREATE TABLE `property_address`
(
    `property_address_id` BIGINT NOT NULL AUTO_INCREMENT
        COMMENT '주소 ID. MySQL AUTO_INCREMENT 생성',

    `property_id` BIGINT NOT NULL
        COMMENT '매물 ID. 논리 참조: property.property_id, 매물과 1:1',

    `legal_dong_code` CHAR(10) NOT NULL
        COMMENT 'Kakao 응답의 법정동 코드. 내부 Region 검증에 사용',

    `exact_road_address` VARCHAR(300) NULL
        COMMENT '정확 도로명 주소. 권한 API에서만 사용',

    `exact_jibun_address` VARCHAR(300) NULL
        COMMENT '정확 지번 주소. 권한 API에서만 사용',

    `detail_address_encrypted` VARBINARY(1000) NULL
        COMMENT '동·호 등 상세주소 암호문. 평문 로그 금지',

    `detail_address_key_version` VARCHAR(50) NULL
        COMMENT '상세주소 암호화에 사용한 키 버전. 암호문이 NULL이면 NULL',

    `exact_location` POINT SRID 4326 NOT NULL
        COMMENT '정확 좌표. POINT(경도 위도) 순서. 공개 DTO 노출 금지',

    `public_address` VARCHAR(300) NOT NULL
        COMMENT '공개 가능한 동 수준 주소',

    `public_location` POINT SRID 4326 NOT NULL
        COMMENT '지도 검색과 마커 표시에 사용하는 비식별 공개 좌표',

    `disclosure_level` VARCHAR(30) NOT NULL
        COMMENT '위치 공개 수준. DONG, APPROXIMATE, COMPLEX_CENTER',

    `location_verified_at` DATETIME(6) NOT NULL
        COMMENT '주소·좌표·법정동·Region 검증 완료 UTC 시각',

    `location_source` VARCHAR(30) NOT NULL
        COMMENT '위치 정보 출처. KAKAO_LOCAL, ADMIN, BATCH',

    `created_at` DATETIME(6) NOT NULL
        COMMENT '최초 생성 UTC 시각',

    `created_by_member_id` BIGINT NULL
        COMMENT '생성한 Member 외부 ID',

    `created_by_role` VARCHAR(30) NULL
        COMMENT '생성 당시 사용자 역할',

    `updated_at` DATETIME(6) NOT NULL
        COMMENT '마지막 수정 UTC 시각',

    `updated_by_member_id` BIGINT NULL
        COMMENT '마지막 수정 Member 외부 ID',

    `updated_by_role` VARCHAR(30) NULL
        COMMENT '마지막 수정 당시 사용자 역할',

    `deleted_at` DATETIME(6) NULL
        COMMENT '소프트 삭제 UTC 시각. NULL이면 활성',

    `deleted_by_member_id` BIGINT NULL
        COMMENT '삭제한 Member 외부 ID',

    `deleted_by_role` VARCHAR(30) NULL
        COMMENT '삭제 당시 사용자 역할',

    `delete_reason` VARCHAR(500) NULL
        COMMENT '소프트 삭제 사유',

    `action_source` VARCHAR(20) NOT NULL
        COMMENT '작업 주체. MEMBER, SYSTEM, BATCH',

    PRIMARY KEY (`property_address_id`),

    CONSTRAINT `uq_property_address_01`
        UNIQUE (`property_id`),

    CONSTRAINT `chk_property_address_key_pair`
        CHECK (
            (
                `detail_address_encrypted` IS NULL
                AND `detail_address_key_version` IS NULL
            )
            OR
            (
                `detail_address_encrypted` IS NOT NULL
                AND `detail_address_key_version` IS NOT NULL
            )
        ),

    CONSTRAINT `chk_property_address_disclosure_level_enum`
        CHECK (
            `disclosure_level` IN (
                'DONG',
                'APPROXIMATE',
                'COMPLEX_CENTER'
            )
        ),

    CONSTRAINT `chk_property_address_location_source_enum`
        CHECK (
            `location_source` IN (
                'KAKAO_LOCAL',
                'ADMIN',
                'BATCH'
            )
        ),

    CONSTRAINT `chk_property_address_action_source_enum`
        CHECK (
            `action_source` IN (
                'MEMBER',
                'SYSTEM',
                'BATCH'
            )
        ),

    KEY `idx_property_address_legal_dong_deleted`
        (`legal_dong_code`, `deleted_at`),

    SPATIAL KEY `spx_property_exact_location`
        (`exact_location`),

    SPATIAL KEY `spx_property_public_location`
        (`public_location`)
)
ENGINE = InnoDB
DEFAULT CHARSET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci
COMMENT = '매물 정확 주소와 지도 공개용 비식별 위치';
