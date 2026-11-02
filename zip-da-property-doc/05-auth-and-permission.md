# Property 인증과 권한 경계

> Property는 JWT 검증 서비스가 아니다. Gateway가 검증한 사용자 정보를 내부 헤더로 전달받고, Property는 역할·소유권·업무 권한을 판단한다.

## 1. 인증 책임

```text
Client
  Authorization: Bearer <access-token>
        ↓
API Gateway
  JWT 서명·만료 검증
  X-User-Id · X-User-Role · X-Trace-Id 생성
        ↓
Property
  SecurityContext · ActorContext 구성
  역할 · 소유권 · 매물 상태 · Member 쓰기 권한 확인
```

| 구성 요소 | 책임 |
| --- | --- |
| Client | Access Token을 Gateway에 전달 |
| Gateway | JWT 검증과 신뢰 가능한 내부 헤더 생성 |
| Property | 내부 헤더를 요청자 정보로 변환하고 기능별 권한 판단 |
| Member | 등록·수정이 가능한 회원 상태인지 응답 |

## 2. 내부 인증 정보

| 헤더 | Property 처리 |
| --- | --- |
| `X-User-Id` | 양의 정수 회원 ID로 변환 |
| `X-User-Role` | 역할 하나를 `ROLE_*` 권한과 `ActorRole`로 변환 |
| `X-Trace-Id` | 요청·응답·하위 호출·감사 이력 연결 |

현재 역할 값은 `USER`, `AGENT`, `CS_ADMIN`, `SALES_ADMIN`, `SUPER_ADMIN`이다. Controller 접근 규칙은 `USER`·`AGENT`와 `CS_ADMIN`·`SUPER_ADMIN` 중심이며, `SALES_ADMIN` 전용 API는 현재 없다.

현재 필터와 `ActorContext`는 `X-User-Role`의 단일 역할 값만 처리한다. 쉼표로 연결한 복수 역할 문자열은 개별 권한으로 분리하지 않는다.

## 3. Property 권한 판단

### 3.1. 역할 확인

Controller의 `@PreAuthorize`에서 기능별 허용 역할을 확인한다.

- 공개 조회: 지역·옵션, 공개 목록·상세·지도·인기 매물
- 회원·중개사: 내 매물·찜, 파일·위치, 등록·수정, 인증·신고
- 관리자: 매물 인증·공개 검수, 신고 처리와 이의신청 검토

### 3.2. 소유권과 상태 확인

역할이 허용되어도 서비스 계층에서 다음 조건을 다시 확인한다.

- 매물·파일·신고·이의신청의 작성자 또는 소유자
- 현재 공개·거래·인증·신고 상태에서 허용된 전이
- 요청 버전과 DB 버전 일치
- 관리자 조치 범위와 열람 사유

### 3.3. Member 쓰기 권한 확인

매물 등록·수정 전 Member 내부 API의 `allowed` 결과를 확인한다. 로컬 Member 상태가 존재하면 탈퇴·제재·중개사 비활성 상태도 추가로 차단한다.

Member 상태를 확인할 수 없으면 쓰기를 허용하지 않는다. Member 이벤트 수신기는 기본 비활성이므로 현재 등록·수정의 기준은 동기 권한 조회이다.

## 4. 관리자 열람 감사

관리자 매물 검수 상세와 신고 상세 조회는 `X-Audit-Reason`을 요구한다.

- 관리자 ID와 역할
- 열람 대상과 행위
- 열람 사유
- Trace ID와 처리 시각

증빙 파일은 권한과 사유 확인 후 제한 시간 Presigned URL로 제공한다.

## 5. Gateway 신뢰 경계

Property의 HTTP 보안 설정은 세션을 사용하지 않고, 내부 헤더를 Spring Security 인증 정보로 변환한다. URL 수준에서 모든 요청을 통과시킨 뒤 보호 API는 메서드 보안으로 제한한다.

Property는 내부 헤더의 발신자를 자체 검증하지 않으므로 운영 배포는 다음 신뢰 경계를 전제로 한다.

- 외부 클라이언트는 Gateway만 호출
- Property 포트는 외부 직접 접근 차단
- Gateway에서 검증한 내부 인증 헤더만 Property에 전달
- 서비스 간 요청은 내부 네트워크 정책으로 제한

클라이언트가 Property에 직접 접근할 수 있으면 `X-User-*` 헤더 위조가 가능하므로 Gateway와 네트워크 경계가 인증 구조의 일부이다.

## 6. 신고 연동 경계

Property에는 신고·이의신청·관리자 조치와 감사 이력이 구현되어 있다. 회원 제재 요청과 Member의 실제 제재 API 연결은 V3 예정이며, 현재 권한 기능으로 간주하지 않는다.

