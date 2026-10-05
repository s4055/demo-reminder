# demo-reminder

Apple Reminders의 핵심 사용 경험(리스트, 리마인더, 완료 처리, 스마트 리스트)을 웹에서 재현한 데모 프로젝트입니다.
이메일/비밀번호로 가입·로그인하며, 사용자마다 자기 리스트와 리마인더만 볼 수 있습니다. Spring Boot REST API와 Next.js 프론트엔드로 구성됩니다.

## 주요 기능

- 리스트 생성 / 이름·색상 수정 / 삭제(소속 리마인더 함께 삭제), 사이드바에 미완료 개수 뱃지 표시
- 리마인더 생성 / 수정(제목·메모·마감일시·플래그) / 완료 토글 / 삭제
- 스마트 리스트: 오늘, 예정됨, 전체, 플래그 지정됨, 완료됨
- 모바일 화면에서는 사이드바가 슬라이드 메뉴로 전환
- 회원가입 / 로그인 / 로그아웃 (세션 쿠키), 사용자별 데이터 분리

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

### 백엔드 (`:8080`)

```bash
cd backend
./gradlew bootRun        # Windows: gradlew.bat bootRun
```

- 데이터는 H2 파일 DB `backend/data/reminderdb.mv.db`에 저장되어 서버를 재시작해도 유지됩니다 (Git에는 올라가지 않습니다).
- 데이터를 초기화하려면 서버를 끄고 `backend/data/` 폴더를 지운 뒤 다시 실행합니다. 스키마는 `ddl-auto: update`로 관리하므로, 엔티티 변경이 기존 데이터와 맞지 않아 기동에 실패할 때도 같은 방법으로 초기화합니다.
- 테스트는 파일 DB를 건드리지 않고 in-memory DB(`jdbc:h2:mem:reminderdb`)를 사용합니다.
- H2 콘솔: http://localhost:8080/h2-console (JDBC URL `jdbc:h2:file:./data/reminderdb`, 사용자 `sa`, 비밀번호 없음)
- 회원가입·로그인·로그아웃을 제외한 모든 API는 로그인(세션 쿠키 `JSESSIONID`)이 필요합니다.
- CORS는 `http://localhost:3000`만 허용하며, 쿠키를 보낼 수 있도록 `allowCredentials`를 켭니다.

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
