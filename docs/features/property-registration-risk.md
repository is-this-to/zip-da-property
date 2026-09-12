# 매물 등록 전 허위·중복 위험검사

## 목적

FR-PR-038에 따라 매물 저장 전에 주소, 가격, 등록자, 이미지 SHA-256,
제목과 설명을 검사한다. 판정 점수와 규칙 코드는 내부 이력으로 보존하고
공개 응답에는 노출하지 않는다.

연계 요구사항은 FR-PR-016, FR-PR-027, FR-PR-049다. 이번 구현은
임호탁 담당인 등록 전 자동 판정까지만 처리하며, 신고 접수, 이의신청과
관리자 운영 조치 API는 해당 담당 영역에서 평가 이력을 참조해 연결한다.

## 처리 흐름

1. Member 등록 권한, Region과 가격 조합을 기존 정책으로 검증한다.
2. 주소를 검증하고 도로명 주소 우선 규칙으로 SHA-256 주소 해시를 만든다.
3. 요청 파일의 소유자, 매물 이미지 용도, VERIFIED 상태와 SHA-256을 확인한다.
4. 활성 매물에서 동일 주소·가격·등록자 및 이미지 SHA-256 후보를 조회한다.
5. 제목·설명의 금칙어, 외부 연락유도 패턴과 가격 범위를 검사한다.
6. `property_risk_assessment`에 규칙 코드, 점수와 결정을 별도 트랜잭션으로 기록한다.
7. 90점 이상이면 매물을 저장하지 않고 차단한다. 그 미만이면
   `property.risk_score`를 저장하고 기존 등록 트랜잭션을 계속한다.

## 초기 판정 규칙

| 규칙 코드 | 점수 | 의미 |
| --- | ---: | --- |
| `SAME_ADDRESS_PRICE_PUBLISHER` | 70 | 동일 주소·가격·등록자의 활성 매물 존재 |
| `DUPLICATE_IMAGE_CHECKSUM` | 70 | 동일 이미지 SHA-256의 활성 매물 존재 |
| `SAME_ADDRESS_IMAGE` | 90 | 동일 주소와 동일 이미지의 활성 매물 존재 |
| `ABNORMAL_PRICE` | 30 | 초기 운영 가격 범위를 벗어남 |
| `FORBIDDEN_WORD` | 30 | 제목 또는 설명에 금칙어 포함 |
| `EXTERNAL_CONTACT_INDUCEMENT` | 40 | 전화번호·외부 메신저·URL 패턴 포함 |

점수는 합산 후 100점으로 제한한다. 70점 이상은 `IN_REVIEW`,
90점 이상은 `BLOCKED`, 나머지는 `PASS`다. 신규 매물의 공개상태는 기존 정책상
항상 `IN_REVIEW`로 시작하며 위험점수는 운영 검수 우선순위에 사용한다.

가격 범위와 텍스트 규칙은 현재 명세에 상세 수치와 사전이 없어 보수적인
초기 운영값으로 구현했다. 운영 정책이 확정되면 이 정책 클래스와 테스트를
같이 변경해야 한다.

## 오류 응답

- `P30 PROPERTY_REGISTRATION_RISK_BLOCKED`: 누적 위험점수 90점 이상
- `P31 PROPERTY_DUPLICATE_DETECTED`: 동일 주소·동일 이미지 활성 매물 탐지

## 데이터와 보안

- 정확 주소와 `normalized_address_hash`는 응답이나 일반 로그에 기록하지 않는다.
- 이미지 checksum과 중복 후보 ID는 내부 판정에만 사용한다.
- 차단 결과는 별도 트랜잭션으로 저장해 등록 본문 롤백과 분리한다.
- `property_risk_assessment.property_id`는 차단된 등록 시도에서도 생성되므로
  `property`와 물리 외래키를 맺지 않는다.

## 배포와 복구

애플리케이션 배포 전에 `database/schema/015_create_property_risk_assessment_table.sql`을
실행한다. 신규 테이블만 추가하므로 기존 버전과 호환된다. 장애 시에는
위험검사 호출을 비활성화하는 forward fix를 먼저 적용하고, 감사 목적의 평가
이력 보존 여부를 확인한 후 테이블 제거 여부를 결정한다.

## 검증

- `PropertyRegistrationRiskPolicyTest`
- `PropertyRegistrationRiskServiceTest`
- `PropertyCreateServiceTest`
- `PropertyRegistrationRiskQueryRepositoryIntegrationTest`
  - `PROPERTY_RISK_QUERY_TEST_ENABLED=true`일 때 실제 MySQL에서 실행한다.
  - `001`부터 `014`까지의 기존 스키마가 먼저 적용되어 있어야 한다.
    특히 `property_image` 테이블과 인증 갱신 컬럼이 없는 구형 로컬 DB에서는
    애플리케이션 조회 자체가 실패하므로 스키마를 최신화한 뒤 실행한다.
