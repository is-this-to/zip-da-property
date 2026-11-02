# Property API 명세

[README로 돌아가기](../README.md)

이 문서는 현재 Controller의 경로 설정을 기준으로 한 탐색용 명세이다. 요청·응답 DTO와 오류 코드의 최종 기준은 실행 중 생성되는 `/api-docs`이다.

## 공통 계약

- 외부 호출은 Gateway 주소와 `/api/property/**`를 사용
- 보호 API는 Gateway에 `Authorization: Bearer <access-token>` 전달
- `USER`, `AGENT`, 관리자 역할은 Spring Security에서 `ROLE_` 접두어를 붙여 평가
- ID는 JavaScript 숫자 정밀도 손실을 피하도록 클라이언트에서 문자열로 취급
- 응답은 `code`, `message`, `data`, `traceId`를 담은 공통 형식

## Trace ID 사용 예시

호출자는 영문, 숫자, `.`, `_`, `-`로 구성된 1~100자의 `X-Trace-Id`를 전달할 수 있다. 값이 없거나 형식이 잘못되면 Property가 새 ID를 생성한다.

```http
GET /api/property/regions/root HTTP/1.1
X-Trace-Id: property-20261024-a1b2c3
```

동일한 값은 응답 헤더와 본문에 포함된다.

```http
HTTP/1.1 200 OK
X-Trace-Id: property-20261024-a1b2c3
Content-Type: application/json
```

```json
{
  "code": "00",
  "message": "SUCCESS",
  "data": [],
  "traceId": "property-20261024-a1b2c3"
}
```

로그 진단 정보(MDC)에도 같은 값이 들어가므로 Client → Gateway → Property → Member 요청을 연계해 찾을 수 있다. 실제 데이터 항목은 호출 API에 따라 달라진다.

Gateway가 외부 `X-Trace-Id`를 그대로 유지할지 재생성할지는 전체 시스템 정책으로 정해야 하며, 추적 ID에 비밀값이나 개인정보를 넣으면 안 된다.

## 공개 조회 API

| Method | Path | 설명 | 인증 |
| --- | --- | --- | --- |
| GET | `/api/property/property-option-codes` | 매물 유형별 옵션 코드 조회 | 공개 |
| GET | `/api/property/regions/root` | 시·도 조회 | 공개 |
| GET | `/api/property/regions/{parentRegionId}/children` | 직계 하위 지역 조회 | 공개 |
| GET | `/api/property/regions/search` | 지역명 검색 | 공개 |
| GET | `/api/property/regions/{regionId}` | 지역 중심·경계 조회 | 공개 |
| GET | `/api/property/properties` | 지도 영역·검색 조건 기반 커서 목록 조회 | 공개 |
| GET | `/api/property/properties/{propertyId}` | 공개 상세와 비식별 위치 조회 | 공개 |
| GET | `/api/property/properties/map` | 확대 단계별 지역 집계·매물 위치·가격 마커 조회 | 공개 |
| GET | `/api/property/properties/popular` | 전국 또는 광역 지역별 찜 순위 조회 | 공개 |

## 회원·중개사 지원 API

| Method | Path | 설명 | 인증 |
| --- | --- | --- | --- |
| GET | `/api/property/apartment-complexes` | 지역·검색어 기반 활성 단지 조회 | USER/AGENT |
| GET | `/api/property/locations/kakao-address` | Kakao 주소와 좌표 조회 | USER/AGENT |
| POST | `/api/property/locations/validate` | 주소·좌표와 내부 지역 경계 일치 확인 | USER/AGENT |
| POST | `/api/property/files/upload-sessions` | 파일 메타데이터와 MinIO 업로드 URL 생성 | USER/AGENT |
| POST | `/api/property/files/{fileId}/complete` | 객체 크기·형식·체크섬 확인 후 완료 처리 | USER/AGENT |
| DELETE | `/api/property-files/{fileId}` | 본인 소유의 매물 미연결 파일 삭제 | USER/AGENT |
| PUT | `/api/property/properties/{propertyId}/favorite` | 찜 등록·해제 | USER/AGENT |
| GET | `/api/property/me/property-favorites` | 찜한 매물 커서 목록 조회 | USER/AGENT |
| GET | `/api/property/me` | 자신이 등록한 활성 매물 커서 목록 조회 | USER/AGENT |

## 매물 등록·관리 API

| Method | Path | 설명 | 인증 |
| --- | --- | --- | --- |
| POST | `/api/property/properties` | 매물 등록 | USER/AGENT |
| GET | `/api/property/properties/{propertyId}/edit` | 수정 화면용 현재 값과 버전 조회 | 회원·중개사·관리자 |
| PATCH | `/api/property/properties/{propertyId}` | 매물 핵심 정보 부분 수정 | 회원·중개사·관리자 |
| PATCH | `/api/property/properties/{propertyId}/transaction-status` | 거래 상태 변경 | 회원·중개사·관리자 |
| PATCH | `/api/property/properties/{propertyId}/publication-status` | 검수·숨김·재공개 등 공개 상태 변경 | 회원·중개사·관리자 |
| DELETE | `/api/property/properties/{propertyId}` | 논리 삭제 | 회원·중개사·관리자 |
| POST | `/api/property/properties/{propertyId}/restore` | 삭제 매물 복구 | 관리자 |

### 매물 등록 요청 예시

