# API-PR-049 및 옵션 도메인 구현 절차 체크리스트

> 옵션 정책이 변경되었습니다. 문서 끝의 **최신 옵션 정책 및 구현 상태 (2026-09-04)**를 우선 적용합니다.

## 1. 결론

**옵션 기능의 첫 API는 API-PR-049부터 구현하는 것이 맞다.**

- API-PR-049는 `propertyType`에 따라 등록 화면이 사용할 옵션 원장을 조회하는 공개 API다.
- 이 API를 먼저 완성하면 같은 조회 모델을 매물 등록·수정의 옵션 검증기에서 재사용할 수 있다.
- 다만 첫 코드가 Controller는 아니다. 계약 충돌 확인과 옵션 원장 DB 준비가 먼저다.
- API-PR-011(매물 등록)과 API-PR-012(매물 수정)의 최상위 트랜잭션 및 Controller는 임호탁 담당 범위다.
  장수린 담당 구현은 옵션 Entity·Repository·검증기·저장·이력을 제공하고 합의된 경계에서 연계하는 것이다.
- API-PR-009의 옵션 상세 응답은 필드 집합이 아직 확정되지 않았으므로 PR-049보다 먼저 구현하지 않는다.

권장 전체 순서:

```text
계약 중단 지점 확인
  -> 옵션 원장·유형 매핑 DB 준비
  -> API-PR-049 Entity/조회 Repository
  -> Response DTO
  -> 조회 Service
  -> 공개 Controller/OpenAPI
  -> API-PR-049 테스트
  -> 공용 PropertyOptionValidator
  -> property_option 현재값 저장
  -> property_option_history 이력 저장
  -> API-PR-011/012 연계
  -> API-PR-009 상세 연계
```

## 2. 검토 근거와 현재 상태

### 검토 자료

- [`AGENTS.md`](./AGENTS.md)
- [`java-spring-code-convention.md`](./java-spring-code-convention.md)
- [`JANG_SURIN_API_IMPLEMENTATION_REFERENCE.md`](./JANG_SURIN_API_IMPLEMENTATION_REFERENCE.md)
- 읽기 전용 원본 `README_문서읽는순서.md`
- 읽기 전용 원본 `요구사항명세서_상세해설서.md`, `요구사항명세서_최종확정본.xlsx`
- 읽기 전용 원본 `3인_파트별_구현가이드_v1.0.docx`, `유즈케이스명세서_개발상세확장본.docx`
- 읽기 전용 원본 `API_명세서_개발최종본.xlsx`, 최신 `API_명세서_개발최종본 수정.xlsx`
- 읽기 전용 원본 `ERD_Zip-da-project-property v1.2.png`
- 읽기 전용 원본 `_Zip-da-project-property v1.2.sql`, `MySQL8_정책확정_34테이블_전체제약.sql`
- 읽기 전용 원본 `FINAL_개발착수_기준서_v1.5.md`와 정합화·SQL 수정내역·담당 추적표
- 현재 `build.gradle`, 공통 응답·예외·OpenAPI·soft-delete 구현

판정 기준: 2026-09-03에 수정된 API Excel을 같은 계약의 최신본으로 보고, 그 외 내용은 문서 읽기 순서와
정책확정 SQL·ERD를 함께 대조했다. 읽기 전용 원본은 수정하지 않았다.

### 원본에서 확인한 API-PR-049 계약

- Method/Path: `GET /api/property-option-codes`
- 인증: `PUBLIC`
- 성공 상태: `200 OK`
- 필수 query: `propertyType`
- 허용값: `APARTMENT`, `OFFICETEL`, `VILLA`, `ROOM`
- 응답: `data.items[]`
- 최신 원본 item 필드:
  `optionCode`, `optionName`, `optionCategory`, `valueType`, `unit`, `allowedValues`, `required`, `displayOrder`
- 2026-09-04 담당자 확장 필드: `filterable`, `registrationEnabled`
- `allowedValues`: `array<string> | null`; `SINGLE_SELECT`일 때 선택 가능한 값 목록, 그 외 타입은 `null`
- `valueType`: `BOOLEAN`, `NUMBER`, `TEXT`, `SINGLE_SELECT`
- 필터: 요청 유형에서 허용된 활성 옵션
- 정렬: `displayOrder ASC`
- 필수 인수 테스트: `propertyType=ROOM`일 때 ROOM에 허용된 활성 옵션만 순서대로 반환

### 저장소 대조 결과

- [x] `PropertyType` Enum에 네 유형이 모두 존재한다.
- [x] `GlobalResponseDTO`, `GlobalExceptionHandler`, `CustomApiResponse`를 재사용할 수 있다.
- [x] `BaseAuditEntity`와 Hibernate `softDelete` 필터 기반이 존재한다.
- [x] 원본 34테이블 SQL을 실제로 확인했다. 과거 참고 문서의 “SQL 미확보” 상태는 현재 기준으로 해소됐다.
- [x] 옵션 Enum 3개, Entity 4개, Repository 4개, 응답 DTO 2개, 조회 Service와 Controller의 골격이 있다.
- [x] `database/schema/003_create_property_option_tables.sql`에 옵션 네 테이블과 Property DB 내부 FK 6개가 있다.
- [x] 최신 API Excel에 추가된 `allowedValues`가 DTO와 조회 Service에 반영돼 있다.
- [x] 현재 소스는 `compileJava --rerun-tasks`에 성공한다.
- [ ] API-PR-049 및 옵션 Validator 전용 테스트는 아직 없다.
- [ ] 전체 테스트는 `${DB_HOST}:${DB_PORT}`가 설정되지 않아 기존 `contextLoads()`에서 실패한다.
- [ ] 옵션 코드·유형별 허용 옵션의 초기 데이터가 저장소에 없다.

