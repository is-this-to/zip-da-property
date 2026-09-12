# Property 쓰기 API OpenAPI 계약

## 목적

`IF-PR-007`, `IF-PR-012`에 따라 Property 쓰기 API의 실제 HTTP 성공
상태와 OpenAPI 문서가 일치하도록 유지한다.

## 성공 응답

| API | 성공 상태 |
| --- | --- |
| 매물 등록 | `201 Created` |
| 매물 수정 | `200 OK` |
| 거래 상태 변경 | `200 OK` |
| 공개 상태 변경 | `200 OK` |
| 매물 삭제 | `204 No Content` |
| 삭제 매물 복구 | `200 OK` |
| 일반 검증 신청 | `201 Created` |
| 집주인·세입자 검증 신청 | `202 Accepted` |
| 재인증 신청 | `202 Accepted` |
| 관리자 검증 승인·반려 | `200 OK` |

`@CustomApiResponse`는 기본적으로 `200`을 문서화하며, 실제 성공 상태가
다른 API는 `successResponseCode`와 `successDescription`을 명시한다.
이 경우 Springdoc이 자동 생성한 잘못된 `200` 응답은 제거된다.

## 동시성·멱등성 헤더

- 매물 등록: `Idempotency-Key` 필수
- 매물 수정 및 상태 변경·삭제·복구·검증: `If-Match` 필수

## 자동 검증

`PropertyOpenApiContractTest`는 실제 `/api-docs` JSON을 생성하여 담당
API의 경로, 메서드, 성공 상태, 필수 헤더 및 대표 오류 응답을 검증한다.
