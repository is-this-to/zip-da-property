# API-PR-049 및 옵션 도메인 통합 구현 가이드

> 대상: 장수린 / 집·학원 공통 작업 문서
> 범위: PR-049 조회 및 PR-011·012에 연결할 옵션 검증·저장·이력

앞으로 옵션 작업의 상태와 정책은 이 문서를 기준으로 관리한다.
## 1. 이 문서를 읽는 기준


이번 통합은 문서 편집이다. 실제 Java 프로젝트, 실행 중인 DB, Slack, Notion, 원본 Excel을 다시 조회하거나 컴파일한 것은 아니다. 아래 상태를 구분한다.

- **완료 보고**: 사용자가 전달한 최신 코덱스 작업 결과에 명시된 내용
- **문서 기록**: 현재 소스에서 재확인하지 않은 내용
- **확인 필요**: 문서끼리 충돌하거나 최신 보고만으로 완료 여부를 판단할 수 없는 내용
- 미체크 항목은 무조건 미구현이라는 뜻이 아니라, 다음 작업에서 확인할 항목도 포함한다.

## 2. 기능 범위와 담당 경계

| API | 역할 | 옵션 작업과의 관계 |
|---|---|---|
| PR-049 `GET /api/property-option-codes` | 유형별 허용 옵션 목록 조회 | 조회 구현 완료 보고. 저장 API가 아님 |
| PR-011 `POST /api/properties` | 매물 등록 | 요청의 `options[]`를 옵션 내부 서비스에서 검증·저장 |
| PR-012 `PATCH /api/properties/{propertyId}` | 매물 수정 | 옵션 추가·변경·soft delete와 이력 처리 |
| PR-009 `GET /api/properties/{propertyId}` | 공개 매물 상세 | 옵션 상세 응답 계약 확정 후 연동 |

대화와 기존 문서 기준으로 PR-011·012의 최상위 Controller와 매물 쓰기 트랜잭션은 임호탁 담당이다. 장수린은 옵션 조회·검증·현재값·이력 처리 로직을 제공한다. 연동 시 실제 담당 코드와 합의된 경계를 확인한다.

별도의 `/api/property-options` 등록·수정·삭제 API를 만들지 않는다. `PropertyOptionCommandService`는 가능한 내부 서비스명 예시이지, 이미 존재하는 클래스나 확정된 이름이 아니다.

## 3. 유지할 옵션 정책

### 3.1 옵션 값과 응답

- 현재값은 `option_value`의 Java `String`으로 다룬다.
- 최신 리뷰 문서의 목표 DB 형식은 `VARCHAR(5)`이며 허용값은 정확히 소문자 문자열 `"true"`, `"false"`다.
- 기본값은 nullable `default_value`, 이력 값은 nullable `before_value`, `after_value`로 관리한다. 구체적인 null 의미와 CHECK는 실제 SQL에서 확인한다.
- 이력의 `changed_fields`는 JSON이 아닌 쉼표 구분 문자열이라는 문서 기록을 기준으로 실제 매핑을 확인한다.
- 이전 `valueType`, `booleanValue`, `numberValue`, `textValue`, `unit`, `allowedValues`, `SINGLE_SELECT`, 옵션 JSON 값 모델은 재도입하지 않는다.
- 위 규칙은 옵션 값 모델에 대한 것이다. PropertyRevision 등 다른 도메인의 JSON까지 제거하라는 뜻이 아니다.
- `filterable`, `registrationEnabled`, `required` 같은 메타데이터 Boolean은 그대로 유지한다.

주의: 기존 문서에는 `VARCHAR(300)`과 `VARCHAR(5)`, Validator의 `있음/없음`과 `"true"/"false"`가 섞여 있다. 정책과 실제 반영 상태를 구분해야 한다. 이번 문서 통합만으로 Validator·Entity·SQL 수정이 완료됐다고 판단하지 않는다.

### 3.2 논리 참조와 활성 중복

