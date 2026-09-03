# ZIPDA Property Database Schema

이 폴더의 SQL 파일은 Flyway 등 자동 마이그레이션 도구를 사용하지 않고
개발자가 MySQL에 직접 실행합니다.

## 실행 순서

`database/schema` 폴더의 번호가 작은 SQL 파일부터 순서대로 실행합니다.

1. `001_create_property_core_tables.sql`
2. `002_create_property_favorite_table.sql`

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

테이블 간 참조 관계는 논리 외래키로 관리하며, 데이터베이스의 물리적인
FOREIGN KEY 제약은 추가하지 않습니다.

참조 무결성과 대상 데이터 존재 여부는 애플리케이션의 Service 및 Repository 계층에서 검증합니다.