## 3. 구현 전 중단 지점

아래 항목은 코드나 migration을 확정하기 전에 팀 결정 또는 사용자 승인이 필요하다.

### 3.1 DB 외래키 정책 — 확정

- 원본 34테이블 SQL은 옵션 테이블에 물리 FK를 정의한다.
- 현재 [`database/README.md`](./database/README.md)는 범위를 구분하지 않고 모든 테이블 관계를 논리 FK로 설명하지만,
  기존 001 DDL과 원본 34테이블 SQL은 Property DB 내부 관계에 물리 FK를 사용한다.
- [x] 기존 정책이 서비스/DB 경계를 넘는 참조만 논리 FK로 관리한다는 것임을 2026-09-04 재확인했다.
- [x] Member처럼 다른 서비스가 소유한 ID에는 Property DB의 물리 FK를 만들지 않는다.
- [x] 같은 Property DB 내부의 `property`, 옵션 코드·매핑·현재값·revision·history 관계에는 물리 FK를 사용한다.
- [x] 현재 옵션 migration의 물리 FK 6개를 유지한다.
- [ ] DB FK가 보장하지 않는 활성·soft-delete·업무 허용 상태는 Service와 Repository에서 검증한다.
- [ ] 삭제·변경 중 참조 무결성이 깨지지 않는지 통합 테스트로 확인한다.
- [x] `database/README.md`의 외래키 문구를 서비스/DB 경계 기준으로 정정했다.

확정 정책: 다른 서비스/DB 참조는 논리 FK, 동일 Property DB 내부 참조는 물리 FK를 사용할 수 있다. 물리 FK는 존재 여부를
보장하고, 활성·soft-delete·권한·상태 같은 업무 규칙은 애플리케이션이 추가로 검증한다.

### 3.2 공통 응답과 오류 code 불일치

- 원본: `success`, `data/error`, `traceId`, `timestamp`
- 현재 코드: `code`, `message`, `data`, `traceId`
- [ ] API-PR-049는 Favorite와 동일하게 현재 `GlobalResponseDTO`를 우선 재사용할지 확인한다.
- [ ] 옵션만을 위해 `global` 응답 구조를 변경하지 않는다.
- [ ] 향후 옵션 검증 오류 422와 `fieldErrors`를 현재 공통 예외 구조에 어떻게 담을지 공통 담당자와 확정한다.
- [x] 옵션 담당자 결정으로 `OPTION_CODE_NOT_FOUND`부터 `OPTION_VALUE_REQUIRED`까지 내부 code `P14`~`P17`을 사용한다.

권장안: PR-049 조회는 현재 공통 응답을 재사용하고, 공통 envelope 변경은 별도 팀 작업으로 둔다.

### 3.3 `SINGLE_SELECT` 허용값 응답 — 최신 명세 반영 완료

- [x] 2026-09-04 담당자 결정으로 PR-049 item에 `allowedValues`를 포함한다.
- [x] `API_명세서_개발최종본 수정.xlsx`의 필드명세와 JSON 예시에도 `allowedValues`가 추가된 것을 확인했다.
- [x] 타입은 `array<string> | null`이다.
- [x] `SINGLE_SELECT`일 때 선택 가능한 코드 목록, 그 외 타입은 `null`이다.
- [x] 현재 Response DTO와 조회 Service가 이 계약을 구현한다.
- [ ] `SINGLE_SELECT` 원장에 허용값이 비어 있는 잘못된 데이터는 빈 배열로 응답할지, 시스템 설정 오류로 차단할지 정한다.
- [ ] `allowed_values_json`을 `String`으로 두 번 파싱하지 않고 JSON 타입 컬렉션으로 한 곳에서 매핑하도록 정리한다.

확정 정책: `allowedValues` 자체와 비-SINGLE_SELECT의 `null`은 더 이상 미확정 사항이 아니다. 남은 결정은 잘못 적재된
SINGLE_SELECT 원장을 어떻게 처리할지뿐이다.

### 3.4 “활성 옵션”의 정확한 조건

원본 테이블에는 다음 상태가 함께 있다.

- `property_option_code.deleted_at IS NULL`
- `property_option_code.is_active = true`
- `property_option_code.is_registration_enabled = true`
- `property_type_option.deleted_at IS NULL`

API·유즈케이스의 화면 트리거에는 필터·등록·수정이 함께 포함된다. 2026-09-04 담당자 결정에 따라 PR-049를 세 화면에서
공유하는 유형별 옵션 메타데이터 API로 사용한다.

- [x] Repository는 유형 매핑과 옵션 코드의 soft-delete 및 `is_active = true`만 공통 필터로 적용한다.
- [x] 응답에 `filterable`, `registrationEnabled`를 추가하기로 결정했다.
- [ ] Java Response DTO와 조회 Service 반영은 담당자가 직접 구현한다.
- [ ] 등록·수정 화면은 `registrationEnabled=true`인 항목만 입력 항목으로 사용한다.
- [ ] 검색 화면은 `filterable=true`인 항목만 필터 항목으로 사용한다.
- [ ] 등록·수정 Service는 클라이언트 표시 결과를 신뢰하지 않고 `registrationEnabled`를 다시 검증한다.
- [ ] 검색 Service는 요청된 옵션 조건에 대해 `filterable`을 다시 검증한다.

확정 정책: 두 Boolean에 AND 조건을 걸어 Repository에서 미리 제외하지 않는다. 한쪽 용도로만 활성화된 옵션도 메타데이터에
남겨 두고, 각 사용처가 자신의 capability 플래그를 적용한다.

### 3.5 옵션 Enum과 초기 원장 데이터

