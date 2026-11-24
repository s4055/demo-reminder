# demo-reminder

[![CI](https://github.com/s4055/demo-reminder/actions/workflows/ci.yml/badge.svg)](https://github.com/s4055/demo-reminder/actions/workflows/ci.yml)

Apple Reminders의 핵심 사용 경험(리스트, 리마인더, 완료 처리, 스마트 리스트)을 웹에서 재현한 데모 프로젝트입니다.
이메일/비밀번호로 가입·로그인하며, 사용자마다 자기 리스트와 리마인더만 볼 수 있습니다. Spring Boot REST API와 Next.js 프론트엔드로 구성됩니다.

## 주요 기능

- 리스트 생성 / 이름·색상 수정 / 삭제(소속 리마인더 함께 삭제), 사이드바에 미완료 개수 뱃지 표시
- 리마인더 생성 / 수정(제목·메모·마감일시·플래그) / 완료 토글 / 삭제
- 스마트 리스트: 오늘, 예정됨, 전체, 플래그 지정됨, 완료됨
- 모바일 화면에서는 사이드바가 슬라이드 메뉴로 전환
- 회원가입 / 로그인 / 로그아웃 (JWT 액세스 토큰 + 리프레시 토큰 쿠키), 사용자별 데이터 분리

## 기술 스택

| 구분 | 스택 |
|---|---|
| 백엔드 | Java 21, Spring Boot 4, Spring Data JPA, Spring Security, H2(파일 모드), Lombok, Actuator·Micrometer(Prometheus) |
| 프론트엔드 | Next.js(App Router), TypeScript, Tailwind CSS, shadcn/ui, TanStack Query, React Hook Form, date-fns |

## 사전 준비

- JDK 21
- Node.js 24 (`frontend/package.json`, `e2e/package.json`의 Volta 설정 기준)

## 실행 방법

백엔드와 프론트엔드를 각각 실행합니다.

| 구분 | 접속 주소 | 비고 |
|---|---|---|
| 프론트엔드 | http://localhost:3000 | 처음 접속하면 `/login`으로 이동 |
| H2 콘솔 | http://localhost:8080/h2-console | JDBC URL `jdbc:h2:file:./data/reminderdb`, 사용자 `sa`, 비밀번호 없음 |

### 백엔드 (`:8080`)

```bash
cd backend
./gradlew bootRun        # Windows: gradlew.bat bootRun
```

- 데이터는 H2 파일 DB `backend/data/reminderdb.mv.db`에 저장되어 서버를 재시작해도 유지됩니다 (Git에는 올라가지 않습니다).
- 데이터를 초기화하려면 서버를 끄고 `backend/data/` 폴더를 지운 뒤 다시 실행합니다. 스키마는 `ddl-auto: update`로 관리하므로, 엔티티 변경이 기존 데이터와 맞지 않아 기동에 실패할 때도 같은 방법으로 초기화합니다.
  - Phase 34(시간대 처리)에서 시각 컬럼이 `Instant`로 바뀌고 리마인더에 `timeZone` 컬럼이 추가되었습니다. 이전 버전에서 만든 로컬 DB가 있다면 `backend/data/`를 지우고 다시 실행해 새 스키마로 만듭니다.
- 서버 기본 시간대와 관계없이 동작합니다. 시각은 UTC 기준 `Instant`로 저장·응답하고, "오늘"과 반복 일정은 브라우저가 보내는 시간대로 계산합니다(`-Duser.timezone=UTC`로 실행해도 같은 결과).
- 테스트는 파일 DB를 건드리지 않고 in-memory DB(`jdbc:h2:mem:reminderdb`)를 사용합니다.
- H2 콘솔: http://localhost:8080/h2-console (JDBC URL `jdbc:h2:file:./data/reminderdb`, 사용자 `sa`, 비밀번호 없음)
- 인증은 서버 세션 없이 JWT로 합니다. 로그인·회원가입 응답의 액세스 토큰(수명 15분)을 `Authorization: Bearer` 헤더로 보내고,
  리프레시 토큰(수명 14일)은 `refresh_token` 쿠키(`HttpOnly`, `SameSite=Lax`, `Path=/api/auth`)로 받아 `POST /api/auth/refresh`로 액세스 토큰을 재발급합니다.
  회원가입·로그인·재발급·로그아웃을 제외한 모든 API는 유효한 액세스 토큰이 필요합니다.
- 리프레시 토큰은 DB에 SHA-256 해시로만 저장하고 재발급할 때마다 교체합니다. 이미 교체·폐기된 토큰이 다시 쓰이면 탈취로 보고
  해당 사용자의 리프레시 토큰을 모두 폐기하므로 다시 로그인해야 합니다. 만료된 토큰은 로그인과 재발급 때 정리합니다.
- 서명 키는 `app.jwt.secret`(32바이트 이상)이며 로컬 전용 기본값이 있습니다. 운영에서는 `JWT_SECRET` 환경 변수로 주입하고,
  HTTPS 환경이면 `JWT_COOKIE_SECURE=true`로 리프레시 쿠키에 `Secure`를 붙입니다.
- 로그인 시도 제한: 15분 안에 같은 이메일로 5번, 같은 IP에서 20번 실패하면 15분 동안 로그인을 막고 429와 `Retry-After` 헤더로 응답합니다.
  기준값은 `app.auth.login-attempt`(`max-failures-per-email`, `max-failures-per-ip`, `window`, `lock-duration`)로 바꿀 수 있습니다.
- 로그인 시도 기록 저장소는 `app.auth.login-attempt.store`(환경 변수 `LOGIN_ATTEMPT_STORE`)로 고릅니다.
  - `memory`(기본값): 서버 메모리에 두므로 Redis 없이 실행되지만, 재시작하면 초기화되고 서버를 여러 대 띄우면 서버끼리 공유하지 않습니다.
  - `redis`: 모든 서버가 같은 Redis(`REDIS_HOST`, `REDIS_PORT`, 기본 `localhost:6379`)에 기록해 실패 횟수와 차단을 공유합니다. 만료는 Redis TTL로 처리합니다.
  - Redis에 연결할 수 없으면 로그인 가용성을 우선해 차단하지 않고(fail-open) ERROR 로그를 남깁니다.
- CORS는 `http://localhost:3000`만 허용하며, 리프레시 토큰 쿠키를 보낼 수 있도록 `allowCredentials`를 켭니다.

### 상태 확인과 메트릭 (management 포트 `:8090`)

Actuator는 앱 포트(`:8080`)와 분리된 management 포트(`MANAGEMENT_PORT`, 기본 `8090`)에서만 응답합니다. 앱 포트의 `/actuator/**`는 404입니다.

| 엔드포인트 | 내용 |
|---|---|
| `GET :8090/actuator/health` | 전체 상태와 구성 요소(`db`, `diskSpace` 등, `store=redis`이면 `redis`)별 상태. 연결 정보 같은 세부 값은 보여 주지 않음 |
| `GET :8090/actuator/health/liveness` | liveness 프로브 |
| `GET :8090/actuator/health/readiness` | readiness 프로브 (DB 포함, Redis는 fail-open이라 제외) |
| `GET :8090/actuator/info` | 애플리케이션 이름과 빌드 버전 |
| `GET :8090/actuator/prometheus` | Prometheus 형식 메트릭 (HTTP 요청 히스토그램, JVM, HikariCP, 프로세스). 모든 메트릭에 `application="reminder"` 태그 |

- 이 세 엔드포인트 외에는 노출하지 않으며(`env`, `beans` 등은 404), 인증 없이 응답합니다. **운영에서는 management 포트를 외부에 열지 않고** 내부 네트워크의 수집기·프로브만 접근하게 한다는 전제입니다.
- Actuator 요청은 요청 로그(`HttpLoggingFilter`)에 남지 않습니다.

인증·보안 메트릭 (모두 카운터이며, 서버를 띄운 직후부터 0으로 노출됩니다):

| 메트릭 (Prometheus 이름) | 태그 | 늘어나는 때 |
|---|---|---|
| `reminder_auth_login_failures_total` | - | 이메일·비밀번호가 맞지 않아 로그인에 실패했을 때 |
| `reminder_auth_login_locks_total` | `scope`(`email`, `ip`) | 로그인 차단이 시작될 때 (서버 여러 대여도 한 번) |
| `reminder_auth_login_rejected_total` | - | 차단 중이라 429로 거부했을 때 |
| `reminder_auth_refresh_reuse_total` | - | 폐기된 리프레시 토큰의 재사용을 감지했을 때 |
| `reminder_auth_login_attempt_store_errors_total` | - | Redis 오류로 차단 확인 없이 로그인을 통과시켰을 때 (fail-open) |

태그에는 이메일, IP 같은 값을 넣지 않습니다.

### 모니터링 스택 (Prometheus `:9090`, Grafana `:3001`, Loki `:3100`, Alloy `:12347`)

Prometheus, Grafana, Loki, Alloy는 `docker-compose.yml`의 `monitoring` 프로필로 따로 띄웁니다(Docker 필요). 스택이 없어도 앱, 테스트, E2E는 그대로 동작합니다.

```bash
docker compose --profile monitoring up -d
docker compose --profile monitoring down
```

| 주소 | 내용 |
|---|---|
| http://localhost:9090/targets | 수집 대상. 호스트에서 실행한 백엔드(`host.docker.internal:8090`)를 15초마다 수집하며 `reminder-backend`가 UP이어야 합니다 |
| http://localhost:9090/alerts | 알림 규칙과 상태(inactive, pending, firing) |
| http://localhost:3001 | Grafana. 로그인 없이 보기 권한으로 열리며, `Reminder` 폴더에 대시보드가 있습니다 |
| http://localhost:12347/collect | Alloy `faro.receiver`. 브라우저가 보낸 프론트엔드 이벤트를 받아 Loki에 저장합니다 |
| http://localhost:3100/ready | Loki 준비 상태 |

- 설정은 저장소의 `monitoring/`에서 관리합니다. Prometheus 설정(`prometheus/prometheus.yml`), 알림 규칙(`prometheus/rules/`),
  Grafana 데이터 소스·대시보드 프로비저닝(`grafana/provisioning/`), 대시보드 JSON(`grafana/dashboards/`),
  Alloy 설정(`alloy/config.alloy`), Loki 설정(`loki/loki.yml`)과 Loki 알림 규칙(`loki/rules/fake/`)이 있습니다.
- 프로비저닝한 대시보드는 Grafana 화면에서 저장할 수 없습니다. 바꾸려면 `grafana/dashboards/`의 JSON을 고친 뒤 Grafana를 다시 띄웁니다.
- 백엔드는 호스트에서 실행하므로 Linux에서도 `host.docker.internal`이 동작하도록 `extra_hosts: host-gateway`를 둡니다.
- Grafana는 로컬 확인용 설정(익명 보기 허용)이며, 운영 배포 구성은 범위 밖입니다.

| 대시보드 | 내용 |
|---|---|
| 백엔드 개요 | 수집 상태, 요청 수, 5xx 비율, 경로별 p95 응답 시간, 상태 코드 분포 |
| JVM·DB | 힙·GC·스레드, HikariCP 활성·유휴·대기 커넥션과 획득 시간 |
| 인증·보안 | 로그인 실패, 이메일·IP별 차단 시작, 차단 중 거부, 토큰 재사용, 저장소 오류 |
| 프론트엔드 | JS 오류 수·페이지별 추이·최근 오류 목록(메시지, 페이지, 브라우저), 세션 수, Web Vitals 등급 분포와 페이지별 LCP·INP·CLS p75 |

대시보드 위쪽의 `서버` 선택으로 이중화 실행 시 서버별로 볼 수 있습니다(두 번째 서버를 수집하려면 `prometheus.yml`의 대상에 `host.docker.internal:8091`을 더합니다).

#### 알림 규칙

백엔드 규칙은 `monitoring/prometheus/rules/alerts.yml`에 정의하며, 전송 채널(Alertmanager 등)은 연결하지 않습니다. 발생한 알림은 Prometheus `/alerts`와 Grafana의 알림 화면(Alerting → Alert rules)에서 확인합니다.

| 규칙 | 조건 | 심각도 |
|---|---|---|
| `BackendDown` | 백엔드 수집 실패가 1분 이상 | critical |
| `HighServerErrorRate` | 5분간 5xx 응답 비율 5% 초과 | critical |
| `HighLatencyP95` | 10분간 API 응답 시간 p95 1초 초과 | warning |
| `DbConnectionPending` | 커넥션을 기다리는 요청이 5분 이상 계속 | warning |
| `JvmHeapHigh` | 10분간 힙 사용률 90% 초과 | warning |
| `LoginLockSpike` | 10분간 로그인 차단 시작 10회 이상 | warning |
| `RefreshTokenReuseDetected` | 5분 안에 리프레시 토큰 재사용 감지 1회 이상 | warning |
| `LoginAttemptStoreFailOpen` | 5분 안에 로그인 시도 저장소 오류 1회 이상 | critical |

규칙마다 `summary`, `description`에 원인과 확인할 대시보드를 적어 둡니다. 규칙 문법 검사와 단위 테스트(`alerts.test.yml`)는 `promtool`로 실행합니다(CI의 `Monitoring config` job과 같음).

```bash
docker run --rm -v "$PWD/monitoring/prometheus:/etc/prometheus" -w /etc/prometheus --entrypoint promtool prom/prometheus:v3.16.0 check config prometheus.yml
docker run --rm -v "$PWD/monitoring/prometheus:/etc/prometheus" -w /etc/prometheus --entrypoint promtool prom/prometheus:v3.16.0 test rules rules/alerts.test.yml
```

Docker 없이 확인하려면 [Prometheus 릴리스](https://github.com/prometheus/prometheus/releases)의 `promtool`을 받아 `monitoring/prometheus`에서 같은 명령(`promtool check config prometheus.yml` 등)을 실행합니다.

프론트엔드 규칙은 Loki ruler가 평가합니다(`monitoring/loki/rules/fake/alerts.yml`, 인증을 끈 단일 테넌트라 디렉터리 이름이 `fake`입니다).
Grafana 알림 화면에서 데이터 소스 관리 규칙(`Loki`)으로 보이며, Loki의 `http://localhost:3100/prometheus/api/v1/rules`에서도 상태를 확인할 수 있습니다.

| 규칙 | 조건 | 심각도 |
|---|---|---|
| `FrontendErrorSpike` | 5분간 프론트엔드 JS 오류 20건 이상 | warning |

#### 프론트엔드 수집 (Grafana Faro)

프론트엔드는 [Grafana Faro Web SDK](https://grafana.com/oss/faro/)로 화면 오류와 사용자 체감 성능을 Alloy(`:12347`)에 보냅니다.
수집 주소를 설정하지 않으면 SDK를 켜지 않으며 수집 요청도 나가지 않습니다(CI와 E2E는 설정하지 않습니다).

```bash
# frontend/.env.local
NEXT_PUBLIC_FARO_URL=http://localhost:12347/collect
```

`NEXT_PUBLIC_*` 값은 빌드 시점에 들어가므로, 설정을 바꾼 뒤에는 `npm run dev`를 다시 띄우거나 다시 빌드합니다.
앱 이름(`reminder-frontend`)은 `NEXT_PUBLIC_FARO_APP_NAME`, 환경(기본값 `NODE_ENV`)은 `NEXT_PUBLIC_FARO_ENVIRONMENT`로 바꿀 수 있고, 버전은 `package.json`의 `version`을 씁니다.

| 수집 | 제외 |
|---|---|
| JS 오류(처리하지 않은 오류·Promise 거부, 렌더링 중 오류)의 메시지·스택 | 콘솔 로그, 사용자 정보(ID·이메일·이름) |
| Web Vitals(LCP, INP, CLS, FCP, TTFB)와 등급 | 리소스·요청 타이밍, 사용자 행동(클릭 추적), 트레이싱 |
| 세션 ID(브라우저 탭 단위 임의 값), 페이지 경로, 브라우저·OS 이름과 버전 | URL 쿼리 문자열·`#` 뒤 값, `Bearer`·JWT 형태 토큰, 이메일 형태 값 |

- 제외 항목은 `frontend/src/lib/faro.ts`의 `beforeSend`에서 보내기 전에 지우거나 `[REDACTED]`로 바꿉니다.
- 수집기는 `http://localhost:3000` 출처만 CORS로 허용하고 요청 속도를 제한합니다(초당 50건, 순간 100건).
- 이벤트는 Loki에 `app_name`, `kind`(`exception`, `measurement`, `event`) 라벨과 logfmt 본문으로 저장됩니다. Grafana Explore에서 `{app_name="reminder-frontend", kind="exception"} | logfmt`처럼 조회합니다.

### 서버 이중화 실행 (로그인 시도 기록 공유)

로컬 Redis는 저장소 루트의 `docker-compose.yml`로 띄웁니다(Docker 필요). 이미 `localhost:6379`에서 Redis가 실행 중이면 그대로 사용해도 됩니다.
H2 파일 DB는 기본적으로 한 프로세스만 열 수 있으므로, 두 서버가 같은 DB를 쓰도록 `AUTO_SERVER=TRUE`를 붙여 실행합니다.

```bash
docker compose up -d redis
cd backend && ./gradlew bootJar
LOGIN_ATTEMPT_STORE=redis java -jar build/libs/reminder-0.0.1-SNAPSHOT.jar --server.port=8080 \
  "--spring.datasource.url=jdbc:h2:file:./data/reminderdb;AUTO_SERVER=TRUE"
LOGIN_ATTEMPT_STORE=redis java -jar build/libs/reminder-0.0.1-SNAPSHOT.jar --server.port=8081 --management.server.port=8091 \
  "--spring.datasource.url=jdbc:h2:file:./data/reminderdb;AUTO_SERVER=TRUE"
```

두 번째 서버는 management 포트가 첫 번째 서버(`:8090`)와 겹치지 않도록 `--management.server.port=8091`로 띄웁니다.

두 서버는 같은 Redis에 실패 횟수와 차단을 기록하므로, 어느 서버로 요청해도 같은 한도가 적용됩니다.
Redis 저장소 테스트(`RedisLoginAttemptStoreTest`)는 Testcontainers로 Redis를 띄우므로 Docker가 없는 환경에서는 건너뜁니다(CI에서는 실행됩니다).

### 프론트엔드 (`:3000`)

```bash
cd frontend
npm install
npm run dev
```

http://localhost:3000 에서 확인할 수 있습니다. 처음에는 `/login`으로 이동하므로 회원가입 후 사용합니다. 백엔드 주소가 다르면 `NEXT_PUBLIC_API_BASE_URL`
환경 변수로 지정합니다(기본값 `http://localhost:8080`).

## 테스트 / 검사

```bash
# 백엔드 테스트 (JUnit 5 + AssertJ, 서비스 계층은 @SpringBootTest 통합 테스트)
cd backend && ./gradlew test

# 프론트엔드 린트 / 타입 검사 / 프로덕션 빌드
cd frontend && npm run lint && npx tsc --noEmit && npm run build
```

### E2E 테스트 (Playwright)

브라우저로 실제 화면을 조작해 사용자 흐름을 확인합니다. 백엔드·프론트엔드와 분리된 `e2e` 패키지이며, 시나리오는 `e2e/tests/*.spec.ts`에 있습니다.
E2E가 프론트엔드를 띄우므로 `frontend`에서도 `npm install`이 되어 있어야 합니다.

```bash
cd e2e
npm install                       # 처음 한 번
npx playwright install chromium   # 처음 한 번
npm run typecheck                 # 타입 검사
npm run e2e                       # 전체 실행 (헤드리스)
npm run e2e:essential             # 필수 테스트만
npm run e2e:supplementary         # 보조 테스트만
npm run e2e:ui                    # 디버그용 UI 모드
```

- 테스트는 **필수**(`@essential` 태그)와 **보조**(태그 없음)로 나눕니다. 필수는 실패하면 배포를 멈춰야 하는 동작으로, 서비스를 쓸 수 없게 되거나(로그인, 핵심 CRUD), 데이터가 사라지거나 덮어써지거나(삭제 범위, 동시 편집 충돌), 권한이 새는(공유 멤버 권한) 경우입니다. 그 밖의 안내 문구, 표시, 편의 기능은 보조입니다.

- 서버를 미리 띄우지 않아도 `npm run e2e`가 백엔드(`bootRun`)와 프론트엔드(`npm run dev`)를 띄우고, 끝나면 종료합니다. 이미 떠 있는 서버가 있으면 재사용합니다(CI에서는 항상 새로 띄우고 프론트엔드는 `build` 후 `start`).
- 백엔드는 인메모리 H2(`jdbc:h2:mem:e2e`)로 실행하므로 로컬 개발 DB(`backend/data/`)를 건드리지 않습니다.
  모든 테스트가 같은 IP에서 실행되므로 IP 로그인 시도 한도를 1000으로 높이고(이메일 한도 5회는 그대로), 액세스 토큰 수명을 30초로 줄여 자동 재발급을 거치게 합니다.
  이미 떠 있는 백엔드를 재사용하면 이 설정이 적용되지 않으므로, E2E 전에는 로컬 백엔드를 끄고 실행합니다.
- 브라우저는 Chromium, `ko-KR` 로케일, `Asia/Seoul` 시간대로 고정합니다. 실패하면 스크린샷과 trace를 남기며, 리포트는 `npx playwright show-report`로 봅니다.

### CI

`master`로 향하는 PR과 `master` 푸시마다 GitHub Actions(`.github/workflows/ci.yml`)가 위 검사를 자동으로 실행합니다.

- **Backend test**: JDK 21(Temurin)에서 `./gradlew test`, 성공·실패와 관계없이 테스트 리포트(HTML)와 결과(XML)를 아티팩트(`backend-test-report`)로 업로드해 건너뛴 테스트도 확인할 수 있게 함
- **Frontend lint & build**: `package.json`의 Volta Node 버전에서 `npm ci` → `npm run lint` → `npm test` → `npm run build`(타입 검사 포함)
- **E2E essential**: JDK 21과 Node에서 `frontend`·`e2e`의 `npm ci` → `npm run typecheck` → `npx playwright install --with-deps chromium` → `npm run e2e:essential`, 실패하면 리포트와 실패 화면·trace를 아티팩트(`e2e-essential-report`)로 업로드
- **E2E supplementary**: 같은 준비 후 `npm run e2e:supplementary`, 실패하면 아티팩트(`e2e-supplementary-report`)로 업로드. 두 E2E job은 병렬로 실행되며, 브랜치별로 실행할 job을 고를 수 있도록 나눠 둡니다

## API 명세

`backend/openapi.yml`(OpenAPI 3.0)에 정리되어 있습니다.

| Method | Path | 설명 |
|---|---|---|
| GET | `/api/lists` | 리스트 전체 조회(미완료 리마인더 개수 포함) |
| POST | `/api/lists` | 리스트 생성 |
| PUT | `/api/lists/{id}` | 리스트 이름/색상 수정 |
| DELETE | `/api/lists/{id}` | 리스트 삭제(소속 리마인더 함께 삭제) |
| GET | `/api/reminders?listId=` | 리마인더 조회(리스트별 필터 선택) |
| GET | `/api/reminders/smart/{view}` | 스마트 뷰 조회(`today`, `scheduled`, `all`, `flagged`, `completed`) |
| POST | `/api/reminders` | 리마인더 생성 |
| PUT | `/api/reminders/{id}` | 리마인더 수정 |
| PATCH | `/api/reminders/{id}/complete` | 완료 토글 |
| PATCH | `/api/reminders/{id}/flag` | 플래그 토글 |
| DELETE | `/api/reminders/{id}` | 리마인더 삭제 |

## 문서

- `spec.md` — 요구사항(PRD)
- `plan.md` — Phase별 개발 계획
- `tasks.md` — Phase별 체크리스트
- `CLAUDE.md` — 코딩 관례
