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
| 백엔드 | Java 21, Spring Boot 4, Spring Data JPA, Spring Security, H2(파일 모드), Lombok |
| 프론트엔드 | Next.js(App Router), TypeScript, Tailwind CSS, shadcn/ui, TanStack Query, React Hook Form, date-fns |

## 사전 준비

- JDK 21
- Node.js 24 (`frontend/package.json`의 Volta 설정 기준)

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
  해당 사용자의 리프레시 토큰을 모두 폐기하므로 다시 로그인해야 합니다. 만료된 토큰은 로그인할 때 정리합니다.
- 서명 키는 `app.jwt.secret`(32바이트 이상)이며 로컬 전용 기본값이 있습니다. 운영에서는 `JWT_SECRET` 환경 변수로 주입하고,
  HTTPS 환경이면 `JWT_COOKIE_SECURE=true`로 리프레시 쿠키에 `Secure`를 붙입니다.
- CORS는 `http://localhost:3000`만 허용하며, 리프레시 토큰 쿠키를 보낼 수 있도록 `allowCredentials`를 켭니다.

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

### CI

`master`로 향하는 PR과 `master` 푸시마다 GitHub Actions(`.github/workflows/ci.yml`)가 위 검사를 자동으로 실행합니다.

- **Backend test**: JDK 21(Temurin)에서 `./gradlew test`, 실패하면 테스트 리포트를 아티팩트(`backend-test-report`)로 업로드
- **Frontend lint & build**: `package.json`의 Volta Node 버전에서 `npm ci` → `npm run lint` → `npm run build`(타입 검사 포함)

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
