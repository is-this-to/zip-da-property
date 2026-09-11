# 매물 검증 증빙 보관 및 정리

## 목적

검증에 제출된 원본 증빙 파일을 검증 종료 후 30일까지만 보관하고,
검증 신청·심사 상태와 이력은 그대로 유지합니다.

## 처리 흐름

1. `VERIFIED`, `REJECTED`, `EXPIRED` 상태이며 `reviewed_at`이 30일 이상 지난
   활성 증빙 연결을 최대 100건 조회합니다.
2. `property_verification_evidence`를 soft delete 합니다.
3. 같은 `property_file`을 참조하는 다른 활성 증빙이 없으면 파일 메타데이터도
   soft delete 합니다.
4. DB 트랜잭션이 커밋된 뒤 기존 파일 삭제 이벤트를 통해 MinIO 객체를 삭제합니다.
5. 객체 삭제가 실패하면 기존 파일 유지보수 배치가 재시도합니다.

## 보호 범위

- `property_verification` 신청·심사 결과와 상태 이력은 삭제하지 않습니다.
- 검증 중인 신청의 증빙은 삭제하지 않습니다.
- 다른 활성 검증에서 공유 중인 파일은 삭제하지 않습니다.
- 로그에는 파일명, 객체 키, 증빙 내용 등 민감정보를 기록하지 않습니다.
- Outbox는 사용하지 않습니다.

## 운영 설정

- 기본 실행 간격: 15분
- 환경 변수: `PROPERTY_VERIFICATION_EVIDENCE_CLEANUP_FIXED_DELAY_MS`
- 배치 크기: 100건
- 선행 DDL: `database/schema/012_add_property_verification_evidence_cleanup_indexes.sql`

## 검증

- 보관 기간 경계와 종료 상태별 대상 선정
- 활성 공유 참조가 없는 파일의 soft delete 및 MinIO 삭제 요청
- 활성 공유 참조가 남은 파일 보호
- 빈 배치의 무동작
- 중복 실행 방지