- 옵션 관계는 물리 FK를 추가하지 않고 논리 ID로 관리한다.
- 참조 대상의 존재·활성·삭제 여부와 유형별 허용 여부는 Repository/Service에서 검증한다.
- **옵션 활성 UNIQUE는 사용하지 않는다.** `active_unique_key` generated column과 Entity 매핑을 추가하지 않는다.
- `(property_type, option_code_id)` 및 `(property_id, option_code_id)`의 활성 중복은 애플리케이션에서 검사한다.
- 옵션 코드 자체의 `option_code` UNIQUE는 유지한다.
- 이력의 `(property_revision_id, property_option_id)` 업무 규칙 UNIQUE는 유지한다.
- 이 정책을 찜 등 다른 도메인의 UNIQUE 정책에 확대 적용하지 않는다.

`existsBy...()` 사전 조회만으로 동시 요청의 중복 저장까지 보장할 수는 없다. 일반적인 `@Transactional` 선언만 추가해도 해결되는 것은 아니다. 쓰기 구현 시 모든 관련 쓰기 경로에 적용되는 잠금 등 별도의 동시성 방식을 합의하고 테스트한다. 이를 이유로 활성 UNIQUE를 임의로 다시 추가하지 않는다.

### 3.3 삭제와 이력

- 옵션 제거는 물리 DELETE가 아니라 soft delete다.
- 삭제된 옵션을 다시 추가할 때 기존 행을 복원하지 않고 새 행을 생성하는 정책을 유지한다.
- 지원하지 않는 `RESTORE` 처리와 이력 종류를 재도입하지 않는다. 실제 Enum·SQL의 잔존 여부는 확인한다.
- `property_option_history`는 append-only, 즉 기존 이력을 수정하지 않고 새 이력을 추가한다.
- 현재값·매물 revision·옵션 이력은 같은 매물 쓰기 트랜잭션에서 함께 성공하거나 함께 rollback되어야 한다.

## 4. PR-049 조회 계약

| 항목 | 기준 |
|---|---|
| Method / Path | `GET /api/property-option-codes` |
| 인증 | PUBLIC |
| 필수 query | `propertyType` |
| 허용 유형 | `APARTMENT`, `OFFICETEL`, `VILLA`, `ROOM` |
| 정상 응답 | 200, `data.items[]` |
| 조회 결과 없음 | 200, `items=[]` |
| 필터 | 요청 유형의 삭제되지 않은 매핑 + 삭제되지 않고 활성인 옵션 코드 |
| 정렬 | 유형 매핑 `displayOrder ASC`, `optionCode ASC`, 매핑 ID ASC |

응답 item의 일곱 필드:

| 필드 | 의미 / 출처 |
|---|---|
| `optionCode` | 옵션 원장의 영문 코드 |
| `optionName` | 옵션 원장의 화면 표시명 |
| `optionCategory` | 옵션 원장의 카테고리 |
| `filterable` | 검색 필터 사용 가능 여부 |
| `registrationEnabled` | 등록·수정 입력 가능 여부 |
| `required` | 해당 유형 매핑의 필수 여부 |
| `displayOrder` | 해당 유형 매핑의 표시 순서 |

카테고리의 문서상 허용값은 `APPLIANCE`, `FURNITURE`, `SECURITY`, `STRUCTURE`, `LIVING`, `ETC`다. 실제 Enum과 SQL을 일치시킨다.

PR-049는 공용 메타데이터 조회이므로 `filterable`과 `registrationEnabled`를 모두 true로 제한하지 않는다. 검색 사용처는 `filterable`, 등록·수정 사용처는 `registrationEnabled`를 적용하고, 쓰기 Service에서도 다시 검증한다.

현재 문서상 공통 응답은 `GlobalResponseDTO<T>`의 `code/message/data/traceId`다. 옵션 작업만을 위해 전역 응답 구조를 변경하지 않는다. 과거 Excel의 다른 envelope와 제거된 응답 필드는 별도 계약 정합화 대상으로 둔다.

