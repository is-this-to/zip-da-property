# ZIPDA Property Database Schema

이 폴더의 SQL 파일은 Flyway 등 자동 마이그레이션 도구를 사용하지 않고
개발자가 MySQL에 직접 실행합니다.

## 실행 순서

`database/schema` 폴더의 번호가 작은 SQL 파일부터 순서대로 실행합니다.

1. `001_create_property_core_tables.sql`
2. `002_create_property_favorite_table.sql`
3. `003_create_property_option_tables.sql`

## 실행 전 확인

- 실행 대상 DB가 Property 서비스 DB인지 확인합니다.
- 동일한 테이블이 이미 존재하는지 확인합니다.
- 팀원이 같은 번호의 SQL 파일을 추가하지 않았는지 확인합니다.
- 운영 DB에서는 실행 전 반드시 백업합니다.

## 현재 정책

- 임차인 직접 등록(`DIRECT_TENANT`)은 지원하지 않습니다.
- 임차인 인증(`TENANT_VERIFIED`)은 지원하지 않습니다.
- 입주 가능 상태 및 입주일은 관리하지 않습니다.
- 전세대출 가능 여부는 관리하지 않습니다.

## 외래키 정책

서비스 또는 데이터베이스 경계와 관계없이 모든 테이블 간 참조는 논리 외래키로 관리합니다.
동일한 Property DB 내부 관계에도 물리 `FOREIGN KEY`를 생성하지 않습니다.

참조 ID 컬럼은 유지하며, 대상 데이터의 존재 여부·활성 상태·soft delete·권한·업무 허용 여부는
Service 및 Repository 계층에서 검증합니다. 참조 대상의 삭제·변경과 동시에 처리되는 경우에도
무결성이 유지되는지 트랜잭션 및 동시성 테스트로 확인합니다.

논리 외래키 정책은 `PRIMARY KEY`, `UNIQUE`, `CHECK`, 인덱스를 제거한다는 의미가 아닙니다.
활성 UNIQUE 사용 여부는 도메인별 정책을 따릅니다. 찜의 활성 UNIQUE는 유지하고,
옵션의 유형별 매핑·현재값에는 활성 UNIQUE를 사용하지 않습니다.

## 옵션 영역의 저장 정책

- 옵션 현재값은 `option_value VARCHAR(300)`에 `있음` 또는 `없음`으로 저장합니다.
- 유형별 기본값은 `default_value VARCHAR(300)`이며, 미확인은 NULL로 구분합니다.
- 옵션 이력은 `before_value`, `after_value`에 문자열 값을 저장합니다.
  `changed_fields VARCHAR(500)`는 변경 필드명을 쉼표로 구분해 저장하며 JSON을 사용하지 않습니다.
- 옵션의 `value_type`, `unit`, `allowed_values_json`은 사용하지 않습니다.
- PR-049는 옵션 메타데이터를 record DTO 목록으로 반환하며,
  `valueType`, `unit`, `allowedValues` 응답 필드는 제거했습니다.
- 옵션 원장 코드 UNIQUE와 revision·옵션 행 조합의 이력 UNIQUE는 유지합니다.
- 이 정책은 옵션 영역에만 적용하며 001 및 다른 도메인의 값 저장 구조를 변경하지 않습니다.
- 수정된 003은 팀 SQL 통합 후 적용할 생성 DDL입니다. 기존 테이블에 그대로 재실행하지 않습니다.