- `optionCategory`의 SQL 허용값:
  `APPLIANCE`, `FURNITURE`, `SECURITY`, `STRUCTURE`, `LIVING`, `ETC`
- `valueType`의 SQL 허용값:
  `BOOLEAN`, `NUMBER`, `TEXT`, `SINGLE_SELECT`
- 정책확정 SQL과 API·요구사항 문서가 두 Enum의 영문 코드를 일관되게 사용한다.
- 옵션 코드와 유형별 매핑 INSERT 데이터도 저장소에는 없다.

- [x] Java `OptionCategory`와 `OptionValueType`은 정책확정 SQL의 CHECK 값과 일치한다.
- [ ] 운영할 `optionCode`, 이름, 카테고리, 타입, 단위, 허용값을 확정한다.
- [ ] APARTMENT/OFFICETEL/VILLA/ROOM별 `required`, 기본값, `displayOrder`를 확정한다.
- [ ] 초기 데이터를 migration INSERT로 관리할지 별도 기준정보 관리 절차로 넣을지 확정한다.

원장 데이터가 없으면 API 코드는 정상이어도 항상 `items=[]`만 반환하므로 PR-049 완료로 볼 수 없다.

### 3.6 수정 시 옵션 제거 정책

- propertyType 변경 후 기존 유형에서만 허용되던 옵션을 자동 삭제할지, 422로 거절할지 확정되지 않았다.
- [ ] API-PR-012 연계 전 `422 거절` 또는 `사용자 확인 후 명시적 제거` 중 하나를 확정한다.

권장안: 백엔드는 자동으로 조용히 삭제하지 않고 422를 반환한다. 프런트가 사용자 확인 후 제거된 옵션 목록으로 다시 요청한다.

## 4. 목표 패키지 구조

현재 프로젝트 구조에 맞춘 제안이다. 실제 클래스 생성은 각 단계에서 하나씩 진행한다.

```text
src/main/java/com/zipdaproperty/domain/option
├── constant
│   ├── OptionCategory.java
│   └── OptionValueType.java
├── controller
│   └── PropertyOptionCodeController.java
├── entity
│   ├── PropertyOptionCode.java
│   ├── PropertyTypeOption.java
│   ├── PropertyOption.java
│   └── PropertyOptionHistory.java
├── repository
│   ├── PropertyOptionCodeRepository.java
│   ├── PropertyTypeOptionRepository.java
│   ├── PropertyOptionQueryRepository.java
│   └── PropertyOptionQueryRow.java
├── request
│   └── PropertyOptionValueRequest.java
├── response
│   └── PropertyOptionCodeListResponse.java
├── service
│   ├── PropertyOptionCodeService.java
│   └── PropertyOptionService.java
└── validator
    └── PropertyOptionValidator.java
```

PR-049만 구현하는 동안에는 `PropertyOptionCode`, `PropertyTypeOption`, 조회 Repository/row, Response, Service,
Controller만 필요하다. 현재값·이력 클래스는 API-PR-011/012 연계 단계에서 추가한다.

## 5. Phase A — API-PR-049 조회 구현

### 0단계. 계약 확정

- [ ] 3장의 중단 지점을 팀과 확인한다.
- [x] 서비스/DB 경계 밖은 논리 FK, 동일 Property DB 내부 옵션 관계는 물리 FK를 사용하는 기존 정책을 재확인했다.
- [x] 기존·수정 API Excel의 `01_API목록`, `03_필드명세`, `06_JSON예시`, `08_테스트케이스`, `09_DB매핑`에서
  API-PR-049 행을 대조했다.
- [x] PR-049 성공 응답은 단순 배열이 아니라 `data.items` 객체다.
- [x] 최신 수정본이 추가한 필드는 `allowedValues`이며 나머지 PR-049 계약은 기존본과 같다.
- [ ] 결과가 없을 때 `200 OK`, `items=[]`로 반환하는 정책을 확인한다.
- [ ] 동일한 `displayOrder`가 있을 때 `optionCode ASC`를 보조 정렬로 사용할지 확인한다.

완료 기준: 구현자가 추측해야 하는 응답 필드, 필터, 정렬, 초기 데이터가 남아 있지 않다.

### 1단계. 옵션 원장·유형 매핑 DDL 준비

대상 파일 제안:

```text
database/schema/003_create_property_option_tables.sql
database/README.md
```

- [ ] 파일 번호가 다른 팀원의 신규 SQL과 충돌하지 않는지 확인한다.
- [x] 옵션 네 테이블의 컬럼 길이와 nullability를 정책확정 SQL·ERD와 대조했다.
- [x] 동일 Property DB 내부 관계의 물리 `FOREIGN KEY`를 migration에 반영했다.
- [x] `option_code` UNIQUE와 `(property_type, option_code_id)` UNIQUE가 반영돼 있다.
- [x] 각 현재 테이블의 `display_order >= 0` CHECK가 반영돼 있다.
- [x] 정책확정 SQL의 Boolean CHECK 12개를 003에 보완했다.
- [x] `property_option_history`의 전·후 displayOrder CHECK 2개와 timeline 인덱스를 보완했다.
- [x] 정책확정 SQL보다 강화된 `chk_property_option_value_count`를 추가해 typed value 정확히 하나를 DB에서도 막는다.
- [ ] soft-delete 후 같은 코드/매핑을 재생성할지 기존 행을 복구할지 결정한다.
- [ ] 확정된 옵션 원장과 유형별 매핑 초기 데이터를 넣는다.
- [x] `database/README.md` 실행 순서에 003을 추가했다.

코드 설명:

