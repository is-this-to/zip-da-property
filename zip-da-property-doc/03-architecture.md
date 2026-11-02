# Property 서비스 아키텍처와 MSA 연동

[README로 돌아가기](../README.md)

## 1. ZIPDA 시스템 내 Property 서비스의 위치와 MSA 경계

이 문서에서 MSA는 서비스가 자신의 데이터와 업무 정책을 소유하고, API·이벤트로만 협업하는 구조를 뜻한다. `zip-da-property`는 ZIPDA MSA에서 매물과 매물 관련 업무 데이터를 관리한다. 상세 업무 기능과 상태 전이는 [주요 기능](06-key-features.md), API 계약은 [API 명세](04-api-specification.md)를 기준으로 한다.

- 데이터 소유권: Property DB는 Property가 관리
- 현재 회원 권한 조회: 매물 등록·수정 전 Member 내부 API 동기 호출
- `<V3 예정>`: Member 상태 변경 이벤트 발행·다른 서비스의 Property 이벤트 소비 정책 확정

```text
Client / Admin Client
        │ 외부 요청
        ▼
SCG ── /api/property/** 라우팅 ──▶ Property ──▶ Property DB
                                      │
              ┌───────────────────────┼───────────────────────┐
              ▼                       ▼                       ▼
           Member                  MinIO / Kakao            Kafka
       쓰기 권한 조회            파일 / 주소·좌표       상태 이벤트 발행

<V3 예정>: Member 상태 변경 발행·수신 정책과
           다른 서비스의 Property 이벤트 소비 기준
```

| 서비스·저장소 | 책임·소유 데이터 | Property와의 경계 |
| --- | --- | --- |
| SCG | 외부 요청 라우팅 · Access Token 검증 · 내부 사용자 헤더 구성 | 인증 정보와 Trace ID 전달 |
| Property | 매물 · 지역 · 단지 · 파일 · 인증 · 찜 · 신고 | Property DB 관리 · 매물 업무 규칙 판단 |
| Member | 회원 계정 · 회원 상태 · 쓰기 권한 정책 | 매물 등록·수정 전 권한 API 동기 호출 |
| MinIO | 원본 파일 객체 | 파일 객체 보관 · 업무 상태와 소유 관계는 Property DB 관리 |

## 2. 서비스 내부 구조

```text
┌─────────────────────────────────────────────┐
│ Client / Admin Client / Gateway             │
└─────────────────────┬───────────────────────┘
                      │ HTTP 요청
┌─────────────────────▼───────────────────────┐
│ Filter Layer                                │
│ TraceIdFilter → HeaderAuthenticationFilter  │
└─────────────────────┬───────────────────────┘
                      │ 인증 정보
┌─────────────────────▼───────────────────────┐
│ Controller Layer                            │
│ 요청 수신 · Bean Validation · @PreAuthorize │
└─────────────────────┬───────────────────────┘
                      │ DTO
┌─────────────────────▼───────────────────────┐
│ Service / Policy Layer                      │
│ 트랜잭션 · 업무 규칙 · 상태 변경            │
└──────────────┬──────────────────┬───────────┘
               │                  │
┌──────────────▼──────────────┐  ┌▼──────────────────────────┐
│ Repository Layer            │  │ External Integration      │
│ Spring Data JPA · QueryDSL  │  │ Member · MinIO · Kakao    │
└──────────────┬──────────────┘  └───────────────────────────┘
               │
┌──────────────▼──────────────┐
│ Property MySQL              │
└─────────────────────────────┘

예외 발생 ──▶ GlobalExceptionHandler ──▶ GlobalResponseDTO
```

- Controller: HTTP 요청·응답과 인증 사용자 식별
- Service: 트랜잭션, 업무 규칙, 상태 변경
- Repository: DB 접근

개별 업무의 상태 전이·검증 규칙은 [주요 기능](06-key-features.md), 요청·응답 타입은 [API 명세](04-api-specification.md)에 정리한다.

## 3. 패키지 구조

아래 구조는 `src/main/java/com/zipdaproperty`의 주요 디렉터리·대표 역할이다. 전체 파일 목록은 제외한다.

