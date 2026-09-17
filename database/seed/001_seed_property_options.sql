-- =========================================================
-- Property Option 기준데이터 Seed
-- 총 15개 / 전부 선택 옵션
-- =========================================================

DROP TEMPORARY TABLE IF EXISTS tmp_property_option_seed;

CREATE TEMPORARY TABLE tmp_property_option_seed
(
    option_code      VARCHAR(50)  NOT NULL PRIMARY KEY,
    option_name      VARCHAR(100) NOT NULL,
    option_category  VARCHAR(30)  NOT NULL,
    display_order    INT          NOT NULL
);

INSERT INTO tmp_property_option_seed
    (option_code, option_name, option_category, display_order)
VALUES
    ('AIR_CONDITIONER',      '에어컨',           'APPLIANCE', 10),
    ('REFRIGERATOR',         '냉장고',           'APPLIANCE', 20),
    ('WASHING_MACHINE',      '세탁기',           'APPLIANCE', 30),
    ('GAS_RANGE',            '가스레인지',       'APPLIANCE', 40),
    ('MICROWAVE',            '전자레인지',       'APPLIANCE', 50),

    ('BUILT_IN_WARDROBE',    '붙박이장',         'FURNITURE', 60),
    ('SHOE_CABINET',         '신발장',           'FURNITURE', 70),

    ('BALCONY',              '베란다',           'STRUCTURE', 80),

    ('ENTRANCE_SECURITY',    '현관 보안',        'SECURITY', 90),

    ('INTERNET',             '인터넷',           'LIVING', 100),
    ('BIDET',                '비데',             'LIVING', 110),
    ('PARKING_AVAILABLE',    '주차 가능',        'LIVING', 120),
    ('ELEVATOR',             '엘리베이터 있음',  'LIVING', 130),
    ('PET_ALLOWED',          '반려동물 가능',    'LIVING', 140),

    ('LOAN_AVAILABLE',       '대출 가능',        'ETC', 150);


-- =========================================================
-- 1. 없는 옵션 코드만 INSERT
-- =========================================================

INSERT INTO property_option_code
(
    option_code,
    option_name,
    option_category,
    description,
    is_filterable,
    is_detail_visible,
    is_registration_enabled,
    display_order,
    is_active,
    created_at,
    updated_at,
    action_source
)
SELECT
    s.option_code,
    s.option_name,
    s.option_category,
    CONCAT('매물 등록 선택 옵션: ', s.option_name),
    0,          -- 검색 필터는 이번 seed에서 활성화하지 않음
    1,          -- 상세 화면 노출 가능
    1,          -- 등록/수정 화면 사용 가능
    s.display_order,
    1,          -- 활성
    NOW(6),
    NOW(6),
    'SYSTEM'
FROM tmp_property_option_seed s
WHERE NOT EXISTS (
    SELECT 1
    FROM property_option_code poc
    WHERE poc.option_code = s.option_code
);


-- =========================================================
-- 2. 기존 3개를 포함하여 15개 기준값 통일
--    soft delete된 동일 코드가 있으면 다시 활성화
-- =========================================================

UPDATE property_option_code poc
JOIN tmp_property_option_seed s
    ON s.option_code = poc.option_code
SET
    poc.option_name = s.option_name,
    poc.option_category = s.option_category,
    poc.description = CONCAT('매물 등록 선택 옵션: ', s.option_name),
    poc.is_filterable = 0,
    poc.is_detail_visible = 1,
    poc.is_registration_enabled = 1,
    poc.display_order = s.display_order,
    poc.is_active = 1,
    poc.updated_at = NOW(6),
    poc.updated_by_member_id = NULL,
    poc.updated_by_role = NULL,
    poc.deleted_at = NULL,
    poc.deleted_by_member_id = NULL,
    poc.deleted_by_role = NULL,
    poc.delete_reason = NULL,
    poc.action_source = 'SYSTEM';


-- =========================================================
-- 3. APARTMENT / OFFICETEL / VILLA / ROOM
--    4개 유형 × 15개 옵션 매핑
--    전부 선택사항
-- =========================================================

INSERT INTO property_type_option
(
    property_type,
    option_code_id,
    is_required,
    default_value,
    display_order,
    created_at,
    updated_at,
    action_source
)
SELECT
    pt.property_type,
    poc.option_code_id,
    0,              -- 전부 선택 옵션
    'false',        -- 기본 미선택
    s.display_order,
    NOW(6),
    NOW(6),
    'SYSTEM'
FROM (
    SELECT 'APARTMENT' AS property_type
    UNION ALL
    SELECT 'OFFICETEL'
    UNION ALL
    SELECT 'VILLA'
    UNION ALL
    SELECT 'ROOM'
) pt
CROSS JOIN tmp_property_option_seed s
JOIN property_option_code poc
    ON poc.option_code = s.option_code
WHERE NOT EXISTS (
    SELECT 1
    FROM property_type_option pto
    WHERE pto.property_type = pt.property_type
      AND pto.option_code_id = poc.option_code_id
      AND pto.deleted_at IS NULL
);


-- =========================================================
-- 4. 기존 활성 매핑도 전부 "선택 옵션"으로 통일
--    기존 AIR_CONDITIONER required=true 문제도 여기서 해결
-- =========================================================

UPDATE property_type_option pto
JOIN property_option_code poc
    ON poc.option_code_id = pto.option_code_id
JOIN tmp_property_option_seed s
    ON s.option_code = poc.option_code
SET
    pto.is_required = 0,
    pto.default_value = 'false',
    pto.display_order = s.display_order,
    pto.updated_at = NOW(6),
    pto.updated_by_member_id = NULL,
    pto.updated_by_role = NULL,
    pto.action_source = 'SYSTEM'
WHERE pto.deleted_at IS NULL
  AND pto.property_type IN (
      'APARTMENT',
      'OFFICETEL',
      'VILLA',
      'ROOM'
  );


-- =========================================================
-- 5. 최종 15개 정책에서 제외된 이전 옵션 비활성화
--    기존 seed를 실행한 환경도 최종 활성 옵션 수를 15개로 맞춤
-- =========================================================

UPDATE property_type_option pto
JOIN property_option_code poc
    ON poc.option_code_id = pto.option_code_id
SET
    pto.updated_at = NOW(6),
    pto.updated_by_member_id = NULL,
    pto.updated_by_role = NULL,
    pto.deleted_at = NOW(6),
    pto.deleted_by_member_id = NULL,
    pto.deleted_by_role = NULL,
    pto.delete_reason = 'FINAL_OPTION_SEED_REPLACED',
    pto.action_source = 'SYSTEM'
WHERE pto.deleted_at IS NULL
  AND poc.option_code IN (
      'INDUCTION',
      'TV',
      'BED'
  );

UPDATE property_option_code
SET
    is_registration_enabled = 0,
    is_active = 0,
    updated_at = NOW(6),
    updated_by_member_id = NULL,
    updated_by_role = NULL,
    deleted_at = NOW(6),
    deleted_by_member_id = NULL,
    deleted_by_role = NULL,
    delete_reason = 'FINAL_OPTION_SEED_REPLACED',
    action_source = 'SYSTEM'
WHERE deleted_at IS NULL
  AND option_code IN (
      'INDUCTION',
      'TV',
      'BED'
  );

DROP TEMPORARY TABLE IF EXISTS tmp_property_option_seed;