- `property_option_code`는 옵션의 공통 이름·카테고리·값 타입을 보관한다.
- `property_type_option`은 같은 옵션이 어떤 매물 유형에 허용되는지와 유형별 필수 여부·표시 순서를 보관한다.
- 응답의 `displayOrder`는 코드 원장의 순서가 아니라 유형별 매핑의 `display_order`를 사용해야 한다.
- 원본 SQL의 PK는 TSID가 아니라 `AUTO_INCREMENT BIGINT`다. API에 내부 PK를 노출하지 않는다.

완료 기준: ROOM을 포함한 네 유형에 대해 조회 가능한 실제 기준 데이터가 존재한다.

### 2단계. Enum 구현

대상 파일:

```text
constant/OptionCategory.java
constant/OptionValueType.java
```

- [x] SQL CHECK 값과 동일한 영문 상수만 선언했다.
- [x] 화면 한글 라벨을 Enum에 넣지 않았다. `optionName`은 DB 원장이 담당한다.
- [x] `PropertyType`은 새로 만들지 않고 기존 `domain.property.constant.PropertyType`을 재사용했다.

코드 형태:

```java
public enum OptionValueType {
    BOOLEAN,
    NUMBER,
    TEXT,
    SINGLE_SELECT
}
```

완료 기준: DB/API/Vue에서 사용하는 문자열과 Java Enum 이름이 정확히 일치한다.

### 3단계. 조회용 Entity 구현

대상 파일:

```text
entity/PropertyOptionCode.java
entity/PropertyTypeOption.java
```

- [x] 네 Entity 모두 `BaseAuditEntity`를 확장한다.
- [x] PK는 `@GeneratedValue(strategy = GenerationType.IDENTITY)`로 매핑했다.
- [x] Enum 컬럼은 `@Enumerated(EnumType.STRING)`을 사용했다.
- [x] JPA 컬럼 길이와 nullability는 원본 SQL과 일치한다.
- [ ] `@Filter(name = "softDelete")`를 적용한다.
- [x] Entity에 `@Data`나 public setter를 추가하지 않았다.
- [x] API 응답에 Entity를 직접 반환하지 않는다.
- [x] 동일 DB 내부 관계이므로 `PropertyTypeOption`에서 `PropertyOptionCode`를 LAZY 연관관계로 매핑했다.
- [ ] 연관관계에는 cascade를 두지 않고, 조회 시 fetch join·EntityGraph·projection 중 하나로 N+1을 방지한다.
- [ ] `allowed_values_json`을 현재의 단순 `String + columnDefinition`이 아니라 `@JdbcTypeCode(SqlTypes.JSON)` 기반으로
  명시적으로 매핑하고 파싱 책임을 한 곳으로 모은다.

`allowed_values_json`은 기존 `PropertyRevision`의 JSON 매핑 방식을 참고할 수 있다. 단, 검증기에서 문자열을 매번 임의 파싱하지
않도록 JSON 표현 방식과 변환 책임을 한 곳으로 고정한다.

완료 기준: 애플리케이션 시작 시 Entity와 실제 두 테이블의 매핑 오류가 없다.

### 4단계. 조회 projection과 Repository 구현

대상 파일:

```text
repository/PropertyOptionQueryRow.java
repository/PropertyOptionQueryRepository.java
```

- [ ] 필요한 응답 필드만 projection으로 한 번에 조회한다. 현재는 Entity 조회 후 변환한다.
- [x] `property_type_option`과 `property_option_code` 양쪽에 `deletedAt IS NULL`을 명시했다.
- [x] 코드 원장의 `isActive = true`를 적용했다.
- [x] 공용 메타데이터 조회이므로 Repository에서 `registrationEnabled`나 `filterable`을 공통 제외 조건으로 사용하지 않는다.
- [x] `propertyType`을 필수 조건으로 적용했다.
- [ ] `typeOption.displayOrder ASC, optionCode.optionCode ASC`로 결정적 정렬을 적용한다.
- [ ] item마다 코드 원장을 다시 조회하는 N+1을 만들지 않는다.

개념 쿼리:

```text
FROM PropertyTypeOption typeOption
JOIN PropertyOptionCode optionCode
  ON optionCode.optionCodeId = typeOption.optionCodeId
WHERE typeOption.propertyType = :propertyType
  AND typeOption.deletedAt IS NULL
  AND optionCode.deletedAt IS NULL
  AND optionCode.isActive = true
ORDER BY typeOption.displayOrder ASC, optionCode.optionCode ASC
```

Repository는 HTTP 응답 wrapper를 알지 않는다. 순수 조회 row 목록만 반환한다.

완료 기준: 단일 쿼리로 해당 유형의 활성·허용 옵션만 정렬해 가져온다.

### 5단계. Response DTO 구현

대상 파일:

```text
response/PropertyOptionCodeListResponse.java
```

- [x] Java `record`로 DTO를 만들었다.
- [x] 조회 Service의 빈 결과는 `items=[]`가 된다.
- [x] 내부 DB ID와 감사·삭제 필드는 포함하지 않는다.
- [x] 최신 명세의 여덟 필드를 구현했다.
- [ ] 담당자 확장 필드 `filterable`, `registrationEnabled`의 Java 반영은 아직 남아 있다.
- [x] `PropertyOptionCodeResponse`에 `allowedValues`를 추가했다.
- [x] 비-SINGLE_SELECT의 `allowedValues=null`을 적용했다.
- [ ] 잘못 적재된 SINGLE_SELECT 허용값 없음의 처리 정책을 확정한다.
- [ ] 전달받은 목록은 `List.copyOf` 등으로 외부 변경을 막는다.

권장 모양:

```java
public record PropertyOptionCodeListResponse(List<Item> items) {
    public record Item(
            String optionCode,
            String optionName,
            OptionCategory optionCategory,
            OptionValueType valueType,
            String unit,
            List<String> allowedValues,
            boolean filterable,
            boolean registrationEnabled,
            boolean required,
            int displayOrder
    ) {}
}
```

