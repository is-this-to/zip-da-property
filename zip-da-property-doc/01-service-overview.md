# Property 서비스 개요

> ZIPDA MSA에서 매물의 등록·검색·공개와 지역·단지, 이미지, 인증, 찜·신고 데이터를 소유하는 도메인 서비스이다.

## 1. 서비스 역할

`zip-da-property`는 매물 생명주기와 검색 정책을 처리하는 Spring Boot 서비스이다.

- 사용자: 지도·조건 기반 매물 검색, 상세 조회, 찜과 신고
- 중개사: 매물 등록·수정, 이미지 관리, 공개·거래 상태 변경
- 관리자: 매물·인증·신고 검토와 운영 상태 변경
- 운영 작업: 인증 만료, 증빙·미연결 파일·멱등 키 정리

## 2. MSA 책임 경계

| 구성 요소 | 책임 |
| --- | --- |
| `zip-da-client` | 사용자·중개사 화면과 Gateway API 호출 |
| `zip-da-admin-client` | 관리자 화면과 Gateway API 호출 |
| `zip-da-scg` | JWT 검증, 내부 인증 정보·Trace ID 전달, 요청 라우팅 |
| `zip-da-member` | 회원·중개사 계정 상태와 매물 쓰기 권한 제공 |
| **`zip-da-property`** | 매물·지역·파일·인증·찜·신고 데이터와 업무 규칙 소유 |

Property는 JWT를 직접 검증하지 않는다. Gateway가 전달한 내부 인증 헤더를 사용하고, 매물 등록·수정 시 Member 서비스에서 회원 상태와 권한을 추가 확인한다.

## 3. 주요 도메인

| 도메인 | 처리 범위 |
| --- | --- |
| 매물 | 등록·수정·조회, 공개·거래 상태, 삭제·복구, 변경 이력 |
| 검색·지역 | 지도 영역 집계, 위치·가격 마커, 조건·커서 검색, 지역·단지 정보 |
| 파일·이미지 | Presigned URL 발급, 업로드 완료 검증, 대표 이미지와 정렬 |
| 인증·위험 평가 | 소유자·세입자·중개사 인증, 만료·재인증, 허위·중복 위험 분석 |
| 사용자 상호작용 | 찜, 신고와 이의신청 |
| 운영 | 감사 이벤트, 멱등성, 논리 삭제, 예약 정리 작업 |

## 4. 요청 처리 흐름

```text
Client / Admin Client
        ↓
API Gateway
  JWT 검증 · 내부 인증 헤더와 Trace ID 생성
        ↓
Property Controller
        ↓
Application Service
  권한 · 상태 · 버전 · 업무 규칙 확인
        ↓
Repository / External Client / Event Publisher
  MySQL · MinIO · Kakao Local · Member · Kafka
```

읽기 요청은 공개 범위와 검색 조건을 적용하고, 쓰기 요청은 작성자·회원 권한, 멱등성 키와 현재 버전을 확인한다.

## 5. 기술 스택

| 영역 | 기술 |
| --- | --- |
| 실행 환경 | Java 21 |
| 애플리케이션 | Spring Boot 4.1.1, Spring MVC, Spring Security |
| 데이터 | Spring Data JPA, QueryDSL 5.1, Hibernate Spatial, MySQL 8.4 |
| 파일 | MinIO 8.6, Presigned URL |
| 연동 | Kakao Local REST API, Member 내부 API, Spring Kafka |
| 문서·빌드 | springdoc-openapi 3.0.3, Gradle Wrapper, GitHub Actions |

## 6. 관련 문서

- [ERD와 데이터베이스](02-erd-and-database.md)
- [Property 서비스 아키텍처](03-architecture.md)
- [주요 기능](06-key-features.md)
- [개발 환경 세팅](07-development-setup.md)