```text
src/main/java/com/zipdaproperty/
├── ZIpDaProjectTakApplication.java
├── domain/
│   ├── apartment/       # 단지 조회·검증
│   ├── favorite/        # 찜
│   ├── file/            # 업로드 세션·객체 정리
│   ├── image/           # 매물 이미지
│   ├── location/        # Kakao 주소·좌표 검증
│   ├── option/          # 매물 옵션
│   ├── property/        # 매물·인증·이벤트·멱등성·Member 연동
│   ├── region/          # 지역 계층·경계
│   └── report/          # 신고·이의신청·관리자 조치
└── global/
    ├── config/          # JPA·외부 연동·OpenAPI 설정
    ├── context/TraceIdContext.java
    ├── error/GlobalExceptionHandler.java
    ├── filter/TraceIdFilter.java
    ├── response/GlobalResponseDTO.java
    └── security/filter/HeaderAuthenticationFilter.java

src/main/resources/
├── application.yaml
└── application-prod.yaml
```

`domain`: 업무 기능별 코드.  
`global`: 추적 ID · 응답 · 보안 · 설정 등 공통 코드.  
DB 접근: Repository · JPA/QueryDSL.

## 4. 공통 처리

### 4.1. 공통 응답

공통 형식: `GlobalResponseDTO<T>`의 `code` · `message` · `data` · `traceId`. 아래 요청은 공개 최상위 지역 조회와 유효한 추적 ID의 예시이다.

```java
public record GlobalResponseDTO<T>(
        String code, String message, T data, String traceId
) { /* success(...), from(...) 팩터리 메서드 */ }
```

```http
GET /api/property/regions/root HTTP/1.1
X-Trace-Id: property-doc-example-001
```

#### 4.1.1. 정상 응답

```http
HTTP/1.1 200 OK
X-Trace-Id: property-doc-example-001
Content-Type: application/json
```

```json
{
  "code": "00",
  "message": "SUCCESS",
  "data": [{ "regionId": 11, "regionName": "서울특별시" }],
  "traceId": "property-doc-example-001"
}
```

#### 4.1.2. 오류 응답

예시 조건: 하위 지역 조회의 `parentRegionId`에 `0` 전달.  
처리: `@Min(1)` 검증 오류 → `GlobalExceptionHandler`.

```http
HTTP/1.1 400 Bad Request
X-Trace-Id: property-doc-example-001
Content-Type: application/json
```

```json
{
  "code": "E21",
  "message": "INVALID_REQUEST",
  "data": null,
  "traceId": "property-doc-example-001"
}
```

### 4.2. 요청 추적

`TraceIdFilter`: 모든 HTTP 요청의 `X-Trace-Id` 검증. 헤더 부재·빈 값·형식 오류 시 새 UUID 생성. 선택된 ID는 응답 헤더·MDC에 설정되고 요청 종료 시 MDC에서 제거된다.

| 구현 | 기능 |
| --- | --- |
| `TraceIdFilter` | `X-Trace-Id` 검증·생성, 응답 헤더와 MDC 설정·정리 |
| `TraceIdContext` | 현재 요청의 Trace ID 조회와 생성 |
| `GlobalExceptionHandler` | Business/Validation/System 예외를 공통 응답으로 변환 |
| `GlobalResponseDTO` | 정상·오류 응답의 공통 형식 제공 |

#### 4.2.1. Trace ID 생성·전파 규칙

1. **Gateway 수신**: Gateway 정책에 따라 `X-Trace-Id` 전달.
2. **Property 처리**: 영문·숫자·`.`, `_`, `-`의 1~100자 검사. 유효한 값 재사용, 그 외 새 UUID 생성.
3. **하위 연동**: Member 권한 조회에 같은 Trace ID 전달.
4. **응답·로그**: 응답 헤더·본문 `traceId`와 MDC에 설정하고 요청 종료 시 MDC 정리.

## 5. 인증·인가 아키텍처

인증·인가 경로: Gateway → Security Filter → Method Security → Service.

| 구현 | 기능 |
| --- | --- |
| Gateway | Access Token 검증 후 내부 사용자 헤더 생성 |
| `HeaderAuthenticationFilter` | `X-User-Id`, `X-User-Role`을 Spring `Authentication`으로 변환 |
| `@PreAuthorize` | 해당 어노테이션이 있는 Controller 메서드의 역할 검사 |
| Service | 리소스 소유권, Member 쓰기 권한, 매물 상태 등 업무 권한 확인 |

HTTP 경로 설정: `SecurityConfiguration`의 `anyRequest().permitAll()`.  
메서드 권한 검사: `@EnableMethodSecurity` · Controller의 `@PreAuthorize`.  
`@PreAuthorize`가 없는 API에는 해당 역할 검사가 자동 적용되지 않는다.

### 5.1. Gateway와 내부 인증 헤더 처리