완료 기준: 원본 JSON처럼 `data.items[]`가 만들어지고 Entity가 직렬화되지 않는다.

### 6단계. 조회 Service 구현

대상 파일:

```text
service/PropertyOptionCodeService.java
```

- [x] 클래스에 `@Transactional(readOnly = true)`를 선언했다.
- [x] Repository 결과를 Response DTO로 변환한다.
- [x] DB가 정렬한 순서를 보존한다.
- [x] 결과가 없으면 예외가 아니라 `items=[]`를 반환한다.
- [x] PUBLIC 조회이므로 `ActorContext`나 Member permission을 요구하지 않는다.
- [ ] 기준서의 옵션 메타데이터 5분 TTL 캐시를 적용한다.
- [ ] 옵션 원장 변경 시 캐시 즉시 무효화 경계를 구현한다.

권장 진입점:

```java
public PropertyOptionCodeListResponse getOptionCodes(PropertyType propertyType)
```

완료 기준: Controller 없이도 네 `PropertyType` 각각의 DTO 결과를 조회할 수 있다.

### 7단계. 공개 Controller와 OpenAPI 구현

대상 파일:

```text
controller/PropertyOptionCodeController.java
```

- [x] `GET /api/property-option-codes`를 정확히 매핑했다.
- [x] 클래스에 `@Validated`를 적용했다.
- [x] `@RequestParam PropertyType propertyType`으로 필수 query를 받는다.
- [x] Controller는 Service 호출과 `200 OK` 응답만 담당한다.
- [x] 현재 프로젝트의 `GlobalResponseDTO.success(response)`를 재사용한다.
- [x] PUBLIC API이므로 `@PreAuthorize`를 붙이지 않았다.
- [ ] OpenAPI에서 전역 bearer 인증 요구가 이 공개 API에 표시되지 않도록 공개 security override를 검증한다.
- [ ] 잘못된 Enum 문자열과 query 누락이 `INVALID_REQUEST` 400으로 변환되는지 확인한다.
- [ ] `@CustomApiResponse`에는 실제로 발생 가능한 공통 오류만 선언한다.

권장 시그니처:

```java
@GetMapping("/api/property-option-codes")
public ResponseEntity<GlobalResponseDTO<PropertyOptionCodeListResponse>> getOptionCodes(
        @RequestParam PropertyType propertyType
) {
    return ResponseEntity.ok(
            GlobalResponseDTO.success(optionCodeService.getOptionCodes(propertyType))
    );
}
```

완료 기준: 인증 헤더 없이 호출 가능하고 OpenAPI의 Method/Path/query/200 응답이 실제 구현과 일치한다.

### 8단계. API-PR-049 테스트

#### Repository/통합 테스트

- [ ] ROOM 허용 매핑, 다른 유형 매핑, 삭제된 매핑, 비활성 코드, 삭제된 코드를 fixture로 준비한다.
- [ ] ROOM 조회에 ROOM 허용 항목만 나온다.
- [ ] `displayOrder` 오름차순이다.
- [ ] 같은 순서에서는 `optionCode` 보조 정렬이 결정적이다.
- [ ] soft-delete된 두 테이블의 행이 모두 제외된다.
- [ ] 확정된 경우 등록 비활성 코드가 제외된다.

#### Service 단위 테스트

- [ ] Repository row가 Response item으로 정확히 변환된다.
- [ ] 빈 결과가 `items=[]`다.
- [ ] `unit=null`이 정상적으로 보존된다.

#### Controller/계약 테스트

- [ ] `GET ...?propertyType=ROOM`은 200이다.
- [ ] query 누락은 400 `INVALID_REQUEST`다.
- [ ] 알 수 없는 유형은 400 `INVALID_REQUEST`다.
- [ ] POST 등 잘못된 Method는 405 `METHOD_NOT_ALLOWED`다.
- [ ] 응답에 내부 ID·감사 필드가 없다.
- [ ] 응답의 `filterable`, `registrationEnabled`가 DB 원장 값과 일치한다.
- [ ] 등록 화면과 검색 화면이 각각 자신의 capability 플래그만 적용한다.
- [ ] 인증 헤더 없이 접근 가능하다.
- [ ] `/v3/api-docs` 또는 현재 설정의 `/api-docs`에 계약이 노출된다.

테스트용 DB 라이브러리를 새로 추가해야 한다면 먼저 승인을 받는다. MySQL의 JSON·CHECK·soft-delete 동작을 검증해야 하므로
단순 H2 대체만으로 Repository 완료를 판정하지 않는다.

검증 명령:

```powershell
.\gradlew.bat compileJava
.\gradlew.bat test
```

## 6. Phase B — 매물 등록·수정용 옵션 검증기

PR-049가 완료된 다음 같은 정책 원장을 이용해 검증기를 구현한다. 이 단계의 오류는 API-PR-011/012에서 422와 field error로
반환되어야 한다.

### 9단계. 옵션 입력 모델 확정

- [ ] 요청 item에 `optionCode`, `booleanValue`, `numberValue`, `textValue`를 둘지 API-PR-011/012 담당자와 확정한다.
- [ ] `numberValue`는 DB `DECIMAL(12,2)`와 맞게 `BigDecimal`을 사용한다.
- [ ] `booleanValue=false`를 “값 없음”으로 판단하지 않는다. null 여부로 값 존재를 판정한다.
- [ ] 같은 `optionCode`의 중복 입력을 허용하지 않는다.
- [ ] 옵션 배열이 없거나 비어 있을 때 필수 옵션 검사를 수행할지 확정한다.

