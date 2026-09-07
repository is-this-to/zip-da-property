-- 집 로컬 DB의 옵션 원장과 APARTMENT 매핑을 seed 작성용으로 확인한다.
-- 이 파일은 SELECT와 SHOW만 사용하며 데이터를 변경하지 않는다.
-- 조회 결과를 전달받기 전에는 option_code_id나 화면 정보를 추측해 INSERT를 작성하지 않는다.

SELECT DATABASE() AS `target_database`, VERSION() AS `mysql_version`;

SELECT
    `table_name`,
    `ordinal_position`,
    `column_name`,
    `column_type`,
    `is_nullable`,
    `column_default`,
    `extra`
FROM `information_schema`.`columns`
WHERE `table_schema` = DATABASE()
  AND `table_name` IN ('property_option_code', 'property_type_option')
ORDER BY `table_name`, `ordinal_position`;

SELECT
    (SELECT COUNT(*) FROM `property_option_code`) AS `property_option_code_count`,
    (SELECT COUNT(*) FROM `property_type_option`) AS `property_type_option_count`,
    (SELECT COUNT(*) FROM `property_option`) AS `property_option_count`,
    (SELECT COUNT(*) FROM `property_option_history`) AS `property_option_history_count`;

-- 옵션 원장 전체 행과 모든 컬럼을 고정된 순서로 조회한다.
SELECT
    `option_code_id`,
    `option_code`,
    `option_name`,
    `option_category`,
    `description`,
    `is_filterable`,
    `is_detail_visible`,
    `is_registration_enabled`,
    `display_order`,
    `is_active`,
    `created_at`,
    `created_by_member_id`,
    `created_by_role`,
    `updated_at`,
    `updated_by_member_id`,
    `updated_by_role`,
    `deleted_at`,
    `deleted_by_member_id`,
    `deleted_by_role`,
    `delete_reason`,
    `action_source`
FROM `property_option_code`
ORDER BY `option_code_id`;

-- APARTMENT 유형 매핑 전체 행과 모든 컬럼을 조회한다.
-- joined_option_code는 향후 seed가 내부 ID 대신 논리 코드로 원장을 찾도록 만드는 기준이다.
SELECT
    `pto`.`property_type_option_id`,
    `pto`.`property_type`,
    `pto`.`option_code_id`,
    `poc`.`option_code` AS `joined_option_code`,
    `pto`.`is_required`,
    `pto`.`default_value`,
    `pto`.`display_order`,
    `pto`.`created_at`,
    `pto`.`created_by_member_id`,
    `pto`.`created_by_role`,
    `pto`.`updated_at`,
    `pto`.`updated_by_member_id`,
    `pto`.`updated_by_role`,
    `pto`.`deleted_at`,
    `pto`.`deleted_by_member_id`,
    `pto`.`deleted_by_role`,
    `pto`.`delete_reason`,
    `pto`.`action_source`
FROM `property_type_option` AS `pto`
LEFT JOIN `property_option_code` AS `poc`
  ON `poc`.`option_code_id` = `pto`.`option_code_id`
WHERE `pto`.`property_type` = 'APARTMENT'
ORDER BY `pto`.`display_order`, `pto`.`property_type_option_id`;

-- 논리 참조가 끊긴 APARTMENT 매핑이 없어야 한다.
SELECT
    `pto`.`property_type_option_id`,
    `pto`.`property_type`,
    `pto`.`option_code_id`
FROM `property_type_option` AS `pto`
LEFT JOIN `property_option_code` AS `poc`
  ON `poc`.`option_code_id` = `pto`.`option_code_id`
WHERE `pto`.`property_type` = 'APARTMENT'
  AND `poc`.`option_code_id` IS NULL
ORDER BY `pto`.`property_type_option_id`;

-- 활성 UNIQUE가 없으므로 활성 중복이 있는지 별도로 확인한다. 결과가 없어야 한다.
SELECT
    `pto`.`property_type`,
    `poc`.`option_code`,
    COUNT(*) AS `active_row_count`
FROM `property_type_option` AS `pto`
JOIN `property_option_code` AS `poc`
  ON `poc`.`option_code_id` = `pto`.`option_code_id`
WHERE `pto`.`property_type` = 'APARTMENT'
  AND `pto`.`deleted_at` IS NULL
GROUP BY `pto`.`property_type`, `poc`.`option_code`
HAVING COUNT(*) > 1
ORDER BY `poc`.`option_code`;

SHOW CREATE TABLE `property_option_code`;
SHOW CREATE TABLE `property_type_option`;