## 5. 현재 클래스와 완료 보고

기본 경로: `src/main/java/com/zipdaproperty/domain/option/`

| 역할 | 클래스 |
|---|---|
| HTTP 요청/응답 | `controller/PropertyOptionController.java` |
| 조회·DTO 변환 | `service/PropertyOptionQueryService.java` |
| QueryDSL 조회 | `repository/PropertyOptionQueryDSLRepository.java` |
| 응답 item | `response/PropertyOptionCodeResponseDTO.java` |
| 응답 목록 | `response/PropertyOptionCodeListResponseDTO.java` |
| 옵션 원장 | `entity/PropertyOptionCode.java` |
| 유형별 허용 규칙 | `entity/PropertyTypeOption.java` |
| 실제 매물 옵션 | `entity/PropertyOption.java` |
| 옵션 변경 이력 | `entity/PropertyOptionHistory.java` |
| 값 검증 | `validator/OptionValueValidator.java` |

목록 DTO는 `List<PropertyOptionCodeResponseDTO> items`를 담는다. 문서 기록상 record와 `List.copyOf`를 사용하며 내부 PK·감사·삭제 필드를 응답에 노출하지 않는다. 정확한 생성자 및 어노테이션은 실제 코드 기준이다.

### 완료 보고된 컨벤션 수정

- [x] Response DTO 두 개에 `DTO` 접미사 및 파일명 변경
- [x] Controller·QueryService·기존 테스트의 참조 타입 변경
- [x] QueryDSL 존재 확인 메서드명 변경, 기존 쿼리와 `deletedAt.isNull()` 유지
- [x] 구체 옵션 예외 클래스 네 개 추가
- [x] Java 운영 코드와 테스트에서 이전 DTO명·메서드명 검색 결과 0건 보고
- [x] 이번 컨벤션 수정에서 SQL·Entity·활성 UNIQUE 정책 및 쓰기 로직 미변경

존재 확인 메서드:

```java
existsByPropertyIdAndOptionCodeIdAndDeletedAtIsNull(
        Long propertyId, Long optionCodeId
)

existsByPropertyTypeAndOptionCodeIdAndDeletedAtIsNull(
        PropertyType propertyType, Long optionCodeId
)
```

이는 시그니처 요약이며 전체 Java 선언이 아니다. 최신 보고에 따르면 선언만 변경했고, 호출하는 Service는 아직 없었다. 메서드 존재를 실제 쓰기 검증 연동 완료로 오해하지 않는다.

예외 경로: `src/main/java/com/zipdaproperty/global/error/custom/business/`

| 예외 클래스 | 기존 CustomResponseCode |
|---|---|
| `OptionCodeNotFoundException` | `OPTION_CODE_NOT_FOUND` |
| `OptionNotAllowedForPropertyTypeException` | `OPTION_NOT_ALLOWED_FOR_PROPERTY_TYPE` |
| `OptionValueInvalidException` | `OPTION_VALUE_INVALID` |
| `OptionValueRequiredException` | `OPTION_VALUE_REQUIRED` |

보고상 모두 `BusinessException`을 상속하고 `(String message)` 생성자를 사용한다. 쓰기 로직에서 실제 생성자에 맞게 메시지를 전달한다. 무인자 생성자가 있다고 가정하지 않는다. 중복·동시성 오류의 구체 예외와 응답은 기존 공통 규칙을 확인해 별도로 정한다.

### 최신 검증 보고

- Java: Microsoft OpenJDK 21.0.12
- `compileJava`: 성공
- `PropertyOptionQueryServiceTest`: 성공
- `PropertyOptionQueryDSLRepositoryTest`: 성공
- `OptionRepositoryMethodTest`: 성공
- `git diff --check`: 오류 없음 보고