### 10단계. `PropertyOptionValidator` 구현

- [ ] 요청된 optionCode를 한 번의 batch 조회로 가져온다.
- [ ] 존재하지 않거나 비활성인 코드는 `OPTION_CODE_NOT_FOUND` 422다.
- [ ] 현재 propertyType에서 허용되지 않은 코드는 `OPTION_NOT_ALLOWED_FOR_PROPERTY_TYPE` 422다.
- [ ] 유형별 필수 옵션 누락은 `OPTION_VALUE_REQUIRED` 422다.
- [ ] `BOOLEAN`은 `booleanValue` 하나만 non-null이어야 한다.
- [ ] `NUMBER`는 `numberValue` 하나만 non-null이어야 한다.
- [ ] `TEXT`는 `textValue` 하나만 non-null이어야 한다.
- [ ] `SINGLE_SELECT`는 `textValue` 하나만 사용하고 `allowed_values_json`에 포함돼야 한다.
- [ ] 둘 이상의 typed value가 설정돼도 `OPTION_VALUE_TYPE_MISMATCH` 422다.
- [ ] 주차 등 Property 정형 필드와 중복 저장하면 안 되는 옵션 정책을 원장과 함께 확인한다.
- [ ] 모든 오류에 Vue 입력과 연결 가능한 `options[index].필드명` field path를 제공한다.
- [ ] 검증기는 Controller나 `GlobalResponseDTO`에 의존하지 않는다.

권장 처리 흐름:

```text
중복 optionCode 확인
  -> 유형의 전체 활성·허용 정책 batch 조회
  -> 요청 코드 존재/활성 확인
  -> 유형 허용 확인
  -> required 누락 확인
  -> valueType별 non-null 필드 개수 확인
  -> SINGLE_SELECT 허용값 확인
  -> 검증 완료된 값 반환
```

### 11단계. 검증기 단위 테스트

- [ ] 정상 BOOLEAN/NUMBER/TEXT/SINGLE_SELECT 값
- [ ] `booleanValue=false` 정상 처리
- [ ] optionCode 중복
- [ ] 없는 코드와 비활성 코드
- [ ] 다른 propertyType의 코드
- [ ] 필수 옵션 누락
- [ ] 타입 필드 없음
- [ ] 타입 필드 둘 이상
- [ ] BOOLEAN에 textValue 전달
- [ ] SINGLE_SELECT 허용 목록 밖의 값
- [ ] 요청 option 수에 관계없이 Repository batch 조회 횟수가 일정함

## 7. Phase C — 옵션 현재값·이력 및 API-PR-011/012 연계

### 12단계. `property_option`과 `property_option_history` DDL/Entity

현재 DDL 파일:

```text
database/schema/003_create_property_option_tables.sql
```

- [x] `property_option`과 `property_option_history`의 동일 DB 내부 물리 FK를 작성했다.
- [ ] 참조 ID 컬럼·FK·인덱스·Service 검증을 원본 SQL과 대조해 완성한다.
- [x] `database/README.md` 실행 순서에 003을 추가했다.
- [ ] 값 컬럼은 `boolean_value`, `number_value`, `text_value` 중 타입에 맞는 하나만 저장한다.
- [ ] 애플리케이션 검증뿐 아니라 가능한 DB CHECK도 함께 정의한다.
- [ ] `(property_id, option_code_id)` UNIQUE와 soft-delete 후 재추가 정책을 확정한다.
- [ ] 재추가 시 기존 행 복구인지 새 행 생성인지 결정한다. 원본의 일반 UNIQUE는 새 행 생성을 막는다.
- [ ] `PropertyOption`은 현재 상태를, `PropertyOptionHistory`는 append-only 사건을 표현한다.
- [ ] 이력 Entity에는 public 수정 메서드를 열지 않는다.

### 13단계. 저장 Service 구현

- [ ] 검증 완료 전 Entity를 생성·변경하지 않는다.
- [ ] optionCode별로 현재 행을 일괄 조회한다.
- [ ] 추가·변경·삭제 집합을 계산한다.
- [ ] 삭제는 물리 DELETE가 아니라 `deleted_at`을 기록한다.
- [ ] 변경된 옵션마다 하나의 history 행을 append한다.
- [ ] history를 현재 `PropertyRevision`과 연결한다.
- [ ] item별 조회·저장으로 N+1을 만들지 않는다.
- [ ] 내부 ID나 `allowed_values_json`을 API 응답에 노출하지 않는다.

### 14단계. API-PR-011/012 통합

- [ ] 임호탁 담당 최상위 Property 쓰기 Service가 검증기와 저장 Service를 호출하도록 합의한다.
- [ ] Property, option 현재값, revision, option history가 하나의 로컬 트랜잭션에서 commit/rollback된다.
- [ ] 옵션 Service가 독립적인 최상위 쓰기 트랜잭션을 만들어 부분 commit하지 않는다.
- [ ] API-PR-011 실패 시 매물과 옵션이 모두 생성되지 않는다.
- [ ] API-PR-012 version 충돌 시 옵션과 이력이 모두 변경되지 않는다.
- [ ] propertyType 변경 시 3.6에서 확정한 제거 정책을 적용한다.
- [ ] 오류 code와 fieldErrors가 합의된 공통 envelope로 반환된다.

### 15단계. 통합 테스트

- [ ] APARTMENT에 허용되지 않은 optionCode는 422이고 매물이 생성되지 않는다.
- [ ] BOOLEAN 옵션에 textValue를 보내면 422이고 매물이 생성되지 않는다.
- [ ] 필수 옵션 누락은 422다.
- [ ] 정상 생성 시 typed value 하나만 저장된다.
- [ ] 수정 시 현재값과 append-only 이력이 같은 revision에 연결된다.
- [ ] 실패한 수정은 현재값·revision·history를 모두 rollback한다.
- [ ] propertyType 변경 후 비허용 옵션이 남지 않는다.
- [ ] soft-delete된 코드·매핑을 저장에 사용할 수 없다.

