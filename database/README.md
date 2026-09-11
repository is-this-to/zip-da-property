# ZIPDA Property Database Schema

이 폴더의 SQL 파일은 Flyway 등 자동 마이그레이션 도구를 사용하지 않고
개발자가 MySQL에 직접 실행합니다.

## 실행 순서

`database/schema` 폴더의 번호가 작은 SQL 파일부터 순서대로 실행합니다.

1. `001_create_property_core_tables.sql`
2. `002_create_property_favorite_table.sql`
3. `003_create_property_option_tables.sql`
4. `004_create_property_idempotency_table.sql`
5. `005_create_property_file_table.sql`
6. `006_create_property_audit_event_table.sql`
7. `007_create_property_verification_tables.sql`
8. `008_create_property_address_table.sql`
9. `009_create_property_member_integration_tables.sql`
10. `010_add_property_tenant_verification.sql`
11. `011_add_property_verification_expiration_index.sql`

## 실행 전 확인

- 실행 대상 DB가 Property 서비스 DB인지 확인합니다.
- 동일한 테이블이 이미 존재하는지 확인합니다.
- 팀원이 같은 번호의 SQL 파일을 추가하지 않았는지 확인합니다.
- 운영 DB에서는 실행 전 반드시 백업합니다.

## 현재 정책

- USER의 임차인 직접 등록(`DIRECT_TENANT`)을 지원합니다.
- `DIRECT_TENANT` 매물은 세입자 인증 승인 시 `TENANT_VERIFIED`로 전환합니다.
- 입주 가능 상태 및 입주일은 관리하지 않습니다.
- 전세대출 가능 여부는 관리하지 않습니다.

## 세입자 등록·인증 확장

- 기존 DB에는 애플리케이션 배포 전에
  `010_add_property_tenant_verification.sql`을 실행합니다.
- `property.publisher_type`과
  `property_publisher_snapshot.publisher_type`에
  `DIRECT_TENANT`를 허용합니다.
- `property.verification_status`에 `TENANT_VERIFIED`를 허용하고,
  `property_verification.verification_type`에 `TENANT`를 허용합니다.
- 스크립트 적용 전에는 기존 애플리케이션 동작과 데이터가 유지됩니다.
- 확장 후 새 Enum 값이 저장되면 이전 애플리케이션 버전에서는 이를 읽지 못할 수 있으므로
  DB 확장 후 새 애플리케이션을 배포하고 이전 버전으로 임의 롤백하지 않습니다.

## 인증 만료 배치 인덱스

- 인증 만료 배치를 배포하기 전에
  `011_add_property_verification_expiration_index.sql`을 한 번 실행합니다.
- `status`, `deleted_at`, `expires_at`, `property_verification_id` 순서로
  만료 대상 조회 인덱스를 추가합니다.
- 인덱스 생성은 대상 테이블 크기에 따라 시간이 걸릴 수 있으므로 운영 반영 전
  스테이징에서 실행 시간과 잠금 영향을 확인합니다.
- 배치는 기본 60초 간격으로 최대 100건을 처리하며
  `PROPERTY_VERIFICATION_EXPIRATION_FIXED_DELAY_MS`로 실행 간격을 변경할 수 있습니다.
- 만료 시 `property_verification.status`와 `property.verification_status`를
  `EXPIRED`로 변경하고 revision·상태 이력·감사·Kafka 이벤트를 함께 기록합니다.
- 지도 및 찜 공개 조회는 `OWNER_VERIFIED`, `TENANT_VERIFIED`,
  `AGENT_VERIFIED` 상태만 노출합니다.

## Member 이벤트 연동

- `009_create_property_member_integration_tables.sql`을 적용한 뒤에만
  `MEMBER_KAFKA_ENABLED=true`로 Member Kafka 소비자를 활성화합니다.
- 기본 토픽은 `zipda.member.events.v1`, 기본 소비자 그룹은
  `zip-da-property-member-events`입니다.
- `property_member_event_consumption.event_id` 기본 키로 중복 이벤트를
  멱등 처리합니다.
- `MemberWithdrawn` 이벤트는 해당 회원이 작성한 공개 상태(`PUBLISHED`)
  매물을 `HIDDEN`으로 변경하고 revision·상태 이력·감사·Kafka 이벤트를
  함께 기록합니다.
- Member 이벤트에는 이메일·전화번호·서류 원문 등 개인정보를 포함하거나
  Property DB에 복제하지 않습니다.

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

- 옵션 현재값은 `option_value VARCHAR(5)`에 정확히 소문자 문자열 `true` 또는 `false`로 저장합니다.
- 유형별 기본값은 nullable `default_value VARCHAR(5)`이며, 미확인은 NULL로 구분합니다.
- 옵션 이력의 nullable `before_value`, `after_value`도 `VARCHAR(5)`의 `true` 또는 `false`를 저장합니다.
  `changed_fields VARCHAR(500)`는 변경 필드명을 쉼표로 구분해 저장하며 JSON을 사용하지 않습니다.
- 옵션의 `value_type`, `unit`, `allowed_values_json`은 사용하지 않습니다.
- PR-049는 옵션 메타데이터를 record DTO 목록으로 반환하며,
  `valueType`, `unit`, `allowedValues` 응답 필드는 제거했습니다.
- 옵션 원장 코드 UNIQUE와 revision·옵션 행 조합의 이력 UNIQUE는 유지합니다.
- 이 정책은 옵션 영역에만 적용하며 001 및 다른 도메인의 값 저장 구조를 변경하지 않습니다.
- 수정된 003은 팀 SQL 통합 후 적용할 생성 DDL입니다. 기존 테이블에 그대로 재실행하지 않습니다.