```http
POST /api/property/properties HTTP/1.1
Authorization: Bearer <access-token>
Content-Type: application/json
Idempotency-Key: property-create-1001

{ PropertyCreateRequest }
```

`PropertyCreateRequest`에는 매물 유형·거래 유형·가격·면적, 제목·설명, 주소·좌표, 이미지 파일 ID, 옵션 목록이 포함된다. `Idempotency-Key`는 중복 클릭이나 네트워크 재시도로 동일 매물이 여러 번 등록되는 상황을 방지한다.

### 매물 등록 정상 응답 예시

```http
HTTP/1.1 201 Created
Content-Type: application/json
```

```json
{
  "code": "00",
  "message": "SUCCESS",
  "data": {
    "propertyId": "884685586571263701",
    "version": 0,
    "publicationStatus": "IN_REVIEW",
    "transactionStatus": "AVAILABLE",
    "verificationStatus": "UNVERIFIED"
  },
  "traceId": "property-create-example-001"
}
```

### 매물 등록 오류 응답 예시

예시 조건: `Idempotency-Key` 누락.

```json
{
  "code": "P20",
  "message": "IDEMPOTENCY_KEY_REQUIRED",
  "data": null,
  "traceId": "property-create-example-001"
}
```

- 수정·상태 변경·삭제·복구: `If-Match`와 요청 본문의 `version` 일치 필요
- 매물 삭제: 성공 시 `204 No Content`

## 매물 인증·관리자 검수 API

### 인증 변경

| Method | Path | 설명 | 인증 |
| --- | --- | --- | --- |
| POST | `/api/property/properties/{propertyId}/verifications` | 역할에 맞는 소유·중개 인증 신청 | USER/AGENT |
| POST | `/api/property/properties/{propertyId}/verifications/owner` | 집주인 증빙 기반 인증 신청 | USER |
| POST | `/api/property/properties/{propertyId}/verifications/tenant` | 세입자 증빙 기반 인증 신청 | USER |
| POST | `/api/property/properties/{propertyId}/reverification` | 재인증 신청 | USER/AGENT |
| PATCH | `/api/property/properties/{propertyId}/verifications/{verificationId}` | 인증 승인·반려 | 관리자 |

인증 변경 요청도 `If-Match`와 본문의 `version` 일치를 확인한다.

### 관리자 조회

| Method | Path | 설명 | 인증 |
| --- | --- | --- | --- |
| GET | `/api/admin/property-verifications` | 인증 신청 목록 조회 | 관리자 |
| GET | `/api/admin/property-verifications/{verificationId}` | 인증 신청 상세 조회 | 관리자 |
| GET | `/api/admin/property-publication-reviews` | 공개 검수 대상 목록 조회 | 관리자 |
| GET | `/api/admin/property-publication-reviews/{propertyId}` | 공개 검수 대상 상세 조회 | 관리자 |

관리자 상세 조회는 `X-Audit-Reason`으로 열람 사유를 받아 감사 이력에 기록한다.

## 신고와 이의신청 API

### 회원·중개사

| Method | Path | 설명 | 인증 |
| --- | --- | --- | --- |
| POST | `/api/property/properties/{propertyId}/reports` | 사유·설명·증빙을 포함한 신고 접수 | USER/AGENT |
| GET | `/api/property/me/property-reports` | 내가 접수한 신고 목록 조회 | USER/AGENT |
| POST | `/api/property-reports/{reportId}/appeals` | 신고 대상 매물 작성자의 이의신청 접수 | USER/AGENT |

### 관리자

| Method | Path | 설명 | 인증 |
| --- | --- | --- | --- |
| GET | `/api/admin/property-reports` | 신고 목록 조회 | 관리자 |
| GET | `/api/admin/property-reports/{reportId}` | 신고 상세 조회와 열람 감사 기록 | 관리자 |
| PATCH | `/api/admin/property-reports/{reportId}/status` | 처리 상태 변경 | 관리자 |
| PATCH | `/api/admin/property-reports/{reportId}/assignee` | 담당 관리자 배정 | 관리자 |
| PATCH | `/api/admin/property-reports/{reportId}/risk-score` | 위험도 조정 | 관리자 |
| POST | `/api/admin/property-reports/{reportId}/actions` | 매물 숨김·복구·시정 요청 등 운영 조치 | 관리자 |
| PATCH | `/api/admin/property-report-appeals/{appealId}` | 이의신청 승인·반려 | 관리자 |

신고 상세 조회는 `X-Audit-Reason`이 필요하다. Property 내부의 신고·이의신청·운영 이력은 구현되어 있으며, 사용자·관리자 화면과 Member 회원 제재 연결은 V3 예정이다.

## 서비스 간 연동

Property가 제공하는 `/internal/**` Controller는 현재 없다. 매물 등록·수정 시 Property는 Member의 다음 API를 호출한다.

| 방향 | 메서드·경로 | 적용 |
| --- | --- | --- |
| Property → Member | `GET /internal/members/{memberId}/permissions?action={action}` | 등록 `PROPERTY_CREATE`, 수정 `PROPERTY_UPDATE` 권한 확인 |

호출 시 `X-Trace-Id`를 전달하며, 오류·비정상 응답은 `MEMBER_API_UNAVAILABLE`로 처리한다. 요청·응답 형식과 허용·거부 사유는 [Member API 명세](https://github.com/is-this-to/zip-da-member/blob/dev/zip-da-member-doc/04-api-specification.md#매물-쓰기-권한-조회)를 기준으로 한다.