## 8. Phase D — API-PR-009 공개 상세 연계

이 단계는 상세 응답 필드 계약이 확정된 뒤에만 진행한다.

### 16단계. 공개 상세 옵션 조회

- [ ] 상세에 옵션을 배열/카테고리 그룹 중 어떤 형태로 반환할지 확정한다.
- [ ] `is_detail_visible = true`, 활성 코드, 활성 현재값만 반환한다.
- [ ] 정확주소·좌표 등 다른 비공개 필드를 함께 노출하지 않는다.
- [ ] 상세 item마다 옵션을 다시 조회하는 N+1을 피한다.
- [ ] 공개 불가 매물에는 옵션 DTO도 반환하지 않는다.

## 9. 단계별 진행 기록

각 구현 대화가 끝날 때 이 표와 관련 체크박스를 갱신한다.

| 단계 | 상태 | 주요 파일 | 결정·검증 기록 |
|---:|---|---|---|
| 0 | 대부분 완료 / seed 대기 | 이 문서 | FK·`allowedValues`·Enum·공용 메타데이터 용도·P14~P17 확정 |
| 1 | 대부분 완료 | `003_create_property_option_tables.sql` | 네 테이블·내부 FK 6개·값 개수/Boolean/이력 CHECK·인덱스 작성됨. seed만 대기 |
| 2 | 완료 | `type/OptionCategory`, `OptionValueType`, `OptionChangeType` | 정책확정 SQL의 영문 코드와 일치 |
| 3 | 부분 완료 | 옵션 Entity 4개 | 컬럼·LAZY 연관관계 골격 구현됨. soft-delete 필터·JSON 명시 매핑·생성/변경 메서드 보완 필요 |
| 4 | 수정 필요 | `PropertyTypeOptionRepository` | 공용 soft-delete·active 조회 구현됨. N+1과 보조 정렬 보완 필요 |
| 5 | 대부분 완료 | 옵션 Response 2개 | 최신 수정 Excel의 `allowedValues`까지 구현됨. 방어적 복사·OpenAPI 계약 테스트 필요 |
| 6 | 부분 완료 | `PropertyOptionQueryService` | readOnly 조회·응답 변환 구현됨. JSON 중복 파싱·5분 캐시·조회 구조 개선 필요 |
| 7 | 부분 완료 | `PropertyOptionController` | Method/Path/PUBLIC/200 구현됨. PUBLIC OpenAPI security override·API 문서 검증 필요 |
| 8 | 미착수 | PR-049 테스트 | 옵션 관련 테스트 파일 없음. ROOM·삭제·비활성·정렬·계약 테스트 필요 |
| 9~11 | 부분 완료 | `OptionValueValidator` | 타입별 값 검증 구현됨. 유형 허용·활성·required·중복·fieldErrors·batch 조회 미구현 |
| 12~15 | 초기 골격 | 현재값·이력 Entity/Repository | Entity·Repository 골격만 존재. 저장 Service·revision/history 생성·트랜잭션 연계 미구현 |
| 16 | 계약 대기 | PR-009 상세 | 상세 옵션 응답 확정 후 진행 |

### 현재 코드 정밀 검토에서 확인된 수정점

#### PR-049 완료 전에 반드시 처리

- [ ] `PropertyTypeOptionRepository`를 fetch join·`@EntityGraph`·projection 중 하나로 바꿔
  `typeOption.getOptionCode()`가 항목 수만큼 추가 SQL을 만들지 않게 한다.
- [x] PR-049를 등록·수정·검색 공용 메타데이터로 확정하고 capability Boolean 적용 기준을 고정했다.
- [ ] 동일 `displayOrder`에서 `optionCode ASC` 보조 정렬을 추가한다.
- [ ] 옵션 Entity 네 개에 저장소 공통 `@Filter(name = "softDelete")` 정책을 적용한다.
- [ ] `allowed_values_json`을 Hibernate JSON 타입으로 명시 매핑하고 Service와 Validator의 중복 파서를 제거한다.
- [ ] 기준서에 확정된 옵션 메타데이터 5분 TTL 캐시와 원장 변경 시 무효화를 구현한다.
- [ ] 최신 Excel의 `allowedValues` 포함 여부·비-SINGLE_SELECT `null`을 Controller/OpenAPI 테스트로 고정한다.
- [ ] ROOM 정상 조회, 다른 유형·삭제 매핑·삭제 코드·비활성 코드 제외, 정렬, 빈 목록 테스트를 작성한다.
- [ ] 공개 API인데 전역 bearer 요구가 붙는 현재 OpenAPI 설정을 이 operation에서 해제하고 문서 결과를 검증한다.
- [ ] 실제 기준정보 seed가 없는 상태를 해결한다. 지금은 깨끗한 DB에서 API가 항상 `items=[]`다.

#### DDL에서 보완

- [x] 옵션 코드 원장의 `is_filterable`, `is_detail_visible`, `is_registration_enabled`, `is_active` Boolean CHECK를 추가했다.
- [x] 유형 매핑의 `is_required`, nullable `default_boolean_value` Boolean CHECK를 추가했다.
- [x] 현재값의 nullable `boolean_value`, `verified` Boolean CHECK를 추가했다.
- [x] 이력의 전·후 `display_order >= 0`, Boolean 값, `verified` CHECK와
  `(property_option_id, occurred_at)` timeline 인덱스를 추가했다.
- [ ] `003_create_property_option_tables.sql` 파일 끝 개행을 추가한다.
- [x] `database/README.md` 실행 순서에 003을 추가했다.

