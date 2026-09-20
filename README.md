# demo-reminder

Apple Reminders의 핵심 사용 경험(리스트, 리마인더, 완료 처리, 스마트 리스트)을 웹에서 재현한 데모 프로젝트입니다.
인증 없이 단일 사용자 기준으로 동작하며, Spring Boot REST API와 Next.js 프론트엔드로 구성됩니다.

## 주요 기능

- 리스트 생성 / 이름·색상 수정 / 삭제(소속 리마인더 함께 삭제), 사이드바에 미완료 개수 뱃지 표시
- 리마인더 생성 / 수정(제목·메모·마감일시·플래그) / 완료 토글 / 삭제
- 스마트 리스트: 오늘, 예정됨, 전체, 플래그 지정됨, 완료됨
- 모바일 화면에서는 사이드바가 슬라이드 메뉴로 전환

## 기술 스택

| 구분 | 스택 |
|---|---|
| 백엔드 | Java 21, Spring Boot 4, Spring Data JPA, H2(in-memory), Lombok |
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

- H2 in-memory DB를 사용하므로 서버를 재시작하면 데이터가 초기화됩니다.
- H2 콘솔: http://localhost:8080/h2-console (JDBC URL `jdbc:h2:mem:reminderdb`, 사용자 `sa`, 비밀번호 없음)
- CORS는 `http://localhost:3000`만 허용합니다.

### 프론트엔드 (`:3000`)

```bash
cd frontend
npm install
npm run dev
```

http://localhost:3000 에서 확인할 수 있습니다. 백엔드 주소가 다르면 `NEXT_PUBLIC_API_BASE_URL`
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
