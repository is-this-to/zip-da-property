# ERD와 데이터베이스

> 실제 로컬 MySQL `zipda_property` 스키마를 기준으로 현재 데이터 구조를 정리한다. ERD와 테이블 명세는 데이터 행이 아닌 실제 스키마 메타데이터를 기준으로 한다.

## 1. ERD

![ZIPDA Property ERD](images/erd-property.png)

원본 ERD와 실제 DB 구조가 다를 경우 실행 중인 `zipda_property` 스키마와 현재 소스를 우선한다.

## 2. 테이블 목록

현재 `zipda_property`에는 29개 테이블과 619개 컬럼이 존재한다. 테이블명을 선택하면 3절의 컬럼 명세로 이동한다.

| 영역 | 테이블 | 역할 |
| --- | --- | --- |
| 지역·단지 | [`region`](#3-1-region) | 법정동 지역 계층과 표시 정보 |
| 지역·단지 | [`region_boundary`](#3-2-region_boundary) | 지역 경계와 공간 정보 |
| 지역·단지 | [`region_import_batch`](#3-3-region_import_batch) | 지역 데이터 적재 실행 이력 |
| 지역·단지 | [`apartment_complex`](#3-4-apartment_complex) | 공동주택 단지 기본 정보 |
| 지역·단지 | [`apartment_complex_external_ref`](#3-5-apartment_complex_external_ref) | 단지의 외부 데이터 원천 식별자 연결 |
| 지역·단지 | [`apartment_complex_facility`](#3-6-apartment_complex_facility) | 단지 편의시설 정보 |
| 매물 핵심·이력 | [`property`](#3-7-property) | 매물 기본 정보와 현재 상태 |
| 매물 핵심·이력 | [`property_address`](#3-8-property_address) | 매물 주소와 정확·공개 위치 |
| 매물 핵심·이력 | [`property_option`](#3-9-property_option) | 매물 선택 옵션 값 |
| 매물 핵심·이력 | [`property_option_code`](#3-10-property_option_code) | 매물 유형별 옵션 코드 |
| 매물 핵심·이력 | [`property_type_option`](#3-11-property_type_option) | 매물 유형과 옵션 코드의 허용 관계 |
| 매물 핵심·이력 | [`property_option_history`](#3-12-property_option_history) | 매물 옵션 변경 이력 |
| 매물 핵심·이력 | [`property_revision`](#3-13-property_revision) | 매물 변경 Revision |
| 매물 핵심·이력 | [`property_status_history`](#3-14-property_status_history) | 거래·공개·인증 상태 변경 이력 |
| 매물 핵심·이력 | [`property_publisher_snapshot`](#3-15-property_publisher_snapshot) | 등록자 정보 스냅샷 |
| 매물 핵심·이력 | [`property_audit_event`](#3-16-property_audit_event) | 매물 관련 감사 이벤트 |
| 파일·이미지 | [`property_file`](#3-17-property_file) | 업로드 파일 메타데이터와 검증 상태 |
| 파일·이미지 | [`property_image`](#3-18-property_image) | 매물 이미지 연결과 표시 순서 |
| 인증·위험 평가 | [`property_verification`](#3-19-property_verification) | 매물 인증 심사 상태 |
| 인증·위험 평가 | [`property_verification_evidence`](#3-20-property_verification_evidence) | 인증 증빙 파일 연결 |
| 인증·위험 평가 | [`property_risk_assessment`](#3-21-property_risk_assessment) | 등록 전 위험 평가 결과 |
| 찜·신고 | [`property_favorite`](#3-22-property_favorite) | 회원별 관심 매물 상태 |
| 찜·신고 | [`property_report`](#3-23-property_report) | 매물 신고와 처리 상태 |
| 찜·신고 | [`property_report_action`](#3-24-property_report_action) | 관리자 신고 조치 이력 |
| 찜·신고 | [`property_report_appeal`](#3-25-property_report_appeal) | 신고 대상 매물 작성자의 이의신청 |
| 찜·신고 | [`property_report_evidence`](#3-26-property_report_evidence) | 신고 증빙 파일 연결 |
| 연동·운영 | [`property_idempotency`](#3-27-property_idempotency) | 매물 등록 중복 방지 요청 기록 |
| 연동·운영 | [`property_member_event_consumption`](#3-28-property_member_event_consumption) | 처리한 Member 이벤트 이력 |
| 연동·운영 | [`property_member_state`](#3-29-property_member_state) | Member 상태의 로컬 투영 |

## 3. 모든 테이블 명세서

실제 `zipda_property`의 컬럼명, 데이터 타입, NULL 허용, 기본값, 인덱스 표기와 컬럼 설명을 옮겼다.

- `PK`: 기본 키
- `UNIQUE`: 고유 인덱스
- `INDEX`: 일반 인덱스 또는 외래 식별자 조회용 인덱스
- `—`: 기본값 또는 키 표기 없음

### 3-1. region

법정동 지역 계층과 표시 정보이다.

| 컬럼 | 데이터 타입 | NULL 허용 | 기본값 | 키 | 설명 |
| --- | --- | --- | --- | --- | --- |
| `region_id` | `bigint` | NOT NULL | — | PK | 내부 지역 ID. API의 regionId |
| `parent_region_id` | `bigint` | NULL | — | INDEX | 상위 지역 ID. 논리 참조: region.region_id. 시도는 NULL, 시군구는 시도, 읍면동은 시군구, 리는 읍면동을 참조 |
| `region_code` | `char(10)` | NOT NULL | — | UNIQUE | 법정동 10자리 코드. 외부 공공데이터 매칭 기준 |
| `region_name` | `varchar(100)` | NOT NULL | — | INDEX | 현재 단계 지역명. 예: 서울특별시, 강남구, 역삼동, 대흥리 |
| `full_region_name` | `varchar(300)` | NOT NULL | — | — | 전체 지역 경로. 예: 경기도 이천시 대월면 대흥리 |
| `region_level` | `tinyint` | NOT NULL | — | INDEX | 지역 단계. 1=시도, 2=시군구, 3=읍면동, 4=리 |
| `region_type` | `varchar(30)` | NOT NULL | — | — | 지역 유형. SIDO, SIGUNGU, EUPMYEONDONG, RI |
| `center_location` | `point` | NULL | — | — | 대표 중심 좌표. POINT(경도 위도) 순서, 좌표계 EPSG:4326 |
| `has_children` | `tinyint(1)` | NOT NULL | — | — | 활성 직계 하위 지역 존재 여부. 0=없음, 1=있음 |
| `is_active` | `tinyint(1)` | NOT NULL | — | — | 현재 사용 가능한 법정동 여부. 0=폐지 또는 비활성, 1=활성 |
| `effective_from` | `date` | NULL | — | — | 행정구역 효력 시작일 |
| `effective_to` | `date` | NULL | — | — | 행정구역 효력 종료일. 현재 유효하면 NULL |
| `replacement_region_id` | `bigint` | NULL | — | INDEX | 폐지 지역을 대체하는 지역 ID. 논리 참조: region.region_id |
| `created_at` | `datetime(6)` | NOT NULL | — | — | 최초 생성 UTC 시각 |
| `created_by_member_id` | `bigint` | NULL | — | — | 생성한 Member 외부 ID. 자동 배치이면 NULL 가능 |
| `created_by_role` | `varchar(30)` | NULL | — | — | 생성 당시 사용자 역할 |
| `updated_at` | `datetime(6)` | NOT NULL | — | — | 마지막 수정 UTC 시각 |
| `updated_by_member_id` | `bigint` | NULL | — | — | 마지막 수정 Member 외부 ID. 자동 배치이면 NULL 가능 |
| `updated_by_role` | `varchar(30)` | NULL | — | — | 마지막 수정 당시 사용자 역할 |
| `deleted_at` | `datetime(6)` | NULL | — | — | 소프트 삭제 UTC 시각. NULL이면 삭제되지 않은 데이터 |
| `deleted_by_member_id` | `bigint` | NULL | — | — | 삭제한 Member 외부 ID |
| `deleted_by_role` | `varchar(30)` | NULL | — | — | 삭제 당시 사용자 역할 |
| `delete_reason` | `varchar(500)` | NULL | — | — | 소프트 삭제 사유 |
| `action_source` | `varchar(20)` | NOT NULL | — | — | 작업 주체. MEMBER, SYSTEM, BATCH |

### 3-2. region_boundary

지역 경계와 공간 정보이다.

| 컬럼 | 데이터 타입 | NULL 허용 | 기본값 | 키 | 설명 |
| --- | --- | --- | --- | --- | --- |
| `region_boundary_id` | `bigint` | NOT NULL | — | PK | 지역 경계 데이터 내부 ID |
| `region_id` | `bigint` | NOT NULL | — | INDEX | 경계가 속한 지역 ID. 논리 참조: region.region_id |
| `geometry_type` | `varchar(20)` | NOT NULL | — | — | 원본 경계 유형. POLYGON 또는 MULTIPOLYGON. DB 저장 시 MULTIPOLYGON으로 정규화 |
| `boundary_geometry` | `multipolygon` | NOT NULL | — | INDEX | 실제 법정구역 경계. EPSG:4326 MULTIPOLYGON으로 저장 |
| `south_west_latitude` | `decimal(10,7)` | NOT NULL | — | — | 경계 bounds 남서쪽 위도 |
| `south_west_longitude` | `decimal(10,7)` | NOT NULL | — | — | 경계 bounds 남서쪽 경도 |
| `north_east_latitude` | `decimal(10,7)` | NOT NULL | — | — | 경계 bounds 북동쪽 위도 |
| `north_east_longitude` | `decimal(10,7)` | NOT NULL | — | — | 경계 bounds 북동쪽 경도 |
| `source_name` | `varchar(100)` | NOT NULL | — | — | 경계 원본 제공기관 또는 원본 파일명 |
| `source_version` | `varchar(50)` | NOT NULL | — | INDEX | 경계 원본 기준 버전 또는 기준일 |
| `checksum` | `char(64)` | NOT NULL | — | — | 원본 또는 정규화 geometry의 SHA-256 값 |
| `simplification_level` | `tinyint` | NOT NULL | — | — | 지도 성능을 위한 경계 단순화 단계. 0은 원본 |
| `created_at` | `datetime(6)` | NOT NULL | — | — | 최초 생성 UTC 시각 |
| `created_by_member_id` | `bigint` | NULL | — | — | 생성한 Member 외부 ID. 자동 배치이면 NULL 가능 |
| `created_by_role` | `varchar(30)` | NULL | — | — | 생성 당시 사용자 역할 |
| `updated_at` | `datetime(6)` | NOT NULL | — | — | 마지막 수정 UTC 시각 |
| `updated_by_member_id` | `bigint` | NULL | — | — | 마지막 수정 Member 외부 ID. 자동 배치이면 NULL 가능 |
| `updated_by_role` | `varchar(30)` | NULL | — | — | 마지막 수정 당시 사용자 역할 |
| `deleted_at` | `datetime(6)` | NULL | — | — | 소프트 삭제 UTC 시각. NULL이면 활성 경계 |
| `active_region_boundary_key` | `bigint` | NULL | — | INDEX | 활성 경계이면 region_id. simplification_level과 함께 지역·단순화 단계별 활성 경계 1개를 보장 |
| `deleted_by_member_id` | `bigint` | NULL | — | — | 삭제한 Member 외부 ID |
| `deleted_by_role` | `varchar(30)` | NULL | — | — | 삭제 당시 사용자 역할 |
| `delete_reason` | `varchar(500)` | NULL | — | — | 소프트 삭제 사유 |
| `action_source` | `varchar(20)` | NOT NULL | — | — | 작업 주체. MEMBER, SYSTEM, BATCH |

### 3-3. region_import_batch

지역 데이터 적재 실행 이력이다.

| 컬럼 | 데이터 타입 | NULL 허용 | 기본값 | 키 | 설명 |
| --- | --- | --- | --- | --- | --- |
| `region_import_batch_id` | `bigint` | NOT NULL | — | PK | 지역 데이터 적재 배치 내부 ID |
| `batch_version` | `varchar(100)` | NOT NULL | — | UNIQUE | 배치를 구분하는 고유 버전. 동일 배치 중복 실행 방지 기준 |
| `source_name` | `varchar(100)` | NOT NULL | — | — | 원본 제공기관 및 원본 파일명 |
| `source_date` | `date` | NOT NULL | — | INDEX | 원본 데이터 기준일 |
| `source_checksum` | `char(64)` | NOT NULL | — | — | 원본 파일의 SHA-256 체크섬 |
| `import_status` | `varchar(30)` | NOT NULL | — | INDEX | 적재 상태. CREATED, VALIDATING, VALIDATED, APPLIED, FAILED |
| `archive_status` | `varchar(30)` | NOT NULL | — | — | 보관 상태. ACTIVE, ARCHIVED |
| `total_count` | `int` | NOT NULL | — | — | 배치 전체 데이터 건수 |
| `valid_count` | `int` | NOT NULL | — | — | 검증 성공 데이터 건수 |
| `invalid_count` | `int` | NOT NULL | — | — | 검증 실패 또는 제외 데이터 건수 |
| `error_summary_json` | `json` | NULL | — | — | 오류 코드별 건수 등의 오류 요약. 원본 데이터 전체 저장 금지 |
| `started_at` | `datetime(6)` | NULL | — | — | 배치 시작 UTC 시각 |
| `completed_at` | `datetime(6)` | NULL | — | — | 배치 완료 UTC 시각 |
| `created_at` | `datetime(6)` | NOT NULL | — | — | 배치 이력 최초 생성 UTC 시각 |
| `created_by_member_id` | `bigint` | NULL | — | — | 배치를 생성한 Member 외부 ID. 자동 배치는 NULL 가능 |
| `created_by_role` | `varchar(30)` | NULL | — | — | 배치 생성 당시 사용자 역할 |
| `updated_at` | `datetime(6)` | NOT NULL | — | — | 배치 이력 마지막 수정 UTC 시각 |
| `updated_by_member_id` | `bigint` | NULL | — | — | 마지막 수정 Member 외부 ID. 자동 배치는 NULL 가능 |
| `updated_by_role` | `varchar(30)` | NULL | — | — | 마지막 수정 당시 사용자 역할 |
| `deleted_at` | `datetime(6)` | NULL | — | — | 소프트 삭제 UTC 시각. NULL이면 삭제되지 않은 배치 |
| `deleted_by_member_id` | `bigint` | NULL | — | — | 삭제한 Member 외부 ID |
| `deleted_by_role` | `varchar(30)` | NULL | — | — | 삭제 당시 사용자 역할 |
| `delete_reason` | `varchar(500)` | NULL | — | — | 소프트 삭제 사유 |
| `action_source` | `varchar(20)` | NOT NULL | — | — | 작업 주체. MEMBER, SYSTEM, BATCH |

### 3-4. apartment_complex

공동주택 단지 기본 정보이다.

| 컬럼 | 데이터 타입 | NULL 허용 | 기본값 | 키 | 설명 |
| --- | --- | --- | --- | --- | --- |
| `apartment_complex_id` | `bigint` | NOT NULL | — | PK | ZIPDA 내부 공동주택 단지 ID |
| `region_id` | `bigint` | NOT NULL | — | INDEX | region.region_id 논리 참조. 단지가 위치한 법정동 |
| `complex_name` | `varchar(200)` | NOT NULL | — | — | 단지명 검색 및 화면 표시명 |
| `road_address` | `varchar(300)` | NULL | — | — | 정규화된 단지 도로명 주소 |
| `jibun_address` | `varchar(300)` | NULL | — | — | 정규화된 단지 지번 주소 |
| `center_location` | `point` | NOT NULL | — | INDEX | 단지 대표 위치. POINT(경도 위도) |
| `total_buildings` | `int` | NULL | — | — | 총 동 수 |
| `total_households` | `int` | NULL | — | — | 총 세대 수 |
| `total_parking_spaces` | `int` | NULL | — | — | 단지 전체 주차대수 |
| `approval_date` | `date` | NULL | — | — | 사용승인일 |
| `is_active` | `tinyint(1)` | NOT NULL | — | — | 공공데이터 기준 현재 사용 가능한 단지 여부 |
| `source_name` | `varchar(100)` | NOT NULL | — | INDEX | 주 기준 원천명. 예: MOLIT_KAPT |
| `source_updated_at` | `datetime(6)` | NULL | — | — | 원천 데이터가 제공한 최종 갱신 시각 |
| `created_at` | `datetime(6)` | NOT NULL | — | — | 행이 처음 생성된 UTC 시각 |
| `created_by_member_id` | `bigint` | NULL | — | — | 생성한 Member 외부 ID |
| `created_by_role` | `varchar(30)` | NULL | — | — | 생성 당시 역할 스냅샷 |
| `updated_at` | `datetime(6)` | NOT NULL | — | — | 마지막 수정 UTC 시각 |
| `updated_by_member_id` | `bigint` | NULL | — | — | 마지막 수정 Member 외부 ID |
| `updated_by_role` | `varchar(30)` | NULL | — | — | 마지막 수정 당시 역할 |
| `deleted_at` | `datetime(6)` | NULL | — | — | 소프트 삭제 시각. NULL이면 삭제되지 않음 |
| `deleted_by_member_id` | `bigint` | NULL | — | — | 삭제한 Member 외부 ID |
| `deleted_by_role` | `varchar(30)` | NULL | — | — | 삭제 당시 역할 |
| `delete_reason` | `varchar(500)` | NULL | — | — | 소프트 삭제 사유 |
| `action_source` | `varchar(20)` | NOT NULL | — | — | 작업 주체: MEMBER, SYSTEM, BATCH |

### 3-5. apartment_complex_external_ref

단지의 외부 데이터 원천 식별자 연결이다.

| 컬럼 | 데이터 타입 | NULL 허용 | 기본값 | 키 | 설명 |
| --- | --- | --- | --- | --- | --- |
| `complex_external_ref_id` | `bigint` | NOT NULL | — | PK | 외부 단지 식별자 연결 ID |
| `apartment_complex_id` | `bigint` | NOT NULL | — | INDEX | apartment_complex.apartment_complex_id 논리 참조 |
| `provider_code` | `varchar(50)` | NOT NULL | — | INDEX | 외부 제공기관 코드. 예: MOLIT_KAPT |
| `external_complex_id` | `varchar(100)` | NOT NULL | — | — | 제공기관이 발급한 단지 고유 ID |
| `is_primary` | `tinyint(1)` | NOT NULL | — | — | 해당 단지의 주 기준 원천 여부 |
| `source_payload_hash` | `char(64)` | NULL | — | — | 정규화한 마지막 원본 payload의 SHA-256 |
| `source_updated_at` | `datetime(6)` | NULL | — | — | 원천 데이터가 제공한 최종 갱신 시각 |
| `last_seen_at` | `datetime(6)` | NOT NULL | — | — | 적재 배치가 원천에서 마지막으로 확인한 UTC 시각 |
| `active_provider_external_key` | `varchar(200)` | NULL | — | UNIQUE | 활성 제공기관+외부 단지 ID 중복 방지 키 |
| `active_complex_provider_key` | `varchar(150)` | NULL | — | UNIQUE | 활성 내부 단지+제공기관 중복 방지 키 |
| `active_primary_key` | `bigint` | NULL | — | UNIQUE | 단지별 활성 주 기준 원천 1개 보장 키 |
| `created_at` | `datetime(6)` | NOT NULL | — | — | 행이 처음 생성된 UTC 시각 |
| `created_by_member_id` | `bigint` | NULL | — | — | 생성한 Member 외부 ID |
| `created_by_role` | `varchar(30)` | NULL | — | — | 생성 당시 역할 스냅샷 |
| `updated_at` | `datetime(6)` | NOT NULL | — | — | 마지막 수정 UTC 시각 |
| `updated_by_member_id` | `bigint` | NULL | — | — | 마지막 수정 Member 외부 ID |
| `updated_by_role` | `varchar(30)` | NULL | — | — | 마지막 수정 당시 역할 |
| `deleted_at` | `datetime(6)` | NULL | — | — | 소프트 삭제 시각. NULL이면 활성 연결 |
| `deleted_by_member_id` | `bigint` | NULL | — | — | 삭제한 Member 외부 ID |
| `deleted_by_role` | `varchar(30)` | NULL | — | — | 삭제 당시 역할 |
| `delete_reason` | `varchar(500)` | NULL | — | — | 소프트 삭제 사유 |
| `action_source` | `varchar(20)` | NOT NULL | — | — | 작업 주체: MEMBER, SYSTEM, BATCH |

### 3-6. apartment_complex_facility

단지 편의시설 정보이다.

| 컬럼 | 데이터 타입 | NULL 허용 | 기본값 | 키 | 설명 |
| --- | --- | --- | --- | --- | --- |
| `complex_facility_id` | `bigint` | NOT NULL | — | PK | 단지 시설 연결 ID |
| `apartment_complex_id` | `bigint` | NOT NULL | — | INDEX | apartment_complex.apartment_complex_id 논리 참조 |
| `facility_code` | `varchar(50)` | NOT NULL | — | — | 정규화 시설 코드. 예: GYM, PLAYGROUND, SECURITY |
| `facility_name` | `varchar(100)` | NOT NULL | — | — | 화면 표시 시설명 |
| `description` | `varchar(1000)` | NULL | — | — | 원천 시설정보를 정규화한 보충 설명 |
| `is_verified` | `tinyint(1)` | NOT NULL | — | — | 출처 또는 운영자 검증 완료 여부 |
| `source_name` | `varchar(100)` | NULL | — | — | 시설정보 제공 원천명 |
| `source_updated_at` | `datetime(6)` | NULL | — | — | 시설 원천 데이터 최종 갱신 시각 |
| `active_complex_facility_key` | `varchar(150)` | NULL | — | UNIQUE | 활성 단지+시설 코드 중복 방지 키 |
| `created_at` | `datetime(6)` | NOT NULL | — | — | 행이 처음 생성된 UTC 시각 |
| `created_by_member_id` | `bigint` | NULL | — | — | 생성한 Member 외부 ID |
| `created_by_role` | `varchar(30)` | NULL | — | — | 생성 당시 역할 스냅샷 |
| `updated_at` | `datetime(6)` | NOT NULL | — | — | 마지막 수정 UTC 시각 |
| `updated_by_member_id` | `bigint` | NULL | — | — | 마지막 수정 Member 외부 ID |
| `updated_by_role` | `varchar(30)` | NULL | — | — | 마지막 수정 당시 역할 |
| `deleted_at` | `datetime(6)` | NULL | — | — | 소프트 삭제 시각. NULL이면 활성 시설 |
| `deleted_by_member_id` | `bigint` | NULL | — | — | 삭제한 Member 외부 ID |
| `deleted_by_role` | `varchar(30)` | NULL | — | — | 삭제 당시 역할 |
| `delete_reason` | `varchar(500)` | NULL | — | — | 소프트 삭제 사유 |
| `action_source` | `varchar(20)` | NOT NULL | — | — | 작업 주체: MEMBER, SYSTEM, BATCH |

### 3-7. property

매물 기본 정보와 현재 상태이다.

| 컬럼 | 데이터 타입 | NULL 허용 | 기본값 | 키 | 설명 |
| --- | --- | --- | --- | --- | --- |
| `property_id` | `bigint` | NOT NULL | — | PK | — |
| `version` | `bigint` | NOT NULL | — | — | — |
| `region_id` | `bigint` | NOT NULL | — | INDEX | — |
| `apartment_complex_id` | `bigint` | NULL | — | — | — |
| `author_member_id` | `bigint` | NOT NULL | — | — | — |
| `publisher_type` | `varchar(30)` | NOT NULL | — | — | — |
| `property_type` | `varchar(30)` | NOT NULL | — | INDEX | — |
| `transaction_type` | `varchar(30)` | NOT NULL | — | — | — |
| `sale_price` | `bigint` | NULL | — | — | — |
| `deposit` | `bigint` | NULL | — | — | — |
| `monthly_rent` | `bigint` | NULL | — | — | — |
| `maintenance_fee` | `bigint` | NULL | — | — | — |
| `supply_area` | `decimal(10,2)` | NULL | — | — | — |
| `exclusive_area` | `decimal(10,2)` | NOT NULL | — | — | — |
| `room_count` | `int` | NULL | — | — | — |
| `bathroom_count` | `int` | NULL | — | — | — |
| `floor` | `int` | NULL | — | — | — |
| `total_floor` | `int` | NULL | — | — | — |
| `floor_condition` | `varchar(30)` | NULL | — | — | — |
| `direction` | `varchar(20)` | NULL | — | — | — |
| `approval_date` | `date` | NULL | — | — | — |
| `building_use` | `varchar(100)` | NULL | — | — | — |
| `is_parking_available` | `tinyint(1)` | NULL | — | — | — |
| `has_elevator` | `tinyint(1)` | NULL | — | — | — |
| `is_pet_allowed` | `tinyint(1)` | NULL | — | — | — |
| `title` | `varchar(200)` | NOT NULL | — | — | — |
| `description` | `text` | NOT NULL | — | — | — |
| `publication_status` | `varchar(30)` | NOT NULL | — | INDEX | — |
| `transaction_status` | `varchar(30)` | NOT NULL | — | — | — |
| `verification_status` | `varchar(30)` | NOT NULL | — | — | — |
| `risk_score` | `decimal(5,2)` | NULL | — | — | — |
| `published_at` | `datetime(6)` | NULL | — | — | — |
| `created_at` | `datetime(6)` | NOT NULL | — | — | — |
| `created_by_member_id` | `bigint` | NULL | — | — | — |
| `created_by_role` | `varchar(30)` | NULL | — | — | — |
| `updated_at` | `datetime(6)` | NOT NULL | — | — | — |
| `updated_by_member_id` | `bigint` | NULL | — | — | — |
| `updated_by_role` | `varchar(30)` | NULL | — | — | — |
| `deleted_at` | `datetime(6)` | NULL | — | — | — |
| `deleted_by_member_id` | `bigint` | NULL | — | — | — |
| `deleted_by_role` | `varchar(30)` | NULL | — | — | — |
| `delete_reason` | `varchar(500)` | NULL | — | — | — |
| `action_source` | `varchar(20)` | NOT NULL | — | — | — |

### 3-8. property_address

매물 주소와 정확·공개 위치이다.

| 컬럼 | 데이터 타입 | NULL 허용 | 기본값 | 키 | 설명 |
| --- | --- | --- | --- | --- | --- |
| `property_address_id` | `bigint` | NOT NULL | — | PK | 주소 ID. MySQL AUTO_INCREMENT 생성 |
| `property_id` | `bigint` | NOT NULL | — | UNIQUE | 매물 ID. 논리 참조: property.property_id, 매물과 1:1 |
| `legal_dong_code` | `char(10)` | NOT NULL | — | INDEX | Kakao 응답의 법정동 코드. 내부 Region 검증에 사용 |
| `exact_road_address` | `varchar(300)` | NULL | — | — | 정확 도로명 주소. 권한 API에서만 사용 |
| `exact_jibun_address` | `varchar(300)` | NULL | — | — | 정확 지번 주소. 권한 API에서만 사용 |
| `detail_address_encrypted` | `varbinary(1000)` | NULL | — | — | 동·호 등 상세주소 암호문. 평문 로그 금지 |
| `detail_address_key_version` | `varchar(50)` | NULL | — | — | 상세주소 암호화에 사용한 키 버전. 암호문이 NULL이면 NULL |
| `exact_location` | `point` | NOT NULL | — | INDEX | 정확 좌표. POINT(경도 위도) 순서. 공개 DTO 노출 금지 |
| `public_address` | `varchar(300)` | NOT NULL | — | — | 공개 가능한 동 수준 주소 |
| `public_location` | `point` | NOT NULL | — | INDEX | 지도 검색과 마커 표시에 사용하는 비식별 공개 좌표 |
| `disclosure_level` | `varchar(30)` | NOT NULL | — | — | 위치 공개 수준. DONG, APPROXIMATE, COMPLEX_CENTER |
| `location_verified_at` | `datetime(6)` | NOT NULL | — | — | 주소·좌표·법정동·Region 검증 완료 UTC 시각 |
| `location_source` | `varchar(30)` | NOT NULL | — | — | 위치 정보 출처. KAKAO_LOCAL, ADMIN, BATCH |
| `created_at` | `datetime(6)` | NOT NULL | — | — | 최초 생성 UTC 시각 |
| `created_by_member_id` | `bigint` | NULL | — | — | 생성한 Member 외부 ID |
| `created_by_role` | `varchar(30)` | NULL | — | — | 생성 당시 사용자 역할 |
| `updated_at` | `datetime(6)` | NOT NULL | — | — | 마지막 수정 UTC 시각 |
| `updated_by_member_id` | `bigint` | NULL | — | — | 마지막 수정 Member 외부 ID |
| `updated_by_role` | `varchar(30)` | NULL | — | — | 마지막 수정 당시 사용자 역할 |
| `deleted_at` | `datetime(6)` | NULL | — | — | 소프트 삭제 UTC 시각. NULL이면 활성 |
| `deleted_by_member_id` | `bigint` | NULL | — | — | 삭제한 Member 외부 ID |
| `deleted_by_role` | `varchar(30)` | NULL | — | — | 삭제 당시 사용자 역할 |
| `delete_reason` | `varchar(500)` | NULL | — | — | 소프트 삭제 사유 |
| `action_source` | `varchar(20)` | NOT NULL | — | — | 작업 주체. MEMBER, SYSTEM, BATCH |

### 3-9. property_option

매물 선택 옵션 값이다.

| 컬럼 | 데이터 타입 | NULL 허용 | 기본값 | 키 | 설명 |
| --- | --- | --- | --- | --- | --- |
| `property_option_id` | `bigint` | NOT NULL | — | PK | 옵션 연결 ID. MySQL AUTO_INCREMENT 생성 |
| `property_id` | `bigint` | NOT NULL | — | INDEX | 대상 매물 |
| `option_code_id` | `bigint` | NOT NULL | — | — | property_option_code 기준 ID |
| `option_value` | `varchar(5)` | NOT NULL | — | — | 옵션 값: true 또는 false |
| `display_order` | `int` | NOT NULL | — | — | 상세 화면 표시 순서 |
| `verified` | `tinyint(1)` | NOT NULL | — | — | 증빙·관리자 확인 여부 |
| `created_at` | `datetime(6)` | NOT NULL | — | — | — |
| `created_by_member_id` | `bigint` | NULL | — | — | — |
| `created_by_role` | `varchar(30)` | NULL | — | — | — |
| `updated_at` | `datetime(6)` | NOT NULL | — | — | — |
| `updated_by_member_id` | `bigint` | NULL | — | — | — |
| `updated_by_role` | `varchar(30)` | NULL | — | — | — |
| `deleted_at` | `datetime(6)` | NULL | — | — | — |
| `deleted_by_member_id` | `bigint` | NULL | — | — | — |
| `deleted_by_role` | `varchar(30)` | NULL | — | — | — |
| `delete_reason` | `varchar(500)` | NULL | — | — | — |
| `action_source` | `varchar(20)` | NOT NULL | — | — | — |

### 3-10. property_option_code

매물 유형별 옵션 코드이다.

| 컬럼 | 데이터 타입 | NULL 허용 | 기본값 | 키 | 설명 |
| --- | --- | --- | --- | --- | --- |
| `option_code_id` | `bigint` | NOT NULL | — | PK | 내부 옵션 기준 ID. MySQL AUTO_INCREMENT 생성 |
| `option_code` | `varchar(50)` | NOT NULL | — | UNIQUE | API·DB에서 사용하는 영문 코드 |
| `option_name` | `varchar(100)` | NOT NULL | — | — | 사용자 화면 한글 표시명 |
| `option_category` | `varchar(30)` | NOT NULL | — | — | APPLIANCE, FURNITURE, SECURITY, STRUCTURE, LIVING, ETC |
| `description` | `varchar(500)` | NULL | — | — | 등록자와 개발자를 위한 정의 |
| `is_filterable` | `tinyint(1)` | NOT NULL | — | — | 검색 필터 제공 여부. 핵심 정형필드와 중복 필터 금지 |
| `is_detail_visible` | `tinyint(1)` | NOT NULL | — | — | 매물 상세 노출 여부 |
| `is_registration_enabled` | `tinyint(1)` | NOT NULL | — | — | 등록·수정 화면 선택 가능 여부 |
| `display_order` | `int` | NOT NULL | — | — | 카테고리 내부 표시 순서 |
| `is_active` | `tinyint(1)` | NOT NULL | — | — | 신규 사용 가능 여부 |
| `created_at` | `datetime(6)` | NOT NULL | — | — | — |
| `created_by_member_id` | `bigint` | NULL | — | — | — |
| `created_by_role` | `varchar(30)` | NULL | — | — | — |
| `updated_at` | `datetime(6)` | NOT NULL | — | — | — |
| `updated_by_member_id` | `bigint` | NULL | — | — | — |
| `updated_by_role` | `varchar(30)` | NULL | — | — | — |
| `deleted_at` | `datetime(6)` | NULL | — | INDEX | — |
| `deleted_by_member_id` | `bigint` | NULL | — | — | — |
| `deleted_by_role` | `varchar(30)` | NULL | — | — | — |
| `delete_reason` | `varchar(500)` | NULL | — | — | — |
| `action_source` | `varchar(20)` | NOT NULL | — | — | — |

### 3-11. property_type_option

매물 유형과 옵션 코드의 허용 관계이다.

| 컬럼 | 데이터 타입 | NULL 허용 | 기본값 | 키 | 설명 |
| --- | --- | --- | --- | --- | --- |
| `property_type_option_id` | `bigint` | NOT NULL | — | PK | 유형-옵션 정책 ID. MySQL AUTO_INCREMENT 생성 |
| `property_type` | `varchar(30)` | NOT NULL | — | INDEX | APARTMENT, OFFICETEL, VILLA, ROOM |
| `option_code_id` | `bigint` | NOT NULL | — | — | 허용할 옵션 기준 |
| `is_required` | `tinyint(1)` | NOT NULL | — | — | 해당 유형 등록 시 필수 여부 |
| `default_value` | `varchar(5)` | NULL | — | — | 기본 옵션 값. true 또는 false. 미확인 시 NULL |
| `display_order` | `int` | NOT NULL | — | — | 유형별 등록 화면 순서 |
| `created_at` | `datetime(6)` | NOT NULL | — | — | — |
| `created_by_member_id` | `bigint` | NULL | — | — | — |
| `created_by_role` | `varchar(30)` | NULL | — | — | — |
| `updated_at` | `datetime(6)` | NOT NULL | — | — | — |
| `updated_by_member_id` | `bigint` | NULL | — | — | — |
| `updated_by_role` | `varchar(30)` | NULL | — | — | — |
| `deleted_at` | `datetime(6)` | NULL | — | — | — |
| `deleted_by_member_id` | `bigint` | NULL | — | — | — |
| `deleted_by_role` | `varchar(30)` | NULL | — | — | — |
| `delete_reason` | `varchar(500)` | NULL | — | — | — |
| `action_source` | `varchar(20)` | NOT NULL | — | — | — |

### 3-12. property_option_history

매물 옵션 변경 이력이다.

| 컬럼 | 데이터 타입 | NULL 허용 | 기본값 | 키 | 설명 |
| --- | --- | --- | --- | --- | --- |
| `property_option_history_id` | `bigint` | NOT NULL | — | PK | 옵션 변경 이력 ID. MySQL AUTO_INCREMENT 생성 |
| `property_revision_id` | `bigint` | NOT NULL | — | INDEX | 이 옵션 변경이 포함된 매물 revision |
| `property_option_id` | `bigint` | NOT NULL | — | INDEX | 변경된 property_option 행 |
| `option_code_id` | `bigint` | NOT NULL | — | — | 변경 당시 옵션 코드 |
| `change_type` | `varchar(30)` | NOT NULL | — | — | CREATE, UPDATE, SOFT_DELETE |
| `changed_fields` | `varchar(500)` | NOT NULL | — | — | 변경 필드명 목록. 쉼표 구분 문자열 |
| `before_value` | `varchar(5)` | NULL | — | — | 변경 전 옵션 값: true 또는 false |
| `after_value` | `varchar(5)` | NULL | — | — | 변경 후 옵션 값: true 또는 false |
| `before_display_order` | `int` | NULL | — | — | 변경 전 표시 순서 |
| `after_display_order` | `int` | NULL | — | — | 변경 후 표시 순서 |
| `before_verified` | `tinyint(1)` | NULL | — | — | 변경 전 검증 여부 |
| `after_verified` | `tinyint(1)` | NULL | — | — | 변경 후 검증 여부 |
| `before_deleted_at` | `datetime(6)` | NULL | — | — | 변경 전 소프트 삭제 시각 |
| `after_deleted_at` | `datetime(6)` | NULL | — | — | 변경 후 소프트 삭제 시각 |
| `occurred_at` | `datetime(6)` | NOT NULL | — | — | 옵션 변경이 실제 완료된 UTC 시각 |
| `created_at` | `datetime(6)` | NOT NULL | — | — | — |
| `created_by_member_id` | `bigint` | NULL | — | — | — |
| `created_by_role` | `varchar(30)` | NULL | — | — | — |
| `updated_at` | `datetime(6)` | NOT NULL | — | — | — |
| `updated_by_member_id` | `bigint` | NULL | — | — | — |
| `updated_by_role` | `varchar(30)` | NULL | — | — | — |
| `deleted_at` | `datetime(6)` | NULL | — | — | — |
| `deleted_by_member_id` | `bigint` | NULL | — | — | — |
| `deleted_by_role` | `varchar(30)` | NULL | — | — | — |
| `delete_reason` | `varchar(500)` | NULL | — | — | — |
| `action_source` | `varchar(20)` | NOT NULL | — | — | — |

### 3-13. property_revision

매물 변경 Revision이다.

| 컬럼 | 데이터 타입 | NULL 허용 | 기본값 | 키 | 설명 |
| --- | --- | --- | --- | --- | --- |
| `property_revision_id` | `bigint` | NOT NULL | — | PK | — |
| `property_id` | `bigint` | NOT NULL | — | INDEX | — |
| `property_version` | `bigint` | NOT NULL | — | — | — |
| `change_type` | `varchar(30)` | NOT NULL | — | — | — |
| `change_scope` | `varchar(30)` | NOT NULL | — | — | — |
| `changed_fields_json` | `json` | NOT NULL | — | — | — |
| `before_snapshot_json` | `json` | NULL | — | — | — |
| `after_snapshot_json` | `json` | NOT NULL | — | — | — |
| `snapshot_schema_version` | `int` | NOT NULL | — | — | — |
| `actor_member_id` | `bigint` | NULL | — | — | — |
| `actor_role` | `varchar(30)` | NULL | — | — | — |
| `change_reason` | `varchar(1000)` | NULL | — | — | — |
| `trace_id` | `varchar(100)` | NOT NULL | — | — | — |
| `occurred_at` | `datetime(6)` | NOT NULL | — | — | — |
| `created_at` | `datetime(6)` | NOT NULL | — | — | — |
| `created_by_member_id` | `bigint` | NULL | — | — | — |
| `created_by_role` | `varchar(30)` | NULL | — | — | — |
| `updated_at` | `datetime(6)` | NOT NULL | — | — | — |
| `updated_by_member_id` | `bigint` | NULL | — | — | — |
| `updated_by_role` | `varchar(30)` | NULL | — | — | — |
| `deleted_at` | `datetime(6)` | NULL | — | — | — |
| `deleted_by_member_id` | `bigint` | NULL | — | — | — |
| `deleted_by_role` | `varchar(30)` | NULL | — | — | — |
| `delete_reason` | `varchar(500)` | NULL | — | — | — |
| `action_source` | `varchar(20)` | NOT NULL | — | — | — |

### 3-14. property_status_history

거래·공개·인증 상태 변경 이력이다.

| 컬럼 | 데이터 타입 | NULL 허용 | 기본값 | 키 | 설명 |
| --- | --- | --- | --- | --- | --- |
| `property_status_history_id` | `bigint` | NOT NULL | — | PK | — |
| `property_id` | `bigint` | NOT NULL | — | INDEX | — |
| `property_revision_id` | `bigint` | NOT NULL | — | INDEX | — |
| `status_type` | `varchar(30)` | NOT NULL | — | — | — |
| `before_status` | `varchar(30)` | NULL | — | — | — |
| `after_status` | `varchar(30)` | NOT NULL | — | — | — |
| `actor_member_id` | `bigint` | NULL | — | — | — |
| `actor_role` | `varchar(30)` | NULL | — | — | — |
| `reason_code` | `varchar(50)` | NULL | — | — | — |
| `reason` | `varchar(1000)` | NULL | — | — | — |
| `property_version` | `bigint` | NOT NULL | — | — | — |
| `trace_id` | `varchar(100)` | NOT NULL | — | — | — |
| `occurred_at` | `datetime(6)` | NOT NULL | — | — | — |
| `created_at` | `datetime(6)` | NOT NULL | — | — | — |
| `created_by_member_id` | `bigint` | NULL | — | — | — |
| `created_by_role` | `varchar(30)` | NULL | — | — | — |
| `updated_at` | `datetime(6)` | NOT NULL | — | — | — |
| `updated_by_member_id` | `bigint` | NULL | — | — | — |
| `updated_by_role` | `varchar(30)` | NULL | — | — | — |
| `deleted_at` | `datetime(6)` | NULL | — | — | — |
| `deleted_by_member_id` | `bigint` | NULL | — | — | — |
| `deleted_by_role` | `varchar(30)` | NULL | — | — | — |
| `delete_reason` | `varchar(500)` | NULL | — | — | — |
| `action_source` | `varchar(20)` | NOT NULL | — | — | — |

### 3-15. property_publisher_snapshot

등록자 정보 스냅샷이다.

| 컬럼 | 데이터 타입 | NULL 허용 | 기본값 | 키 | 설명 |
| --- | --- | --- | --- | --- | --- |
| `publisher_snapshot_id` | `bigint` | NOT NULL | — | PK | — |
| `property_id` | `bigint` | NOT NULL | — | INDEX | — |
| `property_revision_id` | `bigint` | NOT NULL | — | UNIQUE | — |
| `publisher_type` | `varchar(30)` | NOT NULL | — | — | — |
| `publisher_member_id` | `bigint` | NOT NULL | — | — | — |
| `agent_id` | `bigint` | NULL | — | — | — |
| `agency_id` | `bigint` | NULL | — | — | — |
| `office_name` | `varchar(200)` | NULL | — | — | — |
| `office_address` | `varchar(300)` | NULL | — | — | — |
| `office_contact` | `varchar(50)` | NULL | — | — | — |
| `registration_number` | `varchar(100)` | NULL | — | — | — |
| `broker_name` | `varchar(100)` | NULL | — | — | — |
| `snapshot_json` | `json` | NULL | — | — | — |
| `captured_at` | `datetime(6)` | NOT NULL | — | — | — |
| `created_at` | `datetime(6)` | NOT NULL | — | — | — |
| `created_by_member_id` | `bigint` | NULL | — | — | — |
| `created_by_role` | `varchar(30)` | NULL | — | — | — |
| `updated_at` | `datetime(6)` | NOT NULL | — | — | — |
| `updated_by_member_id` | `bigint` | NULL | — | — | — |
| `updated_by_role` | `varchar(30)` | NULL | — | — | — |
| `deleted_at` | `datetime(6)` | NULL | — | — | — |
| `deleted_by_member_id` | `bigint` | NULL | — | — | — |
| `deleted_by_role` | `varchar(30)` | NULL | — | — | — |
| `delete_reason` | `varchar(500)` | NULL | — | — | — |
| `action_source` | `varchar(20)` | NOT NULL | — | — | — |

### 3-16. property_audit_event

매물 관련 감사 이벤트이다.

| 컬럼 | 데이터 타입 | NULL 허용 | 기본값 | 키 | 설명 |
| --- | --- | --- | --- | --- | --- |
| `audit_event_id` | `bigint` | NOT NULL | — | PK | 감사 이벤트 내부 ID |
| `target_type` | `varchar(50)` | NOT NULL | — | INDEX | 감사 대상 종류. 예: PROPERTY, REPORT, EXACT_LOCATION |
| `target_id` | `varchar(100)` | NOT NULL | — | — | 감사 대상의 식별자 |
| `action_code` | `varchar(100)` | NOT NULL | — | — | 감사 행위 코드. 예: PROPERTY_CREATED, VIEW_EXACT_LOCATION |
| `actor_member_id` | `bigint` | NULL | — | INDEX | 행위를 수행한 회원 ID |
| `actor_role` | `varchar(30)` | NULL | — | — | 행위 수행 당시 회원 역할 |
| `action_source` | `varchar(20)` | NOT NULL | — | — | 행위 발생 주체: MEMBER, SYSTEM, BATCH |
| `reason` | `varchar(1000)` | NULL | — | — | 행위를 수행한 사유 |
| `trace_id` | `varchar(100)` | NOT NULL | — | — | 요청 로그와 연결하기 위한 Trace ID |
| `client_ip_hash` | `char(64)` | NULL | — | — | 필요한 경우 원본 IP 대신 저장하는 SHA-256 해시 |
| `occurred_at` | `datetime(6)` | NOT NULL | — | — | 감사 대상 행위가 실제로 발생한 UTC 시각 |
| `created_at` | `datetime(6)` | NOT NULL | — | — | 감사 이벤트 행이 생성된 UTC 시각 |
| `created_by_member_id` | `bigint` | NULL | — | — | 감사 이벤트 행을 생성한 회원 ID |
| `created_by_role` | `varchar(30)` | NULL | — | — | 감사 이벤트 생성 당시 회원 역할 |
| `updated_at` | `datetime(6)` | NOT NULL | — | — | 감사 이벤트 행의 마지막 수정 UTC 시각 |
| `updated_by_member_id` | `bigint` | NULL | — | — | 감사 이벤트 행을 마지막으로 수정한 회원 ID |
| `updated_by_role` | `varchar(30)` | NULL | — | — | 마지막 수정 당시 회원 역할 |
| `deleted_at` | `datetime(6)` | NULL | — | — | 소프트 삭제 UTC 시각. NULL이면 삭제되지 않은 상태 |
| `deleted_by_member_id` | `bigint` | NULL | — | — | 소프트 삭제한 회원 ID |
| `deleted_by_role` | `varchar(30)` | NULL | — | — | 소프트 삭제 당시 회원 역할 |
| `delete_reason` | `varchar(500)` | NULL | — | — | 소프트 삭제 사유 |

### 3-17. property_file

업로드 파일 메타데이터와 검증 상태이다.

| 컬럼 | 데이터 타입 | NULL 허용 | 기본값 | 키 | 설명 |
| --- | --- | --- | --- | --- | --- |
| `property_file_id` | `bigint` | NOT NULL | — | PK | 애플리케이션에서 생성한 파일 TSID |
| `upload_session_id` | `varchar(36)` | NOT NULL | — | INDEX | 업로드 요청 단위 세션 식별자 |
| `owner_member_id` | `bigint` | NOT NULL | — | INDEX | 파일 소유 회원 ID |
| `file_purpose` | `varchar(30)` | NOT NULL | — | — | 파일 사용 용도 |
| `upload_status` | `varchar(30)` | NOT NULL | — | — | 파일 업로드 상태 |
| `original_file_name` | `varchar(255)` | NOT NULL | — | — | 경로 정보가 제거된 원본 파일명 |
| `file_size` | `bigint` | NOT NULL | — | — | 요청된 파일 크기(byte) |
| `mime_type` | `varchar(100)` | NULL | — | — | 업로드 완료 검증된 MIME 타입 |
| `object_key` | `varchar(500)` | NOT NULL | — | UNIQUE | MinIO 객체 키 |
| `checksum` | `varchar(64)` | NULL | — | — | 업로드 완료 검증 시 저장할 체크섬 |
| `expires_at` | `datetime(6)` | NOT NULL | — | INDEX | 업로드 세션 만료 시각 |
| `linked_at` | `datetime(6)` | NULL | — | — | 업무 데이터 연결 시각 |
| `object_deleted_at` | `datetime(6)` | NULL | — | — | 스토리지 객체 삭제 시각 |
| `created_at` | `datetime(6)` | NOT NULL | — | — | — |
| `created_by_member_id` | `bigint` | NULL | — | — | — |
| `created_by_role` | `varchar(30)` | NULL | — | — | — |
| `updated_at` | `datetime(6)` | NOT NULL | — | — | — |
| `updated_by_member_id` | `bigint` | NULL | — | — | — |
| `updated_by_role` | `varchar(30)` | NULL | — | — | — |
| `deleted_at` | `datetime(6)` | NULL | — | — | — |
| `deleted_by_member_id` | `bigint` | NULL | — | — | — |
| `deleted_by_role` | `varchar(30)` | NULL | — | — | — |
| `delete_reason` | `varchar(500)` | NULL | — | — | — |
| `action_source` | `varchar(20)` | NOT NULL | — | — | — |

### 3-18. property_image

매물 이미지 연결과 표시 순서이다.

| 컬럼 | 데이터 타입 | NULL 허용 | 기본값 | 키 | 설명 |
| --- | --- | --- | --- | --- | --- |
| `property_image_id` | `bigint` | NOT NULL | — | PK | 매물 이미지 연결 ID. MySQL AUTO_INCREMENT 생성 |
| `property_id` | `bigint` | NOT NULL | — | INDEX | 논리 참조: property.property_id |
| `property_file_id` | `bigint` | NOT NULL | — | INDEX | 논리 참조: property_file.property_file_id |
| `sort_order` | `int` | NOT NULL | — | — | 이미지 표시 순서. 0부터 시작 |
| `is_representative` | `tinyint(1)` | NOT NULL | — | — | 대표 이미지 여부 |
| `alt_text` | `varchar(300)` | NULL | — | — | 접근성 대체 텍스트 |
| `active_representative_key` | `bigint` | NULL | — | UNIQUE | 활성 대표 이미지만 property_id를 가지는 generated key |
| `active_property_file_key` | `bigint` | NULL | — | UNIQUE | 활성 이미지에서 유일한 property_file_id generated key |
| `created_at` | `datetime(6)` | NOT NULL | — | — | — |
| `created_by_member_id` | `bigint` | NULL | — | — | — |
| `created_by_role` | `varchar(30)` | NULL | — | — | — |
| `updated_at` | `datetime(6)` | NOT NULL | — | — | — |
| `updated_by_member_id` | `bigint` | NULL | — | — | — |
| `updated_by_role` | `varchar(30)` | NULL | — | — | — |
| `deleted_at` | `datetime(6)` | NULL | — | — | — |
| `deleted_by_member_id` | `bigint` | NULL | — | — | — |
| `deleted_by_role` | `varchar(30)` | NULL | — | — | — |
| `delete_reason` | `varchar(500)` | NULL | — | — | — |
| `action_source` | `varchar(20)` | NOT NULL | — | — | — |

### 3-19. property_verification

매물 인증 심사 상태이다.

| 컬럼 | 데이터 타입 | NULL 허용 | 기본값 | 키 | 설명 |
| --- | --- | --- | --- | --- | --- |
| `property_verification_id` | `bigint` | NOT NULL | — | PK | — |
| `property_id` | `bigint` | NOT NULL | — | INDEX | — |
| `verification_type` | `varchar(30)` | NOT NULL | — | — | — |
| `applicant_member_id` | `bigint` | NOT NULL | — | INDEX | — |
| `status` | `varchar(30)` | NOT NULL | — | INDEX | — |
| `submitted_at` | `datetime(6)` | NOT NULL | — | — | — |
| `reviewer_member_id` | `bigint` | NULL | — | — | — |
| `reviewer_role` | `varchar(30)` | NULL | — | — | — |
| `reviewed_at` | `datetime(6)` | NULL | — | — | — |
| `verified_at` | `datetime(6)` | NULL | — | — | — |
| `expires_at` | `datetime(6)` | NULL | — | — | — |
| `renewal_notified_at` | `datetime(6)` | NULL | — | — | 현재 인증 주기의 재인증 안내 발행 시각 |
| `result_code` | `varchar(50)` | NULL | — | — | — |
| `result_reason` | `varchar(1000)` | NULL | — | — | — |
| `verification_version` | `int` | NOT NULL | — | — | — |
| `created_at` | `datetime(6)` | NOT NULL | — | — | — |
| `created_by_member_id` | `bigint` | NULL | — | — | — |
| `created_by_role` | `varchar(30)` | NULL | — | — | — |
| `updated_at` | `datetime(6)` | NOT NULL | — | — | — |
| `updated_by_member_id` | `bigint` | NULL | — | — | — |
| `updated_by_role` | `varchar(30)` | NULL | — | — | — |
| `deleted_at` | `datetime(6)` | NULL | — | — | — |
| `deleted_by_member_id` | `bigint` | NULL | — | — | — |
| `deleted_by_role` | `varchar(30)` | NULL | — | — | — |
| `delete_reason` | `varchar(500)` | NULL | — | — | — |
| `action_source` | `varchar(20)` | NOT NULL | — | — | — |
| `active_verification_key` | `varchar(100)` | NULL | — | UNIQUE | — |

### 3-20. property_verification_evidence

인증 증빙 파일 연결이다.

| 컬럼 | 데이터 타입 | NULL 허용 | 기본값 | 키 | 설명 |
| --- | --- | --- | --- | --- | --- |
| `verification_evidence_id` | `bigint` | NOT NULL | — | PK | — |
| `property_verification_id` | `bigint` | NOT NULL | — | INDEX | — |
| `property_file_id` | `bigint` | NOT NULL | — | INDEX | — |
| `evidence_type` | `varchar(50)` | NOT NULL | — | — | — |
| `sort_order` | `int` | NOT NULL | — | — | — |
| `created_at` | `datetime(6)` | NOT NULL | — | — | — |
| `created_by_member_id` | `bigint` | NULL | — | — | — |
| `created_by_role` | `varchar(30)` | NULL | — | — | — |
| `updated_at` | `datetime(6)` | NOT NULL | — | — | — |
| `updated_by_member_id` | `bigint` | NULL | — | — | — |
| `updated_by_role` | `varchar(30)` | NULL | — | — | — |
| `deleted_at` | `datetime(6)` | NULL | — | — | — |
| `deleted_by_member_id` | `bigint` | NULL | — | — | — |
| `deleted_by_role` | `varchar(30)` | NULL | — | — | — |
| `delete_reason` | `varchar(500)` | NULL | — | — | — |
| `action_source` | `varchar(20)` | NOT NULL | — | — | — |

### 3-21. property_risk_assessment

등록 전 위험 평가 결과이다.

| 컬럼 | 데이터 타입 | NULL 허용 | 기본값 | 키 | 설명 |
| --- | --- | --- | --- | --- | --- |
| `risk_assessment_id` | `bigint` | NOT NULL | — | PK | 등록 전 위험검사 이력 ID |
| `property_id` | `bigint` | NOT NULL | — | INDEX | 등록 시도에 미리 발급한 매물 TSID. 차단 시 property 행은 생성되지 않을 수 있음 |
| `publisher_member_id` | `bigint` | NOT NULL | — | INDEX | 등록을 시도한 Member 외부 ID |
| `normalized_address_hash` | `char(64)` | NOT NULL | — | — | 정규화 표준주소 SHA-256. 공개 DTO와 로그에 노출 금지 |
| `matched_rule_codes_json` | `json` | NOT NULL | — | — | 자동판정에 일치한 규칙 코드 목록 |
| `risk_score` | `decimal(5,2)` | NOT NULL | — | — | 0 이상 100 이하의 등록 위험점수 |
| `decision` | `varchar(30)` | NOT NULL | — | INDEX | PASS, IN_REVIEW, BLOCKED |
| `duplicate_property_id` | `bigint` | NULL | — | — | 대표 중복 후보 매물 ID |
| `evaluated_at` | `datetime(6)` | NOT NULL | — | — | 자동 위험검사 완료 UTC 시각 |
| `created_at` | `datetime(6)` | NOT NULL | — | — | — |
| `created_by_member_id` | `bigint` | NULL | — | — | — |
| `created_by_role` | `varchar(30)` | NULL | — | — | — |
| `updated_at` | `datetime(6)` | NOT NULL | — | — | — |
| `updated_by_member_id` | `bigint` | NULL | — | — | — |
| `updated_by_role` | `varchar(30)` | NULL | — | — | — |
| `deleted_at` | `datetime(6)` | NULL | — | — | — |
| `deleted_by_member_id` | `bigint` | NULL | — | — | — |
| `deleted_by_role` | `varchar(30)` | NULL | — | — | — |
| `delete_reason` | `varchar(500)` | NULL | — | — | — |
| `action_source` | `varchar(20)` | NOT NULL | — | — | — |

### 3-22. property_favorite

회원별 관심 매물 상태이다.

| 컬럼 | 데이터 타입 | NULL 허용 | 기본값 | 키 | 설명 |
| --- | --- | --- | --- | --- | --- |
| `favorite_id` | `bigint` | NOT NULL | — | PK | 찜 ID. MySQL AUTO_INCREMENT 생성 |
| `member_id` | `bigint` | NOT NULL | — | INDEX | 논리 참조: Member 서비스 회원 ID |
| `property_id` | `bigint` | NOT NULL | — | INDEX | 논리 참조: property.property_id |
| `active_favorite_key` | `varchar(150)` | NULL | — | UNIQUE | 활성 회원+매물 찜 조합을 DB가 계산하는 generated key |
| `created_at` | `datetime(6)` | NOT NULL | — | — | — |
| `created_by_member_id` | `bigint` | NULL | — | — | — |
| `created_by_role` | `varchar(30)` | NULL | — | — | — |
| `updated_at` | `datetime(6)` | NOT NULL | — | — | — |
| `updated_by_member_id` | `bigint` | NULL | — | — | — |
| `updated_by_role` | `varchar(30)` | NULL | — | — | — |
| `deleted_at` | `datetime(6)` | NULL | — | — | — |
| `deleted_by_member_id` | `bigint` | NULL | — | — | — |
| `deleted_by_role` | `varchar(30)` | NULL | — | — | — |
| `delete_reason` | `varchar(500)` | NULL | — | — | — |
| `action_source` | `varchar(20)` | NOT NULL | — | — | — |

### 3-23. property_report

매물 신고와 처리 상태이다.

| 컬럼 | 데이터 타입 | NULL 허용 | 기본값 | 키 | 설명 |
| --- | --- | --- | --- | --- | --- |
| `report_id` | `bigint` | NOT NULL | — | PK | Java TSID 생성 신고 ID |
| `property_id` | `bigint` | NOT NULL | — | INDEX | 논리 참조: property.property_id |
| `reporter_member_id` | `bigint` | NOT NULL | — | INDEX | 논리 참조: Member 서비스 신고자 ID |
| `reason_code` | `varchar(50)` | NOT NULL | — | — | 신고 사유 코드 |
| `status` | `varchar(30)` | NOT NULL | — | — | 신고 처리 상태 |
| `detail` | `varchar(1000)` | NOT NULL | — | — | 신고 상세 내용 |
| `risk_score` | `decimal(5,2)` | NULL | — | — | 관리자 판단 위험 점수 |
| `assigned_admin_id` | `bigint` | NULL | — | — | 논리 참조: 담당 관리자 ID |
| `version` | `bigint` | NOT NULL | — | — | JPA 낙관적 잠금 버전 |
| `active_report_key` | `varchar(150)` | NULL | — | UNIQUE | 진행 중 신고자+매물+사유 조합을 DB가 계산하는 generated key |
| `created_at` | `datetime(6)` | NOT NULL | — | — | — |
| `created_by_member_id` | `bigint` | NULL | — | — | — |
| `created_by_role` | `varchar(30)` | NULL | — | — | — |
| `updated_at` | `datetime(6)` | NOT NULL | — | — | — |
| `updated_by_member_id` | `bigint` | NULL | — | — | — |
| `updated_by_role` | `varchar(30)` | NULL | — | — | — |
| `deleted_at` | `datetime(6)` | NULL | — | — | — |
| `deleted_by_member_id` | `bigint` | NULL | — | — | — |
| `deleted_by_role` | `varchar(30)` | NULL | — | — | — |
| `delete_reason` | `varchar(500)` | NULL | — | — | — |
| `action_source` | `varchar(20)` | NOT NULL | — | — | — |

### 3-24. property_report_action

관리자 신고 조치 이력이다.

| 컬럼 | 데이터 타입 | NULL 허용 | 기본값 | 키 | 설명 |
| --- | --- | --- | --- | --- | --- |
| `action_id` | `bigint` | NOT NULL | — | PK | 신고 운영조치 내부 ID |
| `report_id` | `bigint` | NOT NULL | — | INDEX | 논리 참조: property_report.report_id |
| `property_id` | `bigint` | NOT NULL | — | INDEX | 논리 참조: property.property_id |
| `action_code` | `varchar(50)` | NOT NULL | — | — | 신고 운영조치 코드 |
| `reason` | `varchar(1000)` | NOT NULL | — | — | 운영조치 사유 |
| `actor_member_id` | `bigint` | NOT NULL | — | INDEX | 운영조치를 실행한 관리자 Member ID |
| `actor_role` | `varchar(30)` | NOT NULL | — | — | 운영조치 실행 당시 관리자 역할 |
| `trace_id` | `varchar(100)` | NOT NULL | — | — | 요청 로그와 연결하기 위한 Trace ID |
| `executed_at` | `datetime(6)` | NOT NULL | — | — | 운영조치가 실행된 UTC 시각 |
| `created_at` | `datetime(6)` | NOT NULL | — | — | — |
| `created_by_member_id` | `bigint` | NULL | — | — | — |
| `created_by_role` | `varchar(30)` | NULL | — | — | — |
| `updated_at` | `datetime(6)` | NOT NULL | — | — | — |
| `updated_by_member_id` | `bigint` | NULL | — | — | — |
| `updated_by_role` | `varchar(30)` | NULL | — | — | — |
| `deleted_at` | `datetime(6)` | NULL | — | — | — |
| `deleted_by_member_id` | `bigint` | NULL | — | — | — |
| `deleted_by_role` | `varchar(30)` | NULL | — | — | — |
| `delete_reason` | `varchar(500)` | NULL | — | — | — |
| `action_source` | `varchar(20)` | NOT NULL | — | — | — |

### 3-25. property_report_appeal

신고 대상 매물 작성자의 이의신청이다.

| 컬럼 | 데이터 타입 | NULL 허용 | 기본값 | 키 | 설명 |
| --- | --- | --- | --- | --- | --- |
| `appeal_id` | `bigint` | NOT NULL | — | PK | 운영조치 이의신청 내부 ID |
| `report_id` | `bigint` | NOT NULL | — | UNIQUE | 논리 참조: property_report.report_id |
| `appellant_member_id` | `bigint` | NOT NULL | — | — | 논리 참조: Member 서비스 신청자 ID |
| `detail` | `varchar(2000)` | NOT NULL | — | — | 이의신청 상세 내용 |
| `status` | `varchar(30)` | NOT NULL | — | — | 이의신청 처리 상태 |
| `reviewer_member_id` | `bigint` | NULL | — | — | 논리 참조: Member 서비스 검토자 ID |
| `review_reason` | `varchar(1000)` | NULL | — | — | 검토 사유 |
| `reviewed_at` | `datetime(6)` | NULL | — | — | 검토 완료 UTC 시각 |
| `version` | `bigint` | NOT NULL | — | — | JPA 낙관적 잠금 버전 |
| `created_at` | `datetime(6)` | NOT NULL | — | — | — |
| `created_by_member_id` | `bigint` | NULL | — | — | — |
| `created_by_role` | `varchar(30)` | NULL | — | — | — |
| `updated_at` | `datetime(6)` | NOT NULL | — | — | — |
| `updated_by_member_id` | `bigint` | NULL | — | — | — |
| `updated_by_role` | `varchar(30)` | NULL | — | — | — |
| `deleted_at` | `datetime(6)` | NULL | — | — | — |
| `deleted_by_member_id` | `bigint` | NULL | — | — | — |
| `deleted_by_role` | `varchar(30)` | NULL | — | — | — |
| `delete_reason` | `varchar(500)` | NULL | — | — | — |
| `action_source` | `varchar(20)` | NOT NULL | — | — | — |

### 3-26. property_report_evidence

신고 증빙 파일 연결이다.

| 컬럼 | 데이터 타입 | NULL 허용 | 기본값 | 키 | 설명 |
| --- | --- | --- | --- | --- | --- |
| `report_evidence_id` | `bigint` | NOT NULL | — | PK | 신고 증빙 연결 ID. MySQL AUTO_INCREMENT 생성 |
| `report_id` | `bigint` | NOT NULL | — | INDEX | 논리 참조: property_report.report_id |
| `property_file_id` | `bigint` | NOT NULL | — | INDEX | 논리 참조: property_file.property_file_id |
| `evidence_type` | `varchar(30)` | NOT NULL | — | — | 현재 이미지 증빙 유형(SCREENSHOT) |
| `sort_order` | `int` | NOT NULL | — | — | 요청 evidenceFileIds 순서. 0부터 시작 |
| `active_report_file_key` | `varchar(50)` | NULL | — | UNIQUE | 활성 신고+파일 연결을 DB가 계산하는 generated key |
| `created_at` | `datetime(6)` | NOT NULL | — | — | — |
| `created_by_member_id` | `bigint` | NULL | — | — | — |
| `created_by_role` | `varchar(30)` | NULL | — | — | — |
| `updated_at` | `datetime(6)` | NOT NULL | — | — | — |
| `updated_by_member_id` | `bigint` | NULL | — | — | — |
| `updated_by_role` | `varchar(30)` | NULL | — | — | — |
| `deleted_at` | `datetime(6)` | NULL | — | — | — |
| `deleted_by_member_id` | `bigint` | NULL | — | — | — |
| `deleted_by_role` | `varchar(30)` | NULL | — | — | — |
| `delete_reason` | `varchar(500)` | NULL | — | — | — |
| `action_source` | `varchar(20)` | NOT NULL | — | — | — |

### 3-27. property_idempotency

매물 등록 중복 방지 요청 기록이다.

| 컬럼 | 데이터 타입 | NULL 허용 | 기본값 | 키 | 설명 |
| --- | --- | --- | --- | --- | --- |
| `idempotency_id` | `bigint` | NOT NULL | — | PK | 멱등 요청 기록 내부 ID |
| `member_id` | `bigint` | NOT NULL | — | INDEX | 요청한 Member 서비스 회원 ID |
| `endpoint_key` | `varchar(200)` | NOT NULL | — | — | 요청 API 식별자. 예: POST:/api/property/properties |
| `idempotency_key` | `varchar(100)` | NOT NULL | — | — | 클라이언트가 전달한 Idempotency-Key 헤더값 |
| `active_idempotency_key` | `varchar(100)` | NULL | — | — | 활성 멱등 요청에서만 유일성을 적용하는 generated key |
| `request_hash` | `char(64)` | NOT NULL | — | — | 요청 본문을 정규화한 SHA-256 해시 |
| `processing_status` | `varchar(20)` | NOT NULL | — | — | 요청 처리 상태: PROCESSING, COMPLETED, FAILED |
| `resource_type` | `varchar(50)` | NULL | — | — | 생성된 리소스 종류. 예: PROPERTY |
| `resource_id` | `bigint` | NULL | — | — | 요청 처리로 생성된 리소스 ID |
| `response_status` | `smallint` | NULL | — | — | 중복 요청에 재사용할 HTTP 상태 코드 |
| `response_body_json` | `json` | NULL | — | — | 중복 요청에 재사용할 최소 응답 JSON |
| `expires_at` | `datetime(6)` | NOT NULL | — | INDEX | 멱등 키 보존 만료 시각 |
| `created_at` | `datetime(6)` | NOT NULL | — | — | — |
| `created_by_member_id` | `bigint` | NULL | — | — | — |
| `created_by_role` | `varchar(30)` | NULL | — | — | — |
| `updated_at` | `datetime(6)` | NOT NULL | — | — | — |
| `updated_by_member_id` | `bigint` | NULL | — | — | — |
| `updated_by_role` | `varchar(30)` | NULL | — | — | — |
| `deleted_at` | `datetime(6)` | NULL | — | INDEX | — |
| `deleted_by_member_id` | `bigint` | NULL | — | — | — |
| `deleted_by_role` | `varchar(30)` | NULL | — | — | — |
| `delete_reason` | `varchar(500)` | NULL | — | — | — |
| `action_source` | `varchar(20)` | NOT NULL | — | — | — |

### 3-28. property_member_event_consumption

처리한 Member 이벤트 이력이다.

| 컬럼 | 데이터 타입 | NULL 허용 | 기본값 | 키 | 설명 |
| --- | --- | --- | --- | --- | --- |
| `event_id` | `varchar(100)` | NOT NULL | — | PK | Member 이벤트 고유 ID. 중복 소비 방지 키 |
| `event_type` | `varchar(50)` | NOT NULL | — | INDEX | MemberWithdrawn, MemberSanctioned, MemberSanctionReleased, AgentApproved, AgentSuspended |
| `member_id` | `bigint` | NOT NULL | — | INDEX | 이벤트 대상 Member 외부 ID |
| `occurred_at` | `datetime(6)` | NOT NULL | — | — | Member 이벤트 발생 UTC 시각 |
| `processed_at` | `datetime(6)` | NOT NULL | — | — | Property 서비스 처리 완료 UTC 시각 |

### 3-29. property_member_state

Member 상태의 로컬 투영이다.

| 컬럼 | 데이터 타입 | NULL 허용 | 기본값 | 키 | 설명 |
| --- | --- | --- | --- | --- | --- |
| `member_id` | `bigint` | NOT NULL | — | PK | Member 서비스가 발급한 외부 회원 ID |
| `withdrawn_at` | `datetime(6)` | NULL | — | — | MemberWithdrawn 이벤트 발생 UTC 시각 |
| `sanction_scope` | `varchar(100)` | NULL | — | — | MemberSanctioned 이벤트의 제한 범위. 제재가 없으면 NULL |
| `sanctioned_at` | `datetime(6)` | NULL | — | INDEX | 현재 제재 시작 UTC 시각. 제재가 없으면 NULL |
| `agent_id` | `bigint` | NULL | — | INDEX | Member 서비스의 중개사 외부 ID |
| `agency_id` | `bigint` | NULL | — | — | Member 서비스의 중개사무소 외부 ID |
| `agent_active` | `tinyint(1)` | NOT NULL | `0` | — | AgentApproved 이후 true, AgentSuspended 이후 false |
| `last_event_id` | `varchar(100)` | NOT NULL | — | — | 마지막으로 상태에 반영한 Member eventId |
| `last_event_at` | `datetime(6)` | NOT NULL | — | — | 마지막으로 상태에 반영한 Member 이벤트 발생 UTC 시각 |
| `created_at` | `datetime(6)` | NOT NULL | — | — | 읽기 모델 생성 UTC 시각 |
| `updated_at` | `datetime(6)` | NOT NULL | — | — | 읽기 모델 마지막 갱신 UTC 시각 |

## 4. 데이터 설계 기준

### 4.1. 관계와 무결성

- 모든 테이블에 기본 키 적용
- 업무상 중복 방지를 위한 고유 제약과 입력 범위를 제한하는 Check 제약 적용
- 물리 Foreign Key 없이 식별자 기반 논리 관계 사용
- 관계 무결성은 애플리케이션의 등록·수정·삭제 정책에서 관리

### 4.2. 논리 삭제

29개 테이블 중 27개 테이블에 `deleted_at`을 사용한다. 기본 조회에서는 삭제 데이터를 제외하고 복구·감사 흐름에서 필요한 경우에만 명시적으로 조회한다.

다음 연동 상태 테이블은 논리 삭제 대상이 아니다.

- `property_member_event_consumption`
- `property_member_state`

### 4.3. 동시 수정 제어

다음 테이블은 `version` 값을 이용해 오래된 상태를 기준으로 한 수정을 차단한다.

- `property`
- `property_report`
- `property_report_appeal`

API에서는 현재 버전을 `If-Match`로 전달하며, 일치하지 않으면 충돌 응답을 반환한다.

### 4.4. 공간·이력 데이터

- `POINT`: 단지 중심, 매물 원본·공개 위치, 지역 중심 좌표
- `MULTIPOLYGON`: 지역 경계
- `JSON`: 변경 스냅샷, 멱등 응답, 위험 평가 근거와 지역 적재 오류

원본 좌표와 공개 좌표를 분리해 정확한 주소가 공개 검색 응답에 노출되지 않도록 관리한다.

## 5. 실행 설정

- 데이터베이스명: `zipda_property`
- Hibernate 스키마 자동 생성: `ddl-auto: none`
- 실행 환경의 DB 접속 정보가 실제 `zipda_property`를 가리키도록 설정
- 운영 데이터나 비밀값을 README·Git·로그에 기록하지 않음

환경 변수와 실행 절차는 [개발 환경 세팅](07-development-setup.md)을 참고한다.