위 결과는 사용자가 전달한 실행 보고다. 전체 테스트, 실제 MySQL, Postman, OpenAPI 검증까지 모두 통과했다는 의미는 아니다. 과거의 “테스트 없음”, “집 JAVA_HOME 미설정”, “원격보다 7커밋 앞섬”은 현재 상태로 유지하지 않는다.

## 6. 다음 작업 전 확인 체크리스트

이미 완료된 기능을 다시 만드는 목록이 아니다. 최신 프로젝트를 읽고 완료된 항목은 근거와 함께 체크한다.

### 코드·SQL 정합성

- [ ] 옵션 SQL의 `option_value`, `default_value`, `before_value`, `after_value` 길이·nullability·CHECK를 확인한다.
- [ ] Entity 길이와 SQL 길이가 일치하는지 확인한다. 예전 300을 보고 무조건 5로 바꾸지 않고 실제 차이를 먼저 찾는다.
- [ ] Validator가 소문자 문자열 `"true"`, `"false"`만 허용하는지 확인한다.
- [ ] Enum·SQL·도메인 메서드에 `RESTORE`나 복원 동작이 남아 있는지 확인한다.
- [ ] SQL CHECK의 대소문자 판정이 목표 정책과 일치하는지 실제 DB에서 검증한다.
- [ ] 물리 FK·활성 generated key가 재도입되지 않았는지 확인한다.
- [ ] 기존 테이블에 CREATE DDL을 무작정 재실행하지 않는다. 변경이 필요하면 데이터 보존과 적용 SQL을 먼저 검토한다.

### 조회와 기준정보

- [ ] 두 테이블의 삭제 조건, 옵션 코드 활성 조건, 유형 조건을 확인한다.
- [ ] 문서 기록의 두 번 일괄 조회와 Service 정렬을 실제 코드에서 확인한다. 단일 join으로 불필요하게 재작성하지 않는다.
- [ ] 실제 쿼리 횟수와 정렬을 확인한다. Mockito/쿼리 조건 테스트만으로 MySQL 실행 검증을 대신하지 않는다.
- [ ] 집·학원 DB에 실제 옵션 원장과 유형 매핑 seed가 있는지 확인한다.
- [ ] seed의 코드·표시명·카테고리·capability·필수 여부·기본값·표시 순서를 팀 데이터 기준으로 확인한다.
- [ ] 주차·반려동물 등 Property 정형 필드와 중복되는 옵션을 임의로 seed에 추가하지 않는다.
- [ ] 과거 문서의 5분 TTL 캐시·변경 시 무효화 요구가 여전히 유효한지 확인한다. 완료로 간주하거나 임의 추가하지 않는다.

## 7. PR-049 검증 목록

- [ ] 네 유형 각각의 정상 응답, 특히 ROOM의 허용 옵션만 반환
- [ ] 인증 헤더 없이 200 응답
- [ ] 삭제된 매핑·삭제된 코드·비활성 코드 제외
- [ ] 동일 순위의 보조 정렬까지 일정함
- [ ] 빈 결과는 200과 `items=[]`
- [ ] query 누락·알 수 없는 유형은 400, 공통 오류 코드 확인
- [ ] 지원하지 않는 POST는 405, 공통 오류 코드 확인
- [ ] 일곱 응답 필드와 capability 값 확인, 내부 PK·감사 필드 미노출
- [ ] OpenAPI operation의 PUBLIC 표시와 실제 접근 일치
- [ ] 프런트·팀 API 문서가 최신 일곱 필드와 일치하는지 확인

Windows에서 기존 관련 테스트를 확인하는 명령 예시:

```powershell
.\gradlew.bat compileJava
.\gradlew.bat test --tests "*PropertyOptionQueryServiceTest" --tests "*PropertyOptionQueryDSLRepositoryTest" --tests "*OptionRepositoryMethodTest"
```

