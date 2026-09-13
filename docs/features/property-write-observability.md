# Property 쓰기 API 관측성

## 목적

`NFR-PR-005`에 따라 Property 쓰기 API의 처리 결과와 지연시간을
애플리케이션 로그로 기록한다. 로그 집계 시스템은 이 기록을 이용해
요청 수, 오류율, 상태 코드별 건수와 지연시간을 계산할 수 있다.

## 적용 범위

- 경로: `/api/property/properties` 및 모든 하위 경로
- 메서드: `POST`, `PUT`, `PATCH`, `DELETE`
- 조회 메서드와 다른 도메인의 API는 이 기록의 대상이 아니다.

## 로그 계약

성공 응답은 `INFO`, 4xx 및 5xx 응답은 `WARN`으로 기록한다.

```text
property_write_api completed method=PATCH status=200 outcome=SUCCESS durationMs=35
```

공통 로그 패턴의 MDC `traceId`가 함께 출력되므로 SCG 요청과 연결할 수
있다. 필터 체인에서 처리되지 않은 예외는 상태 코드가 아직 설정되지
않았더라도 `500 / ERROR`로 기록한다.

## 민감정보 보호

로그에는 다음 값을 포함하지 않는다.

- 요청 본문과 응답 본문
- propertyId를 포함한 실제 요청 경로
- 정확 주소와 좌표
- 증빙 식별자
- 인증 헤더 및 외부 API 키

## 운영 연계

5분 단위 API 오류율 5% 초과 경고와 로그 보존기간 설정은 배포 환경의
로그 수집·알림 시스템에서 이 로그 계약을 기준으로 구성한다.

## 검증

`PropertyWriteObservabilityFilterTest`에서 쓰기 요청 기록, 조회 제외,
유사 경로 제외 및 미처리 예외의 500 기록을 검증한다.
