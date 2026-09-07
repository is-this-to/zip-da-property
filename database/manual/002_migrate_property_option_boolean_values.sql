-- 실행 전 필수 조건
-- 1. 001_check_property_option_boolean_values.sql의 대상 DB와 MySQL 버전을 확인한다.
-- 2. 네 컬럼의 invalid_row_count가 모두 0인지 확인한다.
-- 3. 운영 또는 공유 DB라면 백업하고 팀 적용 시간을 합의한다.
-- 4. 아래 CHECK 이름이 SHOW CREATE TABLE 결과와 같은지 확인한다.
-- MySQL DDL은 암묵적으로 COMMIT될 수 있으므로 이 파일은 자동 rollback을 전제로 하지 않는다.

-- 기존 CHECK가 true/false 변환을 거부하지 않도록 먼저 제거한다.
ALTER TABLE `property_type_option`
    DROP CHECK `chk_property_type_option_default_value`;

ALTER TABLE `property_option`
    DROP CHECK `chk_property_option_value`;

ALTER TABLE `property_option_history`
    DROP CHECK `chk_property_option_history_before_value`,
    DROP CHECK `chk_property_option_history_after_value`;

-- 의미를 보존하면서 예전 한글 값을 최신 소문자 문자열 값으로 변환한다.
UPDATE `property_type_option`
SET `default_value` = CASE
    WHEN CAST(`default_value` AS BINARY) = CAST('있음' AS BINARY) THEN 'true'
    WHEN CAST(`default_value` AS BINARY) = CAST('없음' AS BINARY) THEN 'false'
    ELSE `default_value`
END
WHERE CAST(`default_value` AS BINARY) IN ('있음', '없음');

UPDATE `property_option`
SET `option_value` = CASE
    WHEN CAST(`option_value` AS BINARY) = CAST('있음' AS BINARY) THEN 'true'
    WHEN CAST(`option_value` AS BINARY) = CAST('없음' AS BINARY) THEN 'false'
    ELSE `option_value`
END
WHERE CAST(`option_value` AS BINARY) IN ('있음', '없음');

-- append-only는 애플리케이션의 이력 기록 원칙이다.
-- 이 UPDATE는 기존 값 표현을 동일 의미의 최신 형식으로 바꾸는 일회성 마이그레이션이다.
UPDATE `property_option_history`
SET `before_value` = CASE
    WHEN CAST(`before_value` AS BINARY) = CAST('있음' AS BINARY) THEN 'true'
    WHEN CAST(`before_value` AS BINARY) = CAST('없음' AS BINARY) THEN 'false'
    ELSE `before_value`
END
WHERE CAST(`before_value` AS BINARY) IN ('있음', '없음');

UPDATE `property_option_history`
SET `after_value` = CASE
    WHEN CAST(`after_value` AS BINARY) = CAST('있음' AS BINARY) THEN 'true'
    WHEN CAST(`after_value` AS BINARY) = CAST('없음' AS BINARY) THEN 'false'
    ELSE `after_value`
END
WHERE CAST(`after_value` AS BINARY) IN ('있음', '없음');

-- 아래 결과가 모두 0인지 확인한 뒤 컬럼 축소와 CHECK 추가 구문을 실행한다.
SELECT
    'property_type_option.default_value' AS `value_source`,
    COUNT(*) AS `invalid_row_count`
FROM `property_type_option`
WHERE `default_value` IS NOT NULL
  AND CAST(`default_value` AS BINARY) NOT IN ('true', 'false')
UNION ALL
SELECT
    'property_option.option_value',
    COUNT(*)
FROM `property_option`
WHERE CAST(`option_value` AS BINARY) NOT IN ('true', 'false')
UNION ALL
SELECT
    'property_option_history.before_value',
    COUNT(*)
FROM `property_option_history`
WHERE `before_value` IS NOT NULL
  AND CAST(`before_value` AS BINARY) NOT IN ('true', 'false')
UNION ALL
SELECT
    'property_option_history.after_value',
    COUNT(*)
FROM `property_option_history`
WHERE `after_value` IS NOT NULL
  AND CAST(`after_value` AS BINARY) NOT IN ('true', 'false');

ALTER TABLE `property_type_option`
    MODIFY COLUMN `default_value` VARCHAR(5) NULL
        COMMENT '기본 옵션 값. true 또는 false. 미확인 시 NULL';

ALTER TABLE `property_option`
    MODIFY COLUMN `option_value` VARCHAR(5) NOT NULL
        COMMENT '옵션 값: true 또는 false';

ALTER TABLE `property_option_history`
    MODIFY COLUMN `before_value` VARCHAR(5) NULL
        COMMENT '변경 전 옵션 값: true 또는 false',
    MODIFY COLUMN `after_value` VARCHAR(5) NULL
        COMMENT '변경 후 옵션 값: true 또는 false';

ALTER TABLE `property_type_option`
    ADD CONSTRAINT `chk_property_type_option_default_value`
        CHECK (
            `default_value` IS NULL
                OR CAST(`default_value` AS BINARY) IN ('true', 'false')
        );

ALTER TABLE `property_option`
    ADD CONSTRAINT `chk_property_option_value`
        CHECK (CAST(`option_value` AS BINARY) IN ('true', 'false'));

ALTER TABLE `property_option_history`
    ADD CONSTRAINT `chk_property_option_history_before_value`
        CHECK (
            `before_value` IS NULL
                OR CAST(`before_value` AS BINARY) IN ('true', 'false')
        ),
    ADD CONSTRAINT `chk_property_option_history_after_value`
        CHECK (
            `after_value` IS NULL
                OR CAST(`after_value` AS BINARY) IN ('true', 'false')
        );

-- 최종 확인: 값별 건수와 최종 DDL을 확인한다.
SELECT `default_value`, COUNT(*) AS `row_count`
FROM `property_type_option`
GROUP BY `default_value`;

SELECT `option_value`, COUNT(*) AS `row_count`
FROM `property_option`
GROUP BY `option_value`;

SELECT `before_value`, `after_value`, COUNT(*) AS `row_count`
FROM `property_option_history`
GROUP BY `before_value`, `after_value`;

SHOW CREATE TABLE `property_type_option`;
SHOW CREATE TABLE `property_option`;
SHOW CREATE TABLE `property_option_history`;