전체 테스트는 별도로 `test`를 실행해 판정한다. DB 환경 문제와 옵션 테스트 실패를 구분해 기록하고, 실행하지 못한 검증을 성공으로 표시하지 않는다. 새 테스트 작성 금지는 과거 한 작업의 제한이었으므로 영구 규칙으로 적용하지 않는다. 쓰기 구현 시에는 해당 작업 범위에 맞게 테스트한다.

## 8. 다음 구현: 옵션 등록·수정 내부 서비스

### 8.1 연동 계약부터 확인

- [ ] PR-011·012의 실제 Service와 Request DTO, `options[]` 구조를 확인한다.
- [ ] 옵션 요청 DTO명·필드명은 기존 코드와 담당자 합의에 맞춘다.
- [ ] PATCH에서 options 미전달, null, 빈 배열의 의미를 구분해 확정한다. 임의로 모두 전체 삭제로 해석하지 않는다.
- [ ] propertyType만 변경되더라도 기존 옵션의 허용 여부를 검증한다.
- [ ] 비허용 옵션 발생 시 422 거절 또는 명시적 제거 정책을 확정한다. 조용히 자동 삭제하지 않는다.
- [ ] propertyId, propertyType, 옵션 요청, actorContext, propertyRevisionId의 전달 시점과 필요 여부를 확정한다.
- [ ] 최초 등록·무변경 요청의 이력 작성 여부 및 이력 change_type을 실제 revision 정책과 맞춘다.

### 8.2 등록 처리

- [ ] 요청 내부의 중복 optionCode를 검사한다.
- [ ] 코드 존재·활성·삭제 여부와 유형별 허용 규칙을 일괄 조회한다.
- [ ] 등록 가능 여부와 필수 옵션 누락을 검사한다.
- [ ] 문자열 옵션값을 검증한다. `"false"`를 미입력으로 취급하지 않는다.
- [ ] 구체 옵션 예외를 사용하고 field error 지원 방식·HTTP 상태를 공통 예외 처리와 맞춘다.
- [ ] 활성 중복 검사와 합의한 동시성 제어를 적용한다.
- [ ] 검증 후 `property_option`을 저장하고 확정된 정책에 따라 이력을 기록한다.

### 8.3 수정·제거·이력 처리

- [ ] 기존 활성 옵션을 일괄 조회하고 요청과 비교한다.
- [ ] 추가 항목은 INSERT, 변경 항목은 확정된 현재값 변경 방식으로 처리한다.
- [ ] 제거 항목은 BaseAuditEntity의 실제 감사·soft delete 방식으로 처리한다.
- [ ] 삭제된 항목의 재추가는 새 행으로 저장한다.
- [ ] 변경 전후 값과 필요한 메타데이터를 history에 기록한다.
- [ ] `(property_revision_id, property_option_id)` 이력 UNIQUE와 기록 횟수가 충돌하지 않도록 한다.
- [ ] 상위 매물 트랜잭션에서 매물·옵션·revision·history가 함께 commit/rollback되게 한다.
- [ ] 항목별 반복 조회로 N+1을 만들지 않는다.

등록·수정·soft delete·history는 완전히 분리된 순차 프로젝트가 아니다. 등록 흐름 안에 저장·이력이, 수정 흐름 안에 추가·변경·제거·이력이 함께 들어가며 중복·동시성 방어는 양쪽에 적용한다.

### 8.4 쓰기 테스트

- [ ] 정상 등록과 `"false"` 저장
- [ ] 중복 코드, 없는 코드, 비활성/삭제 코드, 다른 유형 코드 거절
- [ ] 필수 누락, 잘못된 값, 등록 비활성 옵션 거절
- [ ] 추가·값 변경·soft delete·재추가와 이력 확인
- [ ] options 미전달/null/빈 배열의 합의된 수정 동작
- [ ] propertyType 변경 후 비허용 옵션 잔존 방지
- [ ] 실패 시 매물·옵션·revision·history 전체 rollback
- [ ] 동시에 같은 옵션을 추가해도 활성 중복이 남지 않음
- [ ] 기존 활성 중복 데이터가 있다면 정상 행으로 조용히 간주하지 않고 처리 방침 확인

