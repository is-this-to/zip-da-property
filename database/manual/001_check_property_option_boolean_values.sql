-- 기존 로컬 DB의 옵션 문자열 값을 변경하기 전에 HeidiSQL에서 먼저 실행한다.
-- 이 파일은 조회만 수행하며 데이터를 변경하지 않는다.

SELECT DATABASE() AS target_database, VERSION() AS mysql_version;

SELECT
    `table_name`,
    `column_name`,
    `column_type`,
    `is_nullable`
FROM `information_schema`.`columns`
WHERE `table_schema` = DATABASE()
  AND (
        (`table_name` = 'property_type_option' AND `column_name` = 'default_value')
        OR (`table_name` = 'property_option' AND `column_name` = 'option_value')
        OR (`table_name` = 'property_option_history' AND `column_name` IN ('before_value', 'after_value'))
      )
ORDER BY `table_name`, `ordinal_position`;

SELECT
    `tc`.`table_name`,
    `tc`.`constraint_name`,
    `cc`.`check_clause`
FROM `information_schema`.`table_constraints` AS `tc`
JOIN `information_schema`.`check_constraints` AS `cc`
  ON `cc`.`constraint_schema` = `tc`.`constraint_schema`
 AND `cc`.`constraint_name` = `tc`.`constraint_name`
WHERE `tc`.`constraint_schema` = DATABASE()
  AND `tc`.`constraint_type` = 'CHECK'
  AND `tc`.`table_name` IN (
        'property_type_option',
        'property_option',
        'property_option_history'
      )
ORDER BY `tc`.`table_name`, `tc`.`constraint_name`;

SELECT
    'property_type_option.default_value' AS `value_source`,
    `default_value` AS `stored_value`,
    HEX(`default_value`) AS `stored_value_hex`,
    COUNT(*) AS `row_count`
FROM `property_type_option`
GROUP BY `default_value`, HEX(`default_value`)
ORDER BY `default_value`;

SELECT
    'property_option.option_value' AS `value_source`,
    `option_value` AS `stored_value`,
    HEX(`option_value`) AS `stored_value_hex`,
    COUNT(*) AS `row_count`
FROM `property_option`
GROUP BY `option_value`, HEX(`option_value`)
ORDER BY `option_value`;

SELECT
    'property_option_history.before_value' AS `value_source`,
    `before_value` AS `stored_value`,
    HEX(`before_value`) AS `stored_value_hex`,
    COUNT(*) AS `row_count`
FROM `property_option_history`
GROUP BY `before_value`, HEX(`before_value`)
ORDER BY `before_value`;

SELECT
    'property_option_history.after_value' AS `value_source`,
    `after_value` AS `stored_value`,
    HEX(`after_value`) AS `stored_value_hex`,
    COUNT(*) AS `row_count`
FROM `property_option_history`
GROUP BY `after_value`, HEX(`after_value`)
ORDER BY `after_value`;

-- invalid_row_count가 모두 0이어야 다음 ALTER 파일을 적용할 수 있다.
-- true, false는 이미 목표값이고 있음, 없음은 ALTER 파일에서 변환할 수 있는 구 값이다.
SELECT
    'property_type_option.default_value' AS `value_source`,
    COUNT(*) AS `invalid_row_count`
FROM `property_type_option`
WHERE `default_value` IS NOT NULL
  AND CAST(`default_value` AS BINARY) NOT IN ('true', 'false', '있음', '없음')
UNION ALL
SELECT
    'property_option.option_value',
    COUNT(*)
FROM `property_option`
WHERE CAST(`option_value` AS BINARY) NOT IN ('true', 'false', '있음', '없음')
UNION ALL
SELECT
    'property_option_history.before_value',
    COUNT(*)
FROM `property_option_history`
WHERE `before_value` IS NOT NULL
  AND CAST(`before_value` AS BINARY) NOT IN ('true', 'false', '있음', '없음')
UNION ALL
SELECT
    'property_option_history.after_value',
    COUNT(*)
FROM `property_option_history`
WHERE `after_value` IS NOT NULL
  AND CAST(`after_value` AS BINARY) NOT IN ('true', 'false', '있음', '없음');