#### API-PR-011/012 연계 전에 반드시 처리

- [ ] 현재 `OptionValueValidator`는 전달된 한 항목의 typed value만 검사한다. 코드 존재·활성, propertyType 허용,
  유형별 required 누락, 중복 optionCode, batch 조회, fieldErrors를 담당하는 상위 `PropertyOptionValidator`를 구현한다.
- [ ] `PropertyOption`에 검증 완료 값으로만 생성·수정·소프트 삭제·복구할 수 있는 도메인 메서드를 만든다.
- [ ] `PropertyOptionHistory`에 변경 전·후 스냅샷을 담는 생성 경로를 만들고 이후 수정은 열지 않는다.
- [ ] `property_option` 추가·변경·삭제 집합을 계산해 현재값과 history를 함께 저장하는 Service를 구현한다.
- [ ] 임호탁의 상위 Command가 먼저 만든 `propertyRevisionId`를 받아 같은 로컬 트랜잭션에 참여한다.
- [x] 옵션 담당자 승인에 따라 P14~P17 내부 숫자 코드를 확정했다.

#### 코드 품질 정리

- [ ] DTO 목록에 방어적 복사를 적용한다.
- [ ] wildcard JPA import와 파일 끝 개행을 저장소 컨벤션에 맞게 정리한다.
- [ ] `SINGLE_SELECT` 허용값이 없거나 JSON 모양이 배열 문자열이 아닐 때의 데이터 무결성 오류 정책을 테스트한다.

## 10. 다음 대화에서 시작할 첫 작업

003 DDL 교정과 README 실행 순서 반영은 완료했다. 다음 작업은
**PR-049 Repository의 N+1 제거와 결정적 보조 정렬**이다.

그 뒤 실제 옵션 원장·유형별 매핑 seed를 누가 어떤 데이터로 제공하고 어디서 관리할지만 확정하면 된다.

2026-09-04 검증에서 `compileJava --rerun-tasks`는 성공했다. 전체 `test`는 옵션 로직 실패가 아니라
`${DB_HOST}:${DB_PORT}` 미설정으로 기존 `contextLoads()`의 ApplicationContext가 시작되지 않아 실패했다. 옵션 전용 자동
테스트는 아직 없다.

## 11. 최종 완료 점검

- [ ] Controller -> Service -> Repository 의존 방향을 지켰다.
- [ ] Entity를 API 응답으로 반환하지 않는다.
- [ ] PUBLIC 접근과 OpenAPI 표기가 일치한다.
- [ ] query 누락·잘못된 Enum이 400으로 처리된다.
- [ ] 유형·활성·soft-delete·등록가능 조건이 모든 조회와 검증에 일관되다.
- [ ] `displayOrder`와 보조 정렬이 결정적이다.
- [ ] 타입값은 정확히 하나만 저장된다.
- [ ] SINGLE_SELECT 값은 허용 목록으로 검증된다.
- [ ] 필수 옵션과 field error 계약이 지켜진다.
- [ ] 매물·옵션·revision·history의 트랜잭션 원자성이 보장된다.
- [ ] 이력은 append-only다.
- [ ] 내부 PK·감사 필드·삭제 필드가 응답과 로그에 노출되지 않는다.
- [ ] `compileJava`, 관련 테스트, 전체 `test`, OpenAPI 계약 검증이 통과한다.
- [ ] 원본 Excel과 외부 SQL은 수정하지 않았다.
# 최신 옵션 정책 및 구현 상태 (2026-09-04)

이 절은 앞에 남아 있는 이전 타입별 값·JSON·물리 FK·활성 UNIQUE 관련 설명보다 우선한다.

- [x] 옵션 현재값을 `option_value VARCHAR(300)` / Java String으로 통합했다.
- [x] 허용 입력을 정확히 `있음`·`없음`으로 검증한다.
- [x] 기본값을 nullable `default_value`, 이력을 nullable `before_value`·`after_value`로 변경했다.
- [x] 이력의 변경 필드 목록은 `changed_fields VARCHAR(500)` 쉼표 구분 문자열로 변경했다.
- [x] 옵션 value_type·unit·allowedValues·SINGLE_SELECT·JSON 매핑 및 파싱을 제거했다.
- [x] 논리 ID 기반 일괄 조회와 표시 순서·옵션 코드·매핑 ID 정렬을 유지했다.
- [x] 옵션의 물리 FK·활성 UNIQUE를 사용하지 않는다. 원장 코드·이력 UNIQUE는 유지한다.
- [x] 옵션 조회는 PropertyOptionQueryDSLRepository로 통합했다. PropertyOption·PropertyTypeOption·옵션 원장 조회를 포함한다.
- [x] 기존 JpaRepository는 기본 저장용으로 유지하며, 활성 중복 가능 조회는 List로 받는다.
- [x] 응답 record 목록은 방어적 복사를 사용한다.
- [x] compileJava 및 옵션 전용 테스트 15개가 통과했다 (Validator 3, Repository 분리 1, QueryDSL 조건 6, Service·DTO 5).
- [ ] QueryDSL 메타데이터 테스트·Mockito 테스트와 별도로 실제 DB의 쿼리 실행 및 N+1을 검증한다.
- [ ] SQL 통합 후 MySQL의 실제 저장·조회·CHECK와 쿼리 수를 검증한다.
- [ ] PR-049 변경 응답을 프런트 및 원본 API 계약에 반영한다.
- [ ] 등록·수정 흐름에 문자열 Validator를 연결하고 참조·중복·동시성 정책을 검증한다.

이번 변경은 옵션에만 적용했다. 001 및 다른 도메인은 수정하지 않았다.