## 9. PR-009 상세 연동 — 별도 후속 범위

- [ ] 상세 옵션 응답의 필드와 배열/그룹 형식을 담당자와 확정한다.
- [ ] `is_detail_visible`, 코드 활성·삭제 여부, 현재값 삭제 여부를 반영한다.
- [ ] 공개할 수 없는 매물에는 옵션도 노출하지 않는다.
- [ ] 내부 ID나 다른 비공개 정보가 섞이지 않게 한다.
- [ ] 상세 item마다 추가 조회하지 않는다.

## 10. 집·학원 공통 작업 방식

장소별로 문서를 따로 관리하지 않는다. 두 컴퓨터에서 같은 저장소와 작업 브랜치를 확인하고, 이 파일을 동일하게 사용한다. 이번 문서 생성은 사용자 PC의 Git 저장소에 자동 반영되거나 push된 작업이 아니다.

작업 시작 시 읽기 전용 확인:

```powershell
git status --short --branch
git branch --show-current
git log -3 --oneline
java -version
```

- 예상 브랜치는 대화 기준 `feature/option_JSL`이지만 실제 상태를 먼저 확인한다.
- 기존 변경사항이 있으면 무조건 pull·브랜치 전환·덮어쓰기를 하지 않는다.
- 원격과 동기화한 뒤 작업하고, 장소를 옮기기 전 필요한 변경만 골라 commit/push한다. 문서만 옮기려다 관련 없는 파일까지 포함하도록 `git add .`를 무조건 사용하지 않는다.
- DB 데이터·테이블·접속 설정은 Git pull만으로 동기화되지 않는다. 집·학원의 접속 대상과 적용 SQL·seed를 별도로 확인한다.
- DB 비밀번호, 토큰, 개인정보는 이 문서나 Git에 기록하지 않는다.
- 기존 사용자 변경·미추적 파일·다른 팀원 코드·원본 Excel/ERD/SQL을 임의로 삭제하거나 되돌리지 않는다.
- 전역 응답·예외 처리·보안 설정은 옵션만을 위해 임의로 변경하지 않는다.

## 11. 작업 기록

| 기준 | 작업 | 검증 수준 |
|---|---|---|
| 2026-09-06 전달 보고 | DTO 접미사·참조 변경, existsBy 네이밍, 구체 예외 4개 | 컴파일·관련 테스트 3종 성공 보고 |
| 2026-09-06 문서 정리 | 옵션 정책·진행 상태 정리, 완료/확인 필요 구분 | 문서 검토만 수행. 코드·DB 미변경 |

다음 작업 종료 시 아래 항목을 갱신한다.

- 작업일 / 장소:
- 브랜치 / 마지막 커밋:
- 이번 변경 파일과 내용:
- 직접 실행한 검증과 결과:
- 미실행 검증 / 남은 문제:
- 적용한 SQL·seed 식별 정보(비밀값 제외):
- 다음 작업:

## 12. 현재 적용하지 않는 구 정책

- 물리 FK와 활성 UNIQUE를 다시 추가하라는 이전 지시를 현재 작업에서 제외했다.
- typed value·SINGLE_SELECT·옵션 JSON·RESTORE 구현 지시를 제거했다.
- DTO명과 QueryDSL 메서드명을 최신 완료 보고에 맞췄다.
- 오래된 테스트 부재·JAVA_HOME 오류·원격 커밋 차이를 현재 사실로 유지하지 않았다.
- Repository 메서드 준비와 쓰기 Service 연결 완료를 구분했다.
- Validator·DDL의 서로 다른 기록은 추측으로 완료 처리하지 않고 확인 항목으로 남겼다.
- 옵션 외 찜·파일·신고 등 전체 담당 API 목록은 이 옵션 가이드의 범위에서 제외했다. 담당 변경을 의미하지 않는다.