1. **토큰 없음**: 공개 API 요청 처리
2. **토큰 있음**: Gateway가 검증한 사용자 ID·역할을 내부 헤더로 전달
3. **Property 인증**: `X-User-Id`, `X-User-Role`로 `Authentication` 생성
4. **보호 API**: `@PreAuthorize`와 Service의 소유권·상태·Member 권한 확인 적용. 외부 `X-User-*` 헤더 제거와 직접 접근 차단은 Gateway·네트워크 정책에서 확인 필요

## 6. 논리 삭제 조회 정책

일반 조회는 Hibernate Filter와 AOP로 논리 삭제 데이터를 제외한다. 복구 대상 조회는 필요한 Repository 메서드에서 삭제 데이터를 명시적으로 조회한다.

### 6.1. 일반 조회

```text
RestController / KafkaListener
      │ 메서드 진입
      ▼
SoftDeleteFilterAspect (@Before)
      │ Hibernate Session.enableFilter("softDelete")
      ▼
Service → Repository → MySQL
      │
      ▼
deleted_at IS NULL
```

- 적용 엔티티: `Region`, `RegionBoundary`, `ApartmentComplex`, `PropertyAddress`, `PropertyImage`, `PropertyFavorite`
- `Property` 조회: Repository 메서드에서 `deletedAtIsNull` 또는 `deletedAtIsNotNull` 조건을 명시
- 복구 대상: `deleted_at IS NOT NULL` 조건으로 조회 후 복구 처리

## 7. 데이터와 외부 연동

테이블 관계·컬럼 명세: [ERD & 데이터베이스](02-erd-and-database.md).

- MySQL: 매물·지역·단지·파일·인증·찜·신고 원본 데이터
- MinIO: 매물 이미지와 인증·신고 증빙 객체
- Kakao Local: 주소 검색과 좌표 확보
- Member: 매물 등록·수정 전 회원 쓰기 권한 확인
- Kafka: Property 상태 이벤트 전달

## 8. MSA 서비스 통합 아키텍처

### 8.1. 동기 MSA 연동: Property → Member 권한 조회

```text
Property Service
      │ GET /internal/members/{memberId}/permissions?action=...
      │ X-Trace-Id
      ▼
Member Service
      │ 회원·제재·중개사 상태 기준 권한 판단
      ▼
GlobalResponseDTO<MemberPermissionResponse>
```

- 호출 주체: Property 백엔드
- 호출 방식: Member 응답을 기다리는 Spring `RestClient`
- 책임 경계: Member는 회원 행위 허용 여부를 판단하고, Property는 매물 데이터와 매물 업무를 소유
- 연결·읽기 타임아웃: 각각 기본 `800ms`
- 장애 처리: Member의 HTTP 오류·응답 형식 오류·통신 오류는 `MEMBER_API_UNAVAILABLE`로 변환. 권한을 확인할 수 없으면 매물 등록·수정을 허용하지 않음

요청 형식과 허용·거부 응답 사유는 [Member API 명세](https://github.com/is-this-to/zip-da-member/blob/dev/zip-da-member-doc/04-api-specification.md#매물-쓰기-권한-조회)를 기준으로 한다.

### 8.2. 비동기 MSA 연동과 상태 수명주기

Property는 등록·수정·상태 변경·인증 상태 이벤트를 Kafka로 발행한다. 다른 서비스의 Property 이벤트 소비와 Member 상태 변경 이벤트의 발행·운영 활성화 기준은 V3에서 확정한다.

| 기능 영역 | 현재·V3 예정 | 서비스별 역할 |
| --- | --- | --- |
| Property 상태 이벤트 | 등록·수정·상태 변경·인증 이벤트 발행 구현 | Property: 상태 변경 정보 발행 |
| Property 이벤트 소비 | V3 예정 | 다른 서비스: 필요한 후속 처리 기준 확정 |
| Member 상태 이벤트 | 수신 코드 존재·기본 비활성, Member 발행 연동 V3 예정 | Member: 기준 상태 제공 · Property: 로컬 상태 반영 정책 검토 |
| 연동 운영 기준 | V3 예정 | 재처리·중복 처리·운영 활성화 기준 확정 |

#### 8.2.1. `<Kafka>` 상태 변경 전달(부가 연동)

Kafka는 즉시 응답이 필요하지 않은 상태 변경을 전달하고, 후속 처리는 이벤트를 수신한 서비스가 수행하는 부가 연동이다.

```text
Property 상태 변경
    ↓ 이벤트 발행
Kafka Topic: zipda.property.events.v1
    ↓ key = propertyId
다른 서비스 수신·후속 처리
```

- 트랜잭션 커밋 후 Spring 이벤트 리스너가 Kafka로 발행
- 발행 실패는 로그 기록
- Outbox·재시도·실패 이벤트 저장소는 현재 미적용
