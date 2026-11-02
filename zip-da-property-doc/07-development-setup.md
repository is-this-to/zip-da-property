# 개발 환경 세팅

[README로 돌아가기](../README.md)

## 1. 필수 도구

| 도구 | 기준 |
| --- | --- |
| JDK | 21 |
| MySQL | 8.4, 기존 `zipda_property` DB |
| Gradle | 저장소의 Wrapper 9.5.1 사용 |
| 선택 연동 | MinIO, Kafka, Kakao Local REST API, Member 서비스 |

Java 21이 설치되어 있고, 저장소에 포함된 Gradle Wrapper가 정상 실행되는지 확인한다.

```bash
java -version
./gradlew --version
```

## 2. 환경 변수

### 2.1. 로컬 개발 환경

- 로컬 실행에 필요한 환경 변수는 `.env` 파일에 설정한다.
- `.env` 파일을 설정하고, Spring Boot 애플리케이션 또는 Gradle `bootRun`을 실행할 때 환경 변수가 적용된다.
  - IntelliJ IDEA 실행 설정 예시: Run/Debug Configuration의 환경 변수 파일 항목에 `.env` 경로 지정
  - Git Bash 명령 예시: `source` 명령으로 환경 변수 파일을 불러온 뒤 실행

  ```bash
  set -a
  source /path/to/property.env
  set +a
  ./gradlew bootRun
  ```

- 실제 비밀값은 로컬 `.env`와 배포 Secret에만 보관하고 문서·Git·로그에는 기록하지 않는다.

```dotenv
APP_PORT=8082
APP_DESCRIPTION="ZIPDA Property local"
GATEWAY_URI=http://localhost:8080

DB_HOST=127.0.0.1
DB_PORT=3306
DB_NAME=zipda_property
DB_USER=zipda_local
DB_PASSWORD=[REDACTED_SECRET]

KAFKA_BOOTSTRAP_SERVERS=localhost:9092

PROPERTY_LIST_CURSOR_SECRET=[REDACTED_SECRET]
PROPERTY_PUBLIC_LOCATION_SECRET=[REDACTED_SECRET]

MINIO_ENDPOINT=http://localhost:9000
MINIO_BUCKET=zipda-local
MINIO_ACCESS_KEY=[REDACTED_SECRET]
MINIO_SECRET_KEY=[REDACTED_SECRET]
MINIO_IMAGE_PATH=property-images

KAKAO_LOCAL_REST_API_KEY=[REDACTED_SECRET]
```

- `PROPERTY_PUBLIC_LOCATION_SECRET`은 공개 위치를 생성하는 32바이트 이상 비밀값이다.
- `PROPERTY_LIST_CURSOR_SECRET`은 목록 조회 커서의 위·변조 방지에 사용한다.
- `application.yaml`은 기본 설정을 읽고, `${변수명}` 형식으로 주입된 환경 변수를 참조한다.

### 2.2. 운영 환경 참고

- 운영에서는 중요정보를 Kubernetes `Secret`, 일반 설정을 `ConfigMap`으로 관리하고, `Deployment`가 이를 환경 변수로 주입한다.
- 운영 `Deployment`에서 `SPRING_PROFILES_ACTIVE=prod`를 설정하면 `application-prod.yaml` 설정이 적용된다.
- 운영 profile에서는 Stack Trace·오류 메시지·SQL 출력이 비활성화되고 로그 수준이 축소된다.

```text
Kubernetes Secret / ConfigMap
    → Deployment 환경 변수 주입
    → application.yaml의 ${변수명} 참조
    → Spring Boot 설정 적용
```

## 3. DB 생성 및 스키마 적용

- `spring.jpa.hibernate.ddl-auto=none`이므로 애플리케이션이 DB나 테이블을 자동 생성하지 않는다.
- `spring.sql.init.mode=never`이므로 애플리케이션 실행 시 스키마와 초기 데이터를 적용하지 않는다.

### 3.1. DB 생성

MySQL 8.4 기준 예시이다. DB 이름은 `DB_NAME`과 동일하게 맞춘다.

```sql
CREATE DATABASE IF NOT EXISTS zipda_property
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;
```

### 3.2. 스키마 SQL 실행

Property DB 구조 정의 SQL을 `zipda_property`에 실행한다. SQL 파일 경로는 실제 제공받은 절대 경로로 지정한다.

```powershell
# 실제 제공받은 절대 경로로 변경
$schemaPath = 'E:/path/to/zipda-property-schema.sql'

mysql `
  --host=127.0.0.1 `
  --port=3306 `
  --user=root `
  --password `
  --database=zipda_property `
  --execute="source $schemaPath"
```

## 4. Swagger/OpenAPI 설정

### 4.1. 로컬 개발 환경

- Property 서비스는 OpenAPI JSON을 `http://localhost:<APP_PORT>/api-docs`에서 제공한다.
- 자체 Swagger UI는 기본적으로 비활성화되어 있다.
- 로컬에서 Swagger UI가 필요하면 실행 환경에 다음 값을 추가한다.

```dotenv
SPRINGDOC_SWAGGER_UI_ENABLED=true
```

- 로컬 Swagger UI: `http://localhost:<APP_PORT>/swagger-ui/index.html`
- 보호 API의 인증·권한 흐름은 Gateway를 거쳐 별도로 확인한다.

### 4.2. 운영 환경

- Property 서비스의 자체 Swagger UI는 비활성화하고 `/api-docs`만 제공한다.
- Gateway(SCG 서비스)가 Member와 Property 서비스의 OpenAPI JSON을 모아 통합 Swagger UI를 제공한다.

```text
https://zipda-be.meerkat.p-e.kr/docs
    ├─ /member-service/api-docs   → Member /api-docs
    └─ /property-service/api-docs → Property /api-docs
```

## 5. 검증

```bash
# 단위/통합 테스트 소스가 있을 때 실행
.\gradlew.bat test

# 실행 가능한 JAR 생성
.\gradlew.bat clean bootJar
```

정상 기준은 다음과 같다.

- 테스트: 실패한 테스트 없이 `BUILD SUCCESSFUL`이 표시된다.
- 테스트 결과: `build/reports/tests/test/index.html`에서 실행 결과를 확인할 수 있다.
- JAR 생성: `BUILD SUCCESSFUL`이 표시되고 `build/libs`에 실행 가능한 JAR 파일이 생성된다.
