# 개발 계획: Apple Reminders 웹 클론 (demoreminder)

`spec.md`의 3번(범위) 이후 내용을 기준으로, **가장 단순한 기능부터 시작해서 점진적으로
기능을 추가**하는 방식으로 개발한다. 각 Phase는 그 자체로 실행 가능한 상태(빌드/구동 가능)를 유지한다.

## 기술 스택
v1(Phase 1~5)에서 시작한 스택에 이후 Phase에서 추가한 도구를 괄호 안에 도입 Phase와 함께 적는다.

### 공통 / 개발 환경
- 백엔드: `C:\project\demo-reminder\backend` (기존 Spring Boot 4 프로젝트 재사용)
- 프론트엔드: `C:\project\demo-reminder\frontend` 하위에 신규 Next.js 프로젝트 생성 (모노레포 구조)
- 로컬 포트: 백엔드 `:8080`, 프론트엔드 `:3000`
- 인증: v1은 인증 없음. Phase 12에서 세션 로그인, Phase 28에서 JWT(액세스 토큰 + 리프레시 토큰 쿠키)로 전환
- CI: GitHub Actions (Phase 14, 프론트엔드 테스트는 Phase 31, E2E는 Phase 39에서 추가, Phase 43에서 E2E job을 필수·보조로 분리)
- 저장소 구성: `backend`, `frontend`, `e2e`(Phase 43에서 `frontend` 하위에서 루트로 분리)

### 백엔드
- Spring Boot 4.1.1, Gradle(Groovy DSL), Java 21
- Spring Data JPA + Hibernate, QueryDSL
- H2: v1은 in-memory, Phase 12부터 파일 모드(`backend/data/`). 테스트와 E2E는 in-memory
- Lombok (`@Getter`, `@NoArgsConstructor(access = PROTECTED)`, `@RequiredArgsConstructor`. 엔티티는 setter 없이 생성자와 도메인 메서드로만 변경)
- Bean Validation (`spring-boot-starter-validation`)
- CORS: `WebMvcConfigurer`로 `http://localhost:3000` 허용
- Spring Security (Phase 12), JJWT (Phase 27)
- Spring Data Redis: 로그인 시도 기록 공유 저장소, 설정으로 선택 (Phase 37)
- 테스트: JUnit Jupiter + AssertJ, Spring Boot Test, Testcontainers(Redis, Phase 37)

### 프론트엔드
- Next.js latest (App Router), TypeScript
- Tailwind CSS + shadcn/ui
- 데이터 페칭: TanStack Query (React Query) — 서버 상태 캐싱/재조회에 유리, Phase 1부터 도입
- 날짜 처리: `date-fns`
- 폼: React Hook Form (Phase 4 상세 편집부터 도입)
- 드래그 정렬: `@dnd-kit` (Phase 7), 토스트: `sonner`
- 테스트: Vitest (Phase 31)

### E2E
- Playwright (Chromium), TypeScript (Phase 39에서 `frontend`에 도입, Phase 43에서 루트 `e2e` 패키지로 분리)
- 앱 코드를 import하지 않고 화면 조작과 HTTP API로만 검증, `@essential` 태그로 필수·보조 구분 (Phase 43)

---

## Phase 1 — 최소 기능: 단일 리마인더 목록 (List 개념 없음)
**목표**: 리스트 구분 없이, 하나의 목록에서 리마인더를 추가/완료/삭제할 수 있는 최소 동작 버전.

### 데이터 모델
- `Reminder`: id, title, memo, completed, createdAt (dueAt/flagged/list 없음)

### 백엔드
- `Reminder` 엔티티, `ReminderRepository`, `ReminderController` 구현
- API: `GET /api/reminders`, `POST /api/reminders`, `PATCH /api/reminders/{id}/complete`, `DELETE /api/reminders/{id}`
- CORS 설정 추가

### 프론트엔드
- Next.js 프로젝트 생성 (`create-next-app`, TypeScript, Tailwind, App Router)
- 단일 페이지(`/`): 리마인더 입력 폼 + 목록(체크박스로 완료 처리, 삭제 버튼)
- TanStack Query로 목록 조회/생성/완료/삭제 연동

### 완료 기준
- 리마인더를 추가 → 목록에 즉시 표시 → 체크 시 취소선/완료 표시 → 삭제 시 목록에서 제거

---

## Phase 2 — 리스트(List) 기능 추가
**목표**: 여러 리스트로 리마인더를 분류할 수 있게 확장.

### 데이터 모델
- `List` 엔티티 추가: id, name, color, createdAt
- `Reminder`에 `listId` FK 추가

### 백엔드
- `List` 엔티티/레포지토리/컨트롤러: `GET/POST/PUT/DELETE /api/lists`
- `GET /api/reminders?listId=` 로 리스트별 조회 지원
- 리스트 삭제 시 소속 리마인더 cascade 삭제

### 프론트엔드
- 좌측 사이드바 컴포넌트 추가 (shadcn/ui `Sidebar` 또는 커스텀)
- 리스트 생성/삭제 UI, 리스트 클릭 시 해당 리마인더만 표시
- 사이드바에 리스트별 리마인더 개수 뱃지 표시

### 완료 기준
- 리스트를 만들고 리마인더를 특정 리스트에 추가 → 사이드바에서 리스트 선택 시 해당 리마인더만 노출

---

## Phase 3 — 마감일/플래그 + 스마트 리스트
**목표**: Apple Reminders의 핵심 특징인 스마트 뷰(오늘/예정됨/플래그 지정됨/완료됨) 구현.

### 데이터 모델
- `Reminder`에 `dueAt`(LocalDateTime, nullable), `flagged`(boolean) 추가

### 백엔드
- `GET /api/reminders/smart/{view}` 구현 (`today`, `scheduled`, `all`, `flagged`, `completed`)
- `PATCH /api/reminders/{id}/flag` 추가
- 생성/수정 API에 `dueAt` 반영

### 프론트엔드
- 사이드바 상단에 스마트 리스트 메뉴(오늘/예정됨/전체/플래그 지정됨/완료됨) 추가
- 리마인더 항목에 마감일 표시 + 플래그 아이콘 토글
- 리마인더 생성/입력 폼에 날짜/시간 선택 UI(shadcn `Popover` + `Calendar`) 추가

### 완료 기준
- 오늘 날짜로 마감일을 설정한 리마인더가 "오늘" 뷰에 나타남
- 플래그 토글 시 "플래그 지정됨" 뷰에 즉시 반영

---

## Phase 4 — 리마인더 상세 편집 + 리스트 관리 고도화
**목표**: 생성 이후에도 세부 항목을 자유롭게 수정할 수 있도록 편집 경험 완성.

### 백엔드
- `PUT /api/reminders/{id}` (제목/메모/마감일/플래그 전체 수정) 완성
- `PUT /api/lists/{id}` (이름/색상 수정) 완성

### 프론트엔드
- 리마인더 클릭 시 상세/편집 모달 또는 우측 패널 (React Hook Form)
- 리스트 이름/색상 편집 UI

### 완료 기준
- 기존 리마인더의 메모/마감일/플래그를 수정하고 저장하면 목록에 즉시 반영
- 리스트 이름/색상 변경이 사이드바에 즉시 반영

---

## Phase 5 — UI/UX 폴리싱 & 안정화
**목표**: 데모로 보여줄 수 있는 수준까지 완성도 향상.

- shadcn/ui 컴포넌트로 전체 UI 일관성 확보 (버튼, 체크박스, 모달, 토스트)
- 로딩 상태 / 에러 상태 / 빈 상태(empty state) 처리
- 반응형 레이아웃(모바일에서 사이드바 토글)
- 완료된 항목 목록 정렬(완료일 최신순 등) 및 전체 QA
- README/실행 방법 정리

### 완료 기준
- spec.md의 "9. 성공 기준(Acceptance Criteria)" 항목을 모두 수동 테스트로 통과

---

## v1 추가 작업
**목표**: Phase 5 이후 v2에 들어가기 전에 백엔드 운영 기반과 완료 항목 정렬을 보강한다. (`tasks.md`의 "추가 작업")

- 요청/응답 전문 로깅 필터 `HttpLoggingFilter`: 요청 로그 → 비즈니스 로직 → 응답 로그 순서, UUID `request_id`를 MDC에 넣어 요청 처리 중 모든 로그에 출력
  - `/h2-console` 요청은 로깅 없이 통과시킨다 (본문을 미리 읽으면 H2 로그인 폼 파라미터가 비기 때문)
- 서비스 계층 예외를 `BusinessException(ResultCode)`로 통일하고 `GlobalExceptionHandler`에서 변환한다 (`ResultCode`에 HTTP 상태 포함)
- `Reminder`에 완료일시 `completedAt` 추가: 완료 시 기록, 완료 취소 시 null. "완료됨" 뷰와 프론트엔드 완료 항목을 `updatedAt` 대신 완료일시 최신순으로 정렬
- 서비스가 응답 DTO를 반환하고 `spring.jpa.open-in-view: false`로 설정해, 엔티티→DTO 변환을 트랜잭션 안에서 끝낸다

---

# v2 — 향후 고려 사항 (spec.md 10번)

v1(Phase 1~5) 이후 `spec.md`의 "10. 향후 고려 사항"을 **작업이 단순한 것부터** 순서대로 구현한다.
기존 데이터 모델에 필드 하나를 더하는 기능부터 시작해 새 엔티티, 자기 참조, 스케줄링을 거쳐
인증/멀티 유저와 공유처럼 전체 구조가 바뀌는 기능을 마지막에 둔다.

| 순서 | Phase | 기능 | 복잡도를 결정하는 요소 |
|---|---|---|---|
| 1 | Phase 6 | 우선순위 | `Reminder`에 enum 필드 하나 추가 |
| 2 | Phase 7 | 드래그앤드롭 순서 변경 | 정렬 필드 + 일괄 순서 변경 API + DnD 라이브러리 |
| 3 | Phase 8 | 태그 | 새 엔티티 + 다대다 연관관계 + 태그별 조회 |
| 4 | Phase 9 | 하위 작업(subtask) | 자기 참조 연관관계 + 완료/삭제 전파 + 중첩 UI |
| 5 | Phase 10 | 반복 리마인더 | 완료 시 다음 회차 생성 규칙 + 날짜 계산 |
| 6 | Phase 11 | 알림(브라우저) | 브라우저 권한 + 시간 기반 트리거 + 중복 알림 방지 |
| 7 | Phase 12 | 사용자 인증/멀티 유저 + 영구 저장 | 보안 설정, 모든 데이터의 소유자 분리, H2 파일 모드 |
| 8 | Phase 13 | 리스트 공유/협업 | 인증 전제 + 멤버/권한 모델 + 접근 제어 |

### 공통 원칙 (Phase 6~13)
- 각 Phase는 v1과 마찬가지로 그 자체로 빌드/실행 가능한 상태를 유지한다.
- API를 추가/변경하면 `backend/openapi.yml`을 같은 Phase 안에서 갱신하고, 기능마다 테스트(도메인 단위 테스트, 서비스 통합 테스트, 컨트롤러 테스트)를 함께 작성한다.
- 응답은 `ApiResponse`로 감싸고, 오류는 `BusinessException(ResultCode)`로 던진다.
- 스마트 뷰(오늘/예정됨/전체/플래그 지정됨/완료됨)의 기존 동작은 새 기능이 추가되어도 유지한다.

---

## Phase 6 — 우선순위
**목표**: 리마인더에 우선순위(없음/낮음/보통/높음)를 지정하고 목록에서 구분해 보여준다.

### 데이터 모델
- `Reminder`에 `priority`(enum `Priority`: `NONE`, `LOW`, `MEDIUM`, `HIGH`, 기본값 `NONE`) 추가

### 백엔드
- 생성/수정 요청 DTO와 응답 DTO에 `priority` 반영 (생략 시 `NONE`)
- `Reminder.update(...)`에 우선순위 변경 포함

### 프론트엔드
- 리마인더 상세 편집 모달에 우선순위 선택 UI (shadcn `Select`)
- 리마인더 항목 제목 앞에 Apple Reminders처럼 `!` / `!!` / `!!!` 표시

### 완료 기준
- 우선순위를 지정/변경하면 목록의 표시가 즉시 바뀌고, 새로고침 후에도 유지된다.

---

## Phase 7 — 드래그앤드롭 순서 변경
**목표**: 사이드바의 리스트 순서와 리스트 안의 리마인더 순서를 드래그로 바꾼다.

### 데이터 모델
- `ReminderList`, `Reminder`에 `sortOrder`(int) 추가
- 새로 만드는 리스트/리마인더는 같은 범위(리스트 전체 / 같은 리스트 안)의 마지막 순서로 들어간다.

### 백엔드
- `PATCH /api/lists/order` — 요청 본문 `{ "ids": [...] }` 순서대로 리스트의 `sortOrder` 일괄 갱신
- `PATCH /api/reminders/order` — 요청 본문 `{ "listId": ..., "ids": [...] }` 순서대로 해당 리스트 리마인더의 `sortOrder` 일괄 갱신
  - `ids`가 대상 범위의 항목과 정확히 일치하지 않으면 400
- `GET /api/lists`, `GET /api/reminders?listId=`의 정렬 기준을 `sortOrder`로 변경 (스마트 뷰의 정렬은 그대로 유지)

### 프론트엔드
- `@dnd-kit/core` + `@dnd-kit/sortable` 도입
- 사이드바 리스트 항목, 사용자 리스트 화면의 미완료 리마인더를 드래그로 정렬
- 드롭 즉시 화면 순서를 바꾸고(optimistic update) 실패하면 원래 순서로 되돌린 뒤 토스트 표시
- 스마트 뷰에서는 드래그를 비활성화

### 완료 기준
- 리스트/리마인더 순서를 바꾸면 새로고침 후에도 바뀐 순서가 유지된다.

---

## Phase 8 — 태그
**목표**: 리마인더에 여러 개의 태그를 붙이고, 태그별로 모아 본다.

### 데이터 모델
- `Tag` 엔티티 추가: id, name(고유), createdAt
- `Reminder` N --- M `Tag` (조인 테이블 `reminder_tag`) — 이후 연결 엔티티 `ReminderTag`(`reminder_tag`, `(reminder_id, tag_id)` 유니크)로 전환 (PR #34)

### 백엔드
- 리마인더 생성/수정 요청에 `tagNames: string[]` 추가 — 없는 태그는 자동 생성, 응답에 태그 이름 목록 포함
- `GET /api/tags` — 태그 목록 (미완료 리마인더 개수 포함)
- `DELETE /api/tags/{id}` — 태그 삭제 (리마인더에서도 떼어냄, 리마인더 자체는 유지)
- `GET /api/reminders?tag={name}` — 태그별 리마인더 조회
- 어떤 리마인더에도 붙어 있지 않은 태그는 목록에서 제외한다.

### 프론트엔드
- 상세 편집 모달에 태그 입력 UI (입력 후 Enter로 칩 추가, 칩의 X로 제거)
- 리마인더 항목에 `#태그` 표시
- 사이드바에 "태그" 섹션 추가, 태그 클릭 시 해당 태그의 리마인더 표시

### 완료 기준
- 리마인더에 태그를 붙이면 사이드바 태그 목록에 나타나고, 태그를 선택하면 해당 리마인더만 보인다.

---

## Phase 9 — 하위 작업(subtask)
**목표**: 리마인더 아래에 하위 작업을 추가해 큰 일을 나눠 관리한다.

### 데이터 모델
- `Reminder`에 `parent`(자기 참조 `@ManyToOne`, nullable) 추가
- 깊이는 1단계로 제한한다 (하위 작업의 하위 작업은 만들 수 없음).
- 하위 작업은 부모와 같은 리스트에 속한다.

### 백엔드
- 생성 요청에 `parentId` 추가 — 부모가 없으면 404, 부모가 이미 하위 작업이면 400
- 리스트별 조회(`GET /api/reminders?listId=`)는 최상위 리마인더만 반환하고, 응답에 `subtasks` 목록을 포함한다.
- 스마트 뷰는 하위 작업도 개별 항목으로 조회 대상에 포함한다 (Apple Reminders와 동일).
- 부모 완료 시 하위 작업도 모두 완료 처리, 부모 삭제 시 하위 작업도 함께 삭제
- 리스트의 리마인더 개수에는 최상위 리마인더만 센다.

### 프론트엔드
- 리마인더 항목 아래에 하위 작업을 들여쓰기로 표시, 펼치기/접기 토글
- 상세 편집 모달에서 하위 작업 추가
- 하위 작업도 완료 체크/삭제/편집 가능

### 완료 기준
- 하위 작업을 추가하면 부모 아래에 표시되고, 부모를 완료하면 하위 작업도 함께 완료된다.

---

## Phase 10 — 반복 리마인더
**목표**: 매일/매주/매월/매년 반복되는 리마인더를 지원한다.

### 데이터 모델
- `Reminder`에 `repeatRule`(enum `RepeatRule`: `NONE`, `DAILY`, `WEEKLY`, `MONTHLY`, `YEARLY`, 기본값 `NONE`) 추가
- 반복은 마감일시(`dueAt`)가 있을 때만 설정할 수 있다 (없으면 400).

### 백엔드
- 생성/수정 요청과 응답에 `repeatRule` 반영
- 반복 리마인더를 완료하면 현재 항목은 완료 처리하고, 다음 마감일시로 같은 내용(제목/메모/플래그/우선순위/태그/리스트)의 새 리마인더를 생성
  - 다음 마감일 계산은 도메인 로직으로 분리 (월말(31일) → 다음 달 말일 처리 포함)
- 완료 취소 시에는 이미 생성된 다음 회차를 되돌리지 않는다.

### 프론트엔드
- 상세 편집 모달과 생성 폼의 날짜 선택 UI 옆에 반복 선택 UI (`Select`)
- 리마인더 항목에 반복 아이콘과 반복 주기 표시

### 완료 기준
- 매주 반복 리마인더를 완료하면 "완료됨"에 현재 항목이 들어가고, 7일 뒤 마감일의 새 항목이 "예정됨"에 나타난다.

---

## Phase 11 — 알림(브라우저 알림)
**목표**: 마감일시가 되면 브라우저 알림으로 알려준다.

### 범위
- 웹 앱이 열려 있는 동안 브라우저 Notification API로 알림을 띄운다.
- 탭이 닫혀 있어도 오는 Web Push(Service Worker + 푸시 구독)는 사용자 단위 구독 관리가 필요하므로 Phase 12 이후 별도로 검토한다.

### 백엔드
- `GET /api/reminders/upcoming?from=&to=` — 기간 안에 마감되는 미완료 리마인더 조회 (프론트 알림 스케줄링용)

### 프론트엔드
- 알림 권한 요청 UI (사이드바 또는 헤더의 알림 켜기 버튼, 거부 상태 안내)
- 주기적으로(예: 1분) 다가오는 리마인더를 조회해 마감 시각에 `new Notification(...)` 표시
- 이미 알린 리마인더 id + dueAt 조합은 `localStorage`에 기록해 중복 알림 방지 (마감일시를 바꾸면 다시 알림)
- 알림 클릭 시 해당 리마인더의 상세 편집 열기

### 완료 기준
- 1~2분 뒤로 마감일시를 설정하면 그 시각에 브라우저 알림이 한 번만 뜬다.

---

## Phase 12 — 사용자 인증/멀티 유저 + 영구 저장(H2 파일 모드)
**목표**: 여러 사용자가 각자의 리스트/리마인더를 가지도록 하고, 데이터를 영구 저장한다.

### 데이터 모델
- `User` 엔티티 추가: id, email(고유), password(BCrypt 해시), name, createdAt
- `ReminderList`, `Reminder`, `Tag`에 소유자 `user` FK 추가 (리스트 없이 만든 리마인더도 소유자를 구분해야 하므로 `Reminder`에도 둔다)
- 태그 이름의 고유 조건을 "사용자별 고유"로 변경

### 백엔드
- `spring-boot-starter-security` 도입, 세션(쿠키) 기반 로그인
- `POST /api/auth/signup`, `POST /api/auth/login`, `POST /api/auth/logout`, `GET /api/auth/me`
- `/api/auth/**`를 제외한 모든 API는 로그인 필요 — 미인증 401, 응답은 `ApiResponse` 형식 유지 (`ResultCode`에 `UNAUTHORIZED` 추가)
- 모든 조회/수정/삭제를 현재 사용자의 데이터로 제한 — 다른 사용자의 리소스는 404로 응답해 존재 여부를 노출하지 않음
- CORS에 `allowCredentials` 설정 (또는 Next.js rewrites로 같은 출처 프록시)
- 세션 쿠키에 `SameSite=Lax`, `HttpOnly`를 명시한다 (CSRF 토큰을 쓰지 않는 전제)
- 영구 저장: H2를 in-memory에서 파일 모드(`jdbc:h2:file:./data/reminderdb`)로 전환
  - 데모 프로젝트라 별도 DB 서버(PostgreSQL + docker-compose)를 둘 근거가 없어 H2를 유지한다.
  - 스키마는 기존대로 `ddl-auto: update`로 관리한다 (호환되지 않는 변경은 DB 파일을 지우고 다시 시작).
  - 테스트는 기존처럼 in-memory DB를 사용한다.
  - DB 파일(`backend/data/`)은 Git에 올리지 않는다. `README.md`에 DB 파일 위치와 초기화 방법을 적는다.

### 프론트엔드
- `/login`, `/signup` 페이지 (React Hook Form + 검증 메시지)
- 로그인하지 않은 상태로 메인 화면 접근 시 `/login`으로 이동
- API 클라이언트: `credentials: "include"`, 401 응답 시 로그인 페이지로 이동
- 헤더/사이드바에 사용자 이름과 로그아웃 버튼

### 완료 기준
- 두 계정으로 각각 로그인했을 때 서로의 리스트/리마인더가 보이지 않는다.
- 서버를 재시작해도 데이터(계정 포함)가 유지된다.

---

## Phase 13 — 리스트 공유/협업
**목표**: 리스트를 다른 사용자와 공유해 함께 리마인더를 관리한다.

### 데이터 모델
- `ListMember` 엔티티 추가: id, list, user, role(enum: `OWNER`, `EDITOR`), createdAt
- 리스트 소유자는 `OWNER` 멤버로 함께 저장한다.

### 백엔드
- `GET /api/lists/{id}/members` — 멤버 목록
- `POST /api/lists/{id}/members` — 이메일로 멤버 초대 (소유자만, 없는 사용자는 404, 이미 멤버면 400)
- `DELETE /api/lists/{id}/members/{userId}` — 멤버 제거 (소유자만) / 본인 나가기
- 접근 제어: 리스트 멤버는 소속 리마인더를 조회/생성/수정/완료/삭제할 수 있고, 리스트 자체의 수정/삭제와 멤버 관리는 소유자만 가능
- `GET /api/lists`, 스마트 뷰에 공유받은 리스트와 그 리마인더를 포함

### 프론트엔드
- 리스트 메뉴에 "공유" 다이얼로그 (이메일 초대, 멤버 목록, 제거)
- 사이드바에서 공유된 리스트에 공유 아이콘 표시
- 다른 사용자의 변경을 반영하기 위해 TanStack Query `refetchInterval`/창 포커스 재조회 설정 (실시간 WebSocket은 범위 밖)

### 완료 기준
- A가 리스트에 B를 초대하면 B의 사이드바에 리스트가 나타나고, B가 추가한 리마인더가 A 화면에도 반영된다.
- 멤버가 아닌 사용자는 해당 리스트에 접근할 수 없다.

---

# v3 — 추가 개선 (spec.md 11번)

v2까지 끝난 뒤 사용 경험을 보강한다. 이후 작업을 안전하게 쌓을 수 있도록 **CI를 가장 먼저** 두고,
나머지는 v2와 같이 **작업이 단순한 것부터** 진행한다.

| 순서 | Phase | 기능 | 복잡도를 결정하는 요소 |
|---|---|---|---|
| 1 | Phase 14 | CI (GitHub Actions) | 워크플로 파일, 백엔드/프론트 빌드 환경 |
| 2 | Phase 15 | 완료 항목 숨기기/일괄 삭제 | 일괄 삭제 API 하나 + 표시 토글 |
| 3 | Phase 16 | 검색 | 검색 쿼리(제목/메모/태그) + 새 선택 상태 |
| 4 | Phase 17 | 리스트 간 이동 | 대상 리스트 권한 + 하위 작업 함께 이동 + 순서 재지정 |
| 5 | Phase 18 | 사용자 지정 반복 | 반복 모델 확장 + 요일 기반 다음 날짜 계산 |
| 6 | Phase 19 | 리스트 아이콘 + 그룹 | 새 엔티티 + 사이드바 구조 변경 |

### 공통 원칙 (Phase 14~19)
- v2 공통 원칙을 그대로 따른다 (`openapi.yml` 갱신, 테스트 함께 작성, `ApiResponse`/`BusinessException`).
- Phase 14 이후의 모든 작업은 CI가 통과한 PR로 머지한다.
- 공유 리스트(Phase 13)의 권한 규칙을 유지한다: 리마인더 조작은 멤버, 리스트 자체의 변경은 소유자.

---

## Phase 14 — CI (GitHub Actions)
**목표**: PR과 `master` 푸시마다 빌드/테스트를 자동으로 돌려, 깨진 변경이 머지되지 않게 한다.

### 구성
- `.github/workflows/ci.yml` — `pull_request`(대상 `master`), `push`(`master`) 트리거
- **backend 잡**: `actions/setup-java`(Temurin 21) + Gradle 캐시, `backend/`에서 `./gradlew test`
  - Windows에서 커밋된 `gradlew`에 실행 권한이 없으므로(`100644`) `git update-index --chmod=+x backend/gradlew`로 고친다.
  - 테스트 리포트(`build/reports/tests`)를 아티팩트로 업로드. 처음에는 실패 시에만 올렸으나, 성공한 실행에서도 건너뛴 테스트를 확인할 수 있도록 항상 올리고 XML 결과(`build/test-results/test`)도 함께 올린다 (PR #79)
- **frontend 잡**: `actions/setup-node`(Volta 설정과 같은 Node 24) + npm 캐시, `frontend/`에서 `npm ci` → `npm run lint` → `npm run build`
- 두 잡은 병렬로 실행한다. 프론트엔드 테스트는 아직 없으므로 lint와 build(타입 검사 포함)로 검증한다.

### 저장소 설정 (GitHub에서 수동)
- `master` 브랜치 보호 규칙에 두 잡을 필수 상태 검사로 지정

### 완료 기준
- PR을 올리면 두 잡이 실행되고, 테스트를 일부러 실패시키면 PR에 실패로 표시된다.
- README에 CI 상태 배지가 보인다.

---

## Phase 15 — 완료 항목 숨기기/일괄 삭제
**목표**: 완료 항목이 쌓여도 리스트를 깔끔하게 볼 수 있게 한다.

### 백엔드
- `DELETE /api/reminders/completed?listId=` — 해당 리스트의 완료된 리마인더를 모두 삭제하고 삭제 개수(`deletedCount`)를 반환
  - 완료된 최상위 리마인더는 하위 작업과 함께 삭제되고, 미완료 부모 아래의 완료된 하위 작업도 삭제한다.
  - 리스트 멤버면 실행 가능(리마인더 삭제 권한과 동일), 접근할 수 없는 리스트는 404, `listId` 누락은 400
  - 다른 리스트의 완료 항목과 미완료 항목은 건드리지 않는다.

### 프론트엔드
- 리스트 화면 헤더에 "완료된 항목 보기/숨기기" 토글 — 리스트별 설정을 `localStorage`에 저장 (기본값: 보기, 기존 동작 유지)
- "완료 항목 지우기" 버튼 — 완료 항목이 있을 때만 표시, `AlertDialog`로 개수를 보여주고 확인
- 삭제 후 리마인더/리스트 개수/태그 쿼리 무효화

### 완료 기준
- 완료 항목을 숨겼다가 다시 보일 수 있고, 새로고침해도 설정이 유지된다.
- 일괄 삭제 후 해당 리스트의 완료 항목만 사라진다.

---

## Phase 16 — 검색
**목표**: 리마인더가 많아져도 원하는 항목을 바로 찾는다.

### 백엔드
- `GET /api/reminders/search?q=` — 현재 사용자가 접근 가능한 리마인더(내 리스트 + 공유받은 리스트) 중
  제목·메모 부분 일치(대소문자 무시) 또는 태그 이름 부분 일치 항목 조회
  - 하위 작업도 개별 항목으로 포함 (스마트 뷰와 동일)
  - 정렬: 미완료 먼저(마감일 → 생성일 순), 그다음 완료 항목(완료일시 최신순)
  - `q`는 앞뒤 공백 제거 후 1~100자, 비어 있으면 400
  - LIKE 특수문자(`%`, `_`)는 이스케이프해 문자 그대로 검색

### 프론트엔드
- 사이드바 상단 검색 입력창, 입력 300ms 디바운스 후 조회
- 선택 상태에 `{ type: "search", query }` 추가, 검색어를 지우면 이전 선택으로 복귀
- 검색 결과 항목에 소속 리스트 이름/색상 표시, 결과 없을 때 빈 상태 UI
- 검색 결과에서는 드래그 정렬 비활성화

### 완료 기준
- 제목·메모·태그로 검색하면 일치하는 리마인더만 보이고, 공유받은 리스트의 항목도 검색된다.

---

## Phase 17 — 리스트 간 이동
**목표**: 만든 뒤에도 리마인더를 다른 리스트로 옮길 수 있게 한다.

### 백엔드
- `PATCH /api/reminders/{id}/list` — 요청 본문 `{ "listId": ... }`
  - 대상 리스트에 접근할 수 없으면 404 (멤버인 리스트로만 이동 가능)
  - 하위 작업은 단독으로 이동할 수 없다 → 400 (부모를 따라 이동)
  - 부모를 옮기면 하위 작업도 같은 리스트로 함께 이동
  - 이동한 리마인더는 대상 리스트의 마지막 순서가 되고, 같은 리스트로의 이동은 변경 없음
- 도메인 메서드 `Reminder.moveTo(list, sortOrder)`에서 하위 작업까지 리스트를 바꾼다.

### 프론트엔드
- 상세 편집 모달에 리스트 선택 UI(`Select`, 리스트 색상 점 표시) — 하위 작업 편집 시에는 숨김
- 이동 후 원래/대상 리스트의 리마인더 쿼리와 리스트 개수 무효화, 토스트로 "OO로 이동" 안내

### 완료 기준
- 리마인더를 다른 리스트로 옮기면 하위 작업과 함께 이동하고, 사이드바 양쪽 개수가 즉시 바뀐다.

---

## Phase 18 — 사용자 지정 반복
**목표**: 고정 주기(매일/매주/매월/매년)를 넘어 "2주마다", "평일마다" 같은 반복을 지원한다.

### 데이터 모델
- `Reminder`에 `repeatInterval`(int, 1~99, 기본 1)과 `repeatDaysOfWeek`(`Set<DayOfWeek>`, `@ElementCollection`) 추가 — 이후 엔티티 `ReminderRepeatDay`(`reminder_repeat_day`, `(reminder_id, day_of_week)` 유니크)로 전환 (PR #34)
  - `repeatDaysOfWeek`는 `WEEKLY`일 때만 지정 가능, 비어 있으면 마감일의 요일로 반복 (기존 동작)
  - 기존 데이터(컬럼 추가 전 행)는 간격 1로 동작하도록 처리
- 반복 규칙(주기 + 간격 + 요일)을 값 객체로 묶어 다음 마감일 계산을 도메인 로직에 둔다.

### 백엔드
- 생성/수정 요청과 응답에 `repeatInterval`, `repeatDaysOfWeek` 반영
  - 간격 범위를 벗어나거나 `WEEKLY`가 아닌데 요일을 지정하면 400
- 다음 마감일 계산
  - `DAILY`/`MONTHLY`/`YEARLY`: 간격만큼 더함 (월말 처리는 기존 규칙 유지)
  - `WEEKLY` + 요일: 같은 주(월요일 시작)의 다음 지정 요일, 없으면 간격만큼 뒤 주의 첫 지정 요일. 시각은 유지
- 다음 회차에 간격과 요일도 복사

### 프론트엔드
- 반복 선택에 "평일마다" 프리셋과 "사용자 지정..." 항목 추가
- 사용자 지정 다이얼로그: 간격 숫자 + 단위(일/주/개월/년) + 주 단위일 때 요일 토글
- 리마인더 항목 반복 표시를 "2주마다 월·수", "평일마다"처럼 요약

### 완료 기준
- "2주마다 월·수" 리마인더를 월요일에 완료하면 같은 주 수요일, 수요일에 완료하면 2주 뒤 월요일 회차가 생긴다.

---

## Phase 19 — 리스트 아이콘 + 그룹
**목표**: 리스트가 많아져도 사이드바를 한눈에 정리할 수 있게 한다.

### 데이터 모델
- `ReminderList`에 `icon`(String, 선택) 추가 — 정해진 아이콘 키 목록 중 하나
- `ListGroup` 엔티티 추가: id, user(소유자), name, sortOrder, createdAt
- `ReminderList`에 `group`(`@ManyToOne`, nullable) 추가 — 그룹은 소유자 기준이며, 공유받은 리스트는 그룹 없이 표시

### 백엔드
- 리스트 생성/수정 요청과 응답에 `icon` 반영 (허용 목록 밖이면 400), 응답에 `groupId` 포함
- `GET/POST/PUT/DELETE /api/list-groups` — 그룹 CRUD (자기 그룹만), 그룹 삭제 시 소속 리스트는 그룹 해제만
- `PATCH /api/list-groups/order` — 그룹 순서 일괄 변경 (`ids` 불일치 시 400)
- `PATCH /api/lists/{id}/group` — 요청 본문 `{ "groupId": ... | null }`, 리스트 소유자만 가능, 다른 사용자의 그룹은 404
- 리스트 순서(`sortOrder`)는 기존처럼 소유자 전체 기준을 유지하고, 그룹 안에서는 그 순서로 표시

### 프론트엔드
- 리스트 생성/편집 다이얼로그에 아이콘 선택 그리드 (lucide 아이콘, 리스트 색상 적용)
- 사이드바에 그룹(폴더) 표시, 펼치기/접기 상태는 `localStorage`에 저장
- 그룹 생성/이름 변경/삭제 UI, 리스트 메뉴에 "그룹으로 이동"
- 그룹 순서 드래그 정렬, 그룹 안 리스트 드래그 정렬 (그룹 간 드래그는 범위 밖)

### 완료 기준
- 리스트 아이콘을 바꾸면 사이드바에 즉시 반영된다.
- 리스트를 그룹으로 묶고 접을 수 있으며, 그룹을 삭제해도 리스트와 리마인더는 남는다.

---

# v4 — 품질 개선 (spec.md 12번)

v3까지 끝난 뒤 새 기능 대신 품질을 다듬는다. 동작이 바뀌지 않는 정리 작업부터 시작해
프론트엔드 구조 개선, 마지막으로 API 계약이 바뀌는 동시 수정 충돌 감지 순으로 진행한다.

| 순서 | Phase | 기능 | 복잡도를 결정하는 요소 |
|---|---|---|---|
| 1 | Phase 20 | 문서·의존성 정리 | 문서 갱신 + `package.json` 정리 |
| 2 | Phase 21 | 사이드바 컴포넌트 분리 | 동작을 유지한 채 파일/훅 분리 (드래그 상태 공유) |
| 3 | Phase 22 | 동시 수정 충돌 감지 | `@Version` + 요청 버전 비교 + 409 처리 UI |

### 공통 원칙 (Phase 20~22)
- v2/v3 공통 원칙을 그대로 따른다 (`openapi.yml` 갱신, 테스트 함께 작성, `ApiResponse`/`BusinessException`, CI 통과 PR로 머지).
- 리팩터링 Phase(20, 21)는 사용자에게 보이는 동작을 바꾸지 않는다.

---

## Phase 20 — 문서·의존성 정리
**목표**: 현재 코드와 어긋난 문서와 잘못 분류된 의존성을 바로잡는다.

### 작업
- `frontend/ANALYSIS.md`를 현재 기준(작성일·커밋 갱신)으로 다시 쓴다
  - 인증 페이지(`/login`, `/signup`), 선택 상태(`selection.ts`)의 smart/list/tag/search, 공유·그룹·알림 훅, 폴더 구조와 소스 규모
- `shadcn`(CLI)을 `dependencies`에서 `devDependencies`로 이동하고 `package-lock.json` 갱신
  - `globals.css`의 `@import "shadcn/tailwind.css"`는 빌드 시에만 번들되므로 devDependency로 충분하다. 그 밖에 런타임 JS에서 `shadcn`을 import하지 않는지 확인 (`cn`은 런타임 의존성 유지)

### 완료 기준
- `npm ci` → `npm run lint` → `npm run build`가 통과하고, `ANALYSIS.md`의 구조 설명이 실제 파일과 일치한다.

---

## Phase 21 — 사이드바 컴포넌트 분리
**목표**: 884줄인 `sidebar.tsx`를 역할별로 나눠 읽고 고치기 쉽게 한다. 동작은 바꾸지 않는다.

### 구성
- `components/sidebar/` 폴더로 이동하고 `index.tsx`(조립)에서 `Sidebar`를 export
  - `smart-views.tsx` — 스마트 리스트 메뉴
  - `list-section.tsx` — 그룹/리스트 영역과 `DndContext`
  - `sortable-group.tsx`, `sortable-list-item.tsx`, `list-item-content.tsx`, `move-to-group-menu.tsx`
  - `tag-section.tsx`, `sidebar-item.tsx`
- 드래그 처리(`parseDragId`, 순서 계산, optimistic update/복구)를 `use-sidebar-dnd.ts` 훅으로 분리
- 다이얼로그 열림 상태 등 여러 하위 컴포넌트가 쓰는 상태는 `index.tsx`에 두고 props로 전달

### 완료 기준
- 파일 하나가 300줄을 넘지 않는다.
- 스마트 뷰 선택, 리스트/그룹 드래그 정렬, 그룹 접기, 그룹으로 이동, 리스트 편집/공유/삭제, 태그 선택이 분리 전과 같게 동작한다.

---

## Phase 22 — 동시 수정 충돌 감지
**목표**: 공유 리스트에서 동시 편집 시 나중 저장이 앞의 변경을 조용히 덮어쓰지 않게 한다.

### 데이터 모델
- `Reminder`, `ReminderList`에 `version`(Long, `@Version`) 추가
  - 기존 행은 컬럼 추가 시 `null`이 되므로 0으로 채운다 (기존 백필 방식과 같이 기동 시 보정 또는 컬럼 기본값)

### 백엔드
- 리마인더/리스트 응답에 `version` 포함
- `PUT /api/reminders/{id}`, `PUT /api/lists/{id}` 요청에 `version` 필수(`@NotNull`)
  - 서비스에서 조회한 엔티티의 버전과 요청 버전이 다르면 `BusinessException(VERSION_CONFLICT)` → 409
  - 같은 순간의 경합으로 발생하는 `ObjectOptimisticLockingFailureException`도 `GlobalExceptionHandler`에서 409(`VERSION_CONFLICT`)로 변환
- `ResultCode.VERSION_CONFLICT`(409) 추가 — 기존 `CONFLICT`(중복)와 구분
- 완료/플래그 토글, 순서 변경, 이동, 그룹 지정은 버전을 받지 않는다 (버전은 증가)

### 프론트엔드
- `Reminder`, `ReminderList` 타입과 수정 API에 `version` 추가, 편집 시 열 때의 `version`을 함께 보낸다
- 409 `VERSION_CONFLICT` 응답 시 "다른 사용자가 먼저 수정했습니다" 토스트, 해당 쿼리를 무효화해 최신 내용으로 편집 창을 다시 채운다 (입력 중 내용은 버림)

### 완료 기준
- 두 사용자가 같은 리마인더 편집 창을 열고 A가 먼저 저장하면, B의 저장은 409로 거부되고 B 화면에 A의 변경이 반영된다.

---

# v5 — 백엔드 클린코드 (spec.md 13번)

v4까지 끝난 뒤 백엔드 코드를 정리한다. `CLAUDE.md` 관례 위반을 먼저 바로잡고, 그다음 중복 제거, 마지막으로 책임 이동과
보안 정리 순으로 진행한다. 각 Phase는 동작을 유지하는 리팩터링이며, 바뀌는 동작은 spec.md 13번의 "동작 변경"에 적은 것뿐이다.

| 순서 | Phase | 기능 | 복잡도를 결정하는 요소 |
|---|---|---|---|
| 1 | Phase 23 | 관례 정합성 | 커스텀 검증 제약, 도메인 메서드 이동, 입력 정리 위치 통일 |
| 2 | Phase 24 | 중복 제거 | 여러 서비스/DTO에 걸친 공통 코드 추출 |
| 3 | Phase 25 | 책임 위치·가독성 | 도메인으로 규칙 이동, `Clock` 주입, 이름 정리 |
| 4 | Phase 26 | 로그 민감 정보 마스킹 | 로깅 필터 마스킹 규칙 |

### 공통 원칙 (Phase 23~26)
- v2~v4 공통 원칙을 그대로 따른다 (`openapi.yml` 갱신, 테스트 함께 작성, `ApiResponse`/`BusinessException`, CI 통과 PR로 머지).
- 리팩터링 전후로 기존 테스트가 그대로 통과해야 한다. 테스트를 고쳐야 한다면 그 이유가 spec.md 13번의 "동작 변경"에 해당하는지 확인한다.

---

## Phase 23 — 관례 정합성
**목표**: `CLAUDE.md`의 입력 검증 위치, 도메인 규칙 중복 금지 규칙에 맞춘다.

### 백엔드
- 비밀번호 72바이트 검증을 웹 경계로 이동
  - `@MaxBytes` 같은 커스텀 Bean Validation 제약을 만들어 `SignupRequest.password`에 붙인다
  - `AuthService`의 바이트 검사와 `BCRYPT_MAX_BYTES`를 지우고, 흩어진 숫자 72(`@Size(max = 72)`, 오류 메시지)를 상수 하나로 모은다
- 리마인더 이동에서 서비스의 도메인 규칙 중복 제거
  - `Reminder.isIn(ReminderList)`를 추가하고, "하위 작업이면 예외 → 같은 리스트면 그대로 둠" 판단을 도메인으로 옮긴다
  - `ReminderService.moveReminder`의 `isSubtask()` 검사와 `getList().getId().equals(...)` 비교, 우회를 설명하던 주석을 지운다
- 입력 공백 정리 통일
  - 이름·제목·태그·검색어 등의 정리를 요청 DTO의 `toCommand()`(또는 컨트롤러)로 모으고 `strip()`으로 통일한다
  - 지금 정리하지 않는 리스트 이름과 리마인더 제목도 정리한다 (동작 변경)

### 완료 기준
- 72바이트를 넘는 비밀번호로 가입하면 서비스를 거치지 않고 400이 나온다.
- 하위 작업 단독 이동은 400, 같은 리스트로의 이동은 변경 없이 성공한다.
- 앞뒤 공백이 있는 리스트 이름과 리마인더 제목이 정리된 값으로 저장된다.

---

## Phase 24 — 중복 제거
**목표**: 여러 곳에 반복된 코드를 한 곳으로 모은다.

### 백엔드
- `AuthService.getUser(Authentication)`를 지우고, 로그인 응답은 `loginSession.start()` 뒤 `authService.me()`로 만든다
- 리스트 그룹 조회와 404 처리(`ListGroupService.findGroupOrThrow`, `ReminderListService.changeGroup`)를 한 곳으로 모은다
- `GlobalExceptionHandler`의 `ResponseEntity.status(...).body(ApiResponse.error(...))` 반복을 private 메서드로 추출하고, `handleUnexpected`도 `resultCode.getStatus()`를 쓰게 한다
- `ReminderService.createReminder`와 `createSubtask`의 생성·태그 지정·저장 중복을 합친다 (부모 연결과 순서 지정만 분기)
- 요청 DTO의 제약 상수 정리
  - 태그 이름 최대 길이는 `ReminderRequest`가 아닌 공통 위치(예: `Tag.NAME_MAX_LENGTH`)에 둔다
  - 리스트 생성/수정 요청에 중복된 색상 정규식과 메시지를 상수로 모은다
  - `SignupRequest`의 매직 넘버(100, 50)를 상수로 바꾼다
  - `ReminderRequest.toRecurrence()`를 `Recurrence` 쪽 팩토리로 옮겨 DTO끼리 서로 참조하지 않게 한다

### 완료 기준
- 위 코드가 각각 한 곳에만 있고, 로그인/회원가입/그룹 지정/예외 응답/리마인더·하위 작업 생성 테스트가 그대로 통과한다.

---

## Phase 25 — 책임 위치·가독성
**목표**: 규칙을 제자리로 옮기고 읽기 쉽게 다듬는다.

### 백엔드
- 완료 항목 일괄 삭제(`ReminderService.deleteCompletedReminders`)의 "최상위는 하위 작업과 함께, 하위 작업은 부모가 미완료일 때만" 판단을 `Reminder`의 의도가 드러나는 메서드로 옮긴다
- `Clock` 빈을 등록하고 `ReminderQueryService`(오늘 스마트 뷰)와 `ReminderService`(완료 토글)에서 `now()` 대신 주입한 `Clock`을 쓴다
- `ReminderListResponse.from`에서 `summary.list()`를 지역 변수로 받고, "소유자에게만 `groupId`를 보여준다" 규칙을 이름 있는 메서드로 꺼낸다
- 작은 정리
  - `list.changeGroup(null)` → `list.leaveGroup()`
  - `ListGroupRepository.findByUserIdOrderBySortOrderAscIdAsc`를 다른 리포지토리처럼 `Sort` 파라미터를 받는 방식으로 통일한다
  - `Reminder` 생성자에서 기본값과 같은 `completed = false`, `flagged = false` 대입을 지운다
  - private 메서드(`ReminderListService.toResponse`, `ReminderQueryService.escapeLike`)를 public 메서드 뒤로 옮긴다
  - `SmartView` 문자열 변환을 `Converter<String, SmartView>`로 등록하고, 컨트롤러가 `@PathVariable SmartView view`로 받게 한다 (알 수 없는 값은 계속 400)

### 완료 기준
- 고정한 `Clock`으로 "오늘" 스마트 뷰의 자정 경계와 완료 시각을 검증하는 테스트가 있다.
- 완료 항목 일괄 삭제의 삭제 개수와 결과가 이전과 같다.

---

## Phase 26 — 로그 민감 정보 마스킹
**목표**: 요청 로그에 세션을 가로챌 수 있는 값이 남지 않게 한다.

### 백엔드
- `HttpLoggingFilter`가 `Cookie`(필요하면 `Authorization`도) 헤더 값을 마스킹한다
- 세션 로그에는 세션 ID와 속성 이름만 남기고 값(`SPRING_SECURITY_CONTEXT` 등)은 남기지 않는다

### 완료 기준
- 로그인 후 요청의 로그에 `JSESSIONID` 값과 보안 컨텍스트 내용이 나오지 않는다.

---

# v6 — JWT 인증 (spec.md 14번)

세션 쿠키 인증을 JWT 액세스 토큰과 리프레시 토큰 방식으로 전환한다. 기존 세션 인증을 유지한 채 부품을 먼저 만들고,
서버와 프론트엔드를 한 번에 전환한 뒤, 재사용 감지와 정리를 마지막에 더한다.

| 순서 | Phase | 내용 |
|---|---|---|
| 1 | Phase 27 | 토큰 발급·검증 기반 (`JwtProvider`, `RefreshToken`, `RefreshTokenService`) |
| 2 | Phase 28 | 세션에서 JWT로 전환 (Bearer 인증 필터, 재발급·로그아웃 API, 프론트엔드 자동 재발급) |
| 3 | Phase 29 | 리프레시 토큰 재사용 감지·정리, 문서 |

### 공통 원칙 (Phase 27~29)
- 인증 방식 외의 기능과 API 동작은 바꾸지 않는다.
- 토큰 원문은 DB와 로그에 남기지 않는다.
- 권한은 토큰에 담지 않고 지금처럼 요청마다 DB의 멤버 정보로 확인한다. 토큰에는 사용자 ID만 담는다.
- 서버와 프론트엔드가 함께 바뀌어야 동작하는 전환은 한 Phase에서 끝낸다.
- 학습 목적에 맞게 토큰 검증 흐름이 코드로 드러나도록 JJWT와 직접 만든 인증 필터로 구현한다.

---

## Phase 27 — 토큰 발급·검증 기반
**목표**: 기존 세션 인증을 유지한 채 토큰 발급과 검증에 필요한 부품을 만든다.

### 백엔드
- JJWT 의존성을 추가한다
- `app.jwt` 설정(서명 키, 액세스/리프레시 수명, 쿠키 `secure`)을 `@ConfigurationProperties`로 받고, 서명 키가 32바이트 미만이면 시작 시 실패시킨다
- 서명 키는 `application.yml`에 로컬 전용 기본값을 두고, 운영에서는 `JWT_SECRET` 환경 변수로 주입한다
- 액세스 토큰을 발급하고 검증하는 `JwtProvider`를 만든다 (`sub`=사용자 ID, 시각은 주입한 `Clock` 기준)
- `RefreshToken` 엔티티와 도메인 메서드(`rotate`, `revoke`, 만료/폐기 판단)를 만든다
- `RefreshTokenRepository`를 `repository.user`에 추가한다
- 발급, 회전, 폐기를 담당하는 `RefreshTokenService`를 만든다 (무작위 토큰 생성, SHA-256 해시 저장, 동시 회전은 `@Version`으로 감지)

### 완료 기준
- 만료, 위조, 형식 오류 토큰이 검증에서 거부되는 테스트가 있다.
- 리프레시 토큰 원문이 DB에 저장되지 않는 테스트가 있다.
- 기존 세션 로그인과 전체 테스트가 그대로 통과한다.

---

## Phase 28 — 세션에서 JWT로 전환
**목표**: 백엔드와 프론트엔드의 인증을 JWT로 함께 전환한다.

### 백엔드
- Bearer 토큰으로 인증 정보를 설정하는 `JwtAuthenticationFilter`를 추가한다. 토큰이 유효하지 않으면 인증 없이 통과시켜 기존 `ApiAuthenticationEntryPoint`가 401을 반환하게 한다
- `SecurityConfig`를 `SessionCreationPolicy.STATELESS`로 바꾸고 `/api/auth/refresh`를 허용한다
- 필터는 DB 조회 없이 토큰의 사용자 ID로 인증 정보를 만들고, `CurrentUser` 호출부는 바꾸지 않는다
- `LoginSession`, `SecurityContextRepository`, `LogoutHandler` 빈을 제거한다
- 로그인, 회원가입, 재발급은 `AuthResponse(accessToken, user)`를 반환하고 리프레시 토큰 쿠키(`HttpOnly`, `SameSite=Lax`, `Path=/api/auth`)를 설정한다
- `POST /api/auth/refresh`를 추가하고 `logout`을 토큰 폐기와 쿠키 삭제로 바꾼다
- `HttpLoggingFilter`에서 응답 본문의 `accessToken`을 마스킹하고, 세션 로그를 제거한다
- `backend/openapi.yml`의 인증 설명과 `securitySchemes`, 인증 API를 갱신한다
- `SessionCookieTest`를 리프레시 쿠키 검증으로 교체하고 인증 관련 테스트를 갱신한다

### 프론트엔드
- `api.ts`가 메모리의 액세스 토큰을 `Authorization` 헤더로 보낸다. 리프레시 쿠키 때문에 `credentials: "include"`는 유지하고, 이를 설명하는 기존 주석은 리프레시 쿠키 기준으로 내용만 고친다
- 앱 시작 시 `/api/auth/refresh`로 로그인 상태를 복원한다
- 401 응답 시 한 번만 재발급하고 원래 요청을 다시 보낸다. 재발급이 실패하면 기존 `unauthorizedHandler`를 호출한다 (`login`, `signup`, `refresh`, `logout`은 재시도 제외)
- 동시에 발생한 401은 재발급 한 번을 공유하고, 탭 간에는 Web Locks API로 재발급을 순서대로 처리한다
- `auth-api.ts`의 응답 타입과 로그인/로그아웃 처리를 갱신한다

### 완료 기준
- 로그인 후 `JSESSIONID` 쿠키가 없고, 리프레시 쿠키에 `HttpOnly`가 설정되어 있다.
- 액세스 토큰 수명을 짧게 설정해 실행했을 때 작업 중 로그아웃되지 않는다.
- 새로고침과 여러 탭에서 로그인이 유지된다.
- 로그인과 재발급 응답 로그에 토큰 값이 없다.
- `/h2-console`이 계속 동작한다.

---

## Phase 29 — 재사용 감지·정리
**목표**: 리프레시 토큰 탈취에 대응하고 만료 토큰이 쌓이지 않게 한 뒤 문서를 마무리한다.

### 백엔드
- 폐기된 리프레시 토큰이 다시 사용되면 해당 사용자의 리프레시 토큰을 모두 폐기하고 401을 반환한다. 동시 회전 충돌도 401로 처리한다
- 로그인 시 해당 사용자의 만료된 리프레시 토큰을 삭제한다

### 문서
- `CLAUDE.md`의 인증/세션 관례를 토큰 기준으로 바꾼다 (`LoginSession` 언급 제거)
- `README.md`의 세션 쿠키 설명을 갱신한다

### 완료 기준
- 교체 전 토큰으로 재발급하면 401이 되고, 최신 토큰도 함께 무효가 되는 테스트가 있다.
- 로그인 후 해당 사용자의 만료 토큰이 삭제되는 테스트가 있다.

---

# v7 — 품질·확장성 개선 (spec.md 15번)

동작이 바뀌지 않는 정리와 테스트 기반을 먼저 마련한 뒤, API가 바뀌는 시간대 처리와 페이지네이션,
로그인 보호를 진행하고, 마지막에 백엔드 전체 리뷰에서 나온 지적 사항을 정리한다.

| 순서 | Phase | 내용 |
|---|---|---|
| 1 | Phase 30 | 코드 정리 (import 순서, `.editorconfig`) |
| 2 | Phase 31 | 프론트엔드 테스트 기반 (Vitest, CI 실행) |
| 3 | Phase 32 | 프론트엔드 컴포넌트 분리 (`reminder-list`, `reminder-edit-dialog`) |
| 4 | Phase 33 | 리마인더 단건 조회 (`GET /api/reminders/{id}`) |
| 5 | Phase 34 | 시간대 처리 (`Instant`, 리마인더 `timeZone`) |
| 6 | Phase 35 | 목록 페이지네이션 ("완료됨"과 검색의 커서 기반 조회) |
| 7 | Phase 36 | 로그인 시도 제한 |
| 8 | Phase 37 | 로그인 시도 기록 공유 (Redis, 서버 이중화 대응) |
| 9 | Phase 38 | 코드 리뷰 지적 사항 정리 |

### 공통 원칙 (Phase 30~38)
- 위험이 적은 정리와 테스트 기반을 먼저 마련한 뒤, API가 바뀌는 작업을 진행한다.
- 서버와 프론트엔드가 함께 바뀌어야 동작하는 API 변경은 한 Phase에서 끝낸다.
- API를 바꾸는 Phase는 `backend/openapi.yml`, 백엔드 테스트, 프론트엔드 테스트를 같은 Phase에서 갱신한다.
- 동작을 유지하는 Phase(30~32)는 기존 테스트가 그대로 통과하는 것으로 검증한다.

---

## Phase 30 — 코드 정리
**목표**: 파일마다 다른 형식을 하나의 규칙으로 맞추고, 이후 변경에서 다시 어긋나지 않게 한다.

### 작업
- 백엔드 import 순서를 IntelliJ 기본 배치로 통일한다 (그 밖의 import 알파벳순 → 빈 줄 → `javax`/`java` → 빈 줄 → static import)
  - 예: `ReminderService`에서 `exception`이 `common`보다 앞에 있는 것처럼 알파벳순이 깨진 파일을 고친다
- 루트에 `.editorconfig`를 추가해 들여쓰기, 줄 끝, import 배치(`ij_java_imports_layout`)를 IDE가 자동 적용하게 한다
- `ReminderService.reorderReminders`의 쉼표 뒤 공백 누락 등 형식 오류를 고친다
- `CLAUDE.md`에 import 배치 규칙과 `.editorconfig`를 따른다는 관례를 추가한다

### 완료 기준
- 백엔드 전체 테스트가 통과하고, 동작 코드 변경이 없다 (형식 변경만 포함).

---

## Phase 31 — 프론트엔드 테스트 기반
**목표**: 이후 Phase의 리팩터링과 API 변경을 검증할 수 있도록 프론트엔드 로직 테스트를 마련한다.

### 구성
- Vitest를 devDependency로 추가하고 `npm test`(`vitest run`) 스크립트를 만든다
  - 기본 환경은 `node`로 두고, `localStorage`/`window`가 필요한 테스트만 `jsdom` 환경에서 실행한다
  - `@/` 경로 별칭을 `tsconfig.json`과 같게 설정한다
- 테스트 파일은 대상 옆에 `*.test.ts`로 둔다

### 테스트 대상
- `lib/api.ts`: 성공 응답의 `data` 반환, 실패 응답의 `ApiError`(상태·`resultCode`), 401 시 재발급 후 재시도, 재발급 실패 시 `unauthorizedHandler` 호출, 동시 401의 재발급 1회 공유, 인증 API 경로는 재시도 제외 (`fetch`를 모킹)
- `lib/repeat.ts`: 반복 규칙 ↔ 요청 필드 변환, 표시 문구
- `lib/due-date.ts`: 서버 시각 문자열 파싱·표시, 지남 판단
- `lib/selection.ts`, `lib/notified-reminders.ts`

### CI·문서
- `.github/workflows/ci.yml`의 frontend job에 `npm test` 단계를 추가한다 (lint와 build 사이)
- `CLAUDE.md`에 프론트엔드 관례를 추가한다: `lib/`와 훅의 로직을 바꾸면 테스트를 함께 작성한다

### 완료 기준
- `npm test`가 로컬과 CI에서 통과한다.
- 재발급·재시도 로직의 주요 분기(성공, 재발급 성공 후 재시도, 재발급 실패, 동시 401)가 테스트로 검증된다.

---

## Phase 32 — 프론트엔드 컴포넌트 분리
**목표**: 큰 두 컴포넌트를 역할별로 나눠 Phase 34·35의 변경을 쉽게 한다. 동작은 바꾸지 않는다.

### `reminder-list.tsx` (545줄) → `components/reminder-list/`
- `index.tsx` — `ReminderList` 조립과 화면 상태(편집 대상, 일괄 삭제 다이얼로그)
- `use-reminder-list-items.ts` — 선택 상태별 조회, 완료 항목 표시 여부, 정렬(`byRecentCompletion`), 하위 작업 접기 상태
- `use-reminder-dnd.ts` — 드래그 정렬과 optimistic update/복구
- `sortable-reminder-item.tsx`, `reminder-item.tsx`, `reminder-row.tsx` — 항목 렌더링
- 여러 하위 컴포넌트가 쓰는 동작은 지금의 `ReminderActions` 타입으로 묶어 props로 전달한다

### `reminder-edit-dialog.tsx` (426줄) → `components/reminder-edit-dialog/`
- `index.tsx` — `ReminderEditDialog`
- `reminder-edit-form.tsx` — 폼, 저장, 버전 충돌 처리
- `form-values.ts` — `ReminderFormValues`, `formValuesOf`, 폼 값 → 요청 변환 (Phase 31 방식으로 테스트 추가)
- `list-select.tsx`, `subtask-section.tsx`

### 완료 기준
- `npm run lint`, `npm run build`, `npm test`가 통과한다.
- 리스트·스마트 뷰·태그·검색 화면의 목록, 드래그 정렬, 하위 작업 접기/추가, 편집·충돌 처리가 이전과 같게 동작한다.

---

## Phase 33 — 리마인더 단건 조회
**목표**: 리마인더 하나가 필요할 때 전체 목록을 받지 않게 한다.

### 백엔드
- `GET /api/reminders/{id}` 추가: `findAccessibleById`로 조회해 `ReminderResponse`(하위 작업 포함)를 반환, 없거나 접근할 수 없으면 404
- `openapi.yml` 갱신
- 테스트: 본인 리마인더, 공유 리스트 리마인더, 하위 작업 조회 성공 / 다른 사용자 리마인더 404 (서비스·컨트롤러)

### 프론트엔드
- `reminders-api.ts`에 `getReminder(id)`를 추가하고, `useFetchLatestReminder`가 이를 사용하게 바꾼다
- 테스트: `getReminder` 요청 경로

### 완료 기준
- 편집 중 버전 충돌이 나면 단건 조회 요청 하나로 최신 내용이 폼에 반영된다.

---

## Phase 34 — 시간대 처리
**목표**: 서버와 사용자의 시간대가 달라도 마감 시각, "오늘", 반복 일정이 정확하게 동작하게 한다.

### 데이터 모델
- 시각 필드를 `Instant`로 변경한다: `Reminder.dueAt`·`completedAt`, `BaseTimeEntity.createdAt`·`updatedAt`, `RefreshToken.expiresAt`·`revokedAt`
- `Reminder.timeZone`(`ZoneId`, 문자열 컬럼)을 추가한다. 마감 시각이 있는데 시간대가 없으면 `DomainException`으로 거부한다
- 컬럼 타입이 바뀌므로 로컬 H2 파일 DB(`backend/data`)를 삭제하고 다시 만든다 (`README.md`에 안내)

### 백엔드
- 시각 계산은 `Instant.now(clock)`을 사용한다
- `Recurrence.nextDueAt`이 `Instant`와 `ZoneId`를 받아, 해당 시간대의 날짜·요일 기준으로 다음 마감 시각을 계산한다 (일 단위 반복은 같은 현지 시각 유지)
- 요청 DTO: `dueAt`을 오프셋이 있는 시각으로 받고, `timeZone` 문자열을 `ZoneId`로 변환한다 (잘못된 ID는 400, 웹 경계에서 검증)
- `GET /api/reminders/smart/TODAY`는 `zone` 파라미터(`ZoneId`)를 필수로 받고, 서비스가 그 시간대의 오늘 구간을 `Instant`로 계산한다
- `GET /api/reminders/upcoming`의 `from`/`to`를 `Instant`로 받는다
- Jackson이 `Instant`를 `Z`가 붙은 ISO-8601 문자열로 직렬화하는지 확인한다
- `CLAUDE.md`에 관례를 추가한다: 시각은 `Instant`로 저장·응답하고, 날짜 경계나 반복 계산처럼 현지 시간이 필요한 경우에만 명시적인 `ZoneId`로 변환한다
- `openapi.yml`의 시각 형식, `timeZone`, `zone` 파라미터 설명을 갱신한다
- 테스트
  - 도메인: 시간대가 있는 다음 마감 시각 계산(매일·매주 요일·매월, 서머타임 전환일), 마감 시각만 있고 시간대가 없으면 거부
  - 서비스: `Clock`을 UTC로 고정하고 `zone=Asia/Seoul`로 오늘 뷰 경계 검증 (한국 0시~9시 마감 포함)
  - 컨트롤러: 잘못된 `timeZone`·`zone` 400, `TODAY`에 `zone` 누락 400, 응답 시각 형식
  - 기존 `LocalDateTime` 기반 테스트를 `Instant` 기준으로 갱신

### 프론트엔드
- `lib/timezone.ts`에 브라우저 시간대(`Intl.DateTimeFormat().resolvedOptions().timeZone`)를 구하는 함수를 둔다
- 리마인더 생성·수정 요청에 `timeZone`을 넣고, `dueAt`은 `toISOString()`으로 보낸다
- 오늘 뷰 조회에 `zone`을 붙이고, upcoming 조회의 `from`/`to`를 `toISOString()`으로 보낸다
- `lib/due-date.ts`가 오프셋이 있는 문자열을 파싱해 브라우저 시간대로 표시하도록 바꾸고, "타임존 없는 문자열" 전제를 설명하던 주석을 새 기준으로 고친다
- 테스트: 요청 시각 변환, 다른 시간대 문자열의 표시, 지남 판단

### 완료 기준
- 백엔드를 `-Duser.timezone=UTC`로 실행해도 한국 시간 오전에 마감인 리마인더가 "오늘"에 나타난다.
- 브라우저 시간대를 바꿔(개발자 도구 Sensors) 같은 리마인더를 보면 같은 순간이 각 시간대로 표시된다.
- 매일 반복 리마인더를 완료하면 다음 항목이 다음 날 같은 현지 시각으로 생긴다.
- 마감 알림이 정해진 시각에 뜬다.

---

## Phase 35 — 목록 페이지네이션
**목표**: 계속 늘어나는 "완료됨" 목록과 검색 결과를 나눠 조회한다.

### 백엔드
- 공통 응답 DTO `SliceResponse<T>(List<T> items, String nextCursor)`를 추가한다
- 조회는 `size + 1`개를 가져와 다음 페이지가 있는지 판단한다
- **완료됨**: `GET /api/reminders/completed`를 추가하고 `SmartView.COMPLETED`를 제거한다
  - 정렬 `completedAt desc, id desc`의 키셋 페이지네이션. 커서는 마지막 항목의 `(completedAt, id)`를 Base64URL로 인코딩한 값
- **검색**: `GET /api/reminders/search`의 응답을 `SliceResponse`로 바꾼다
  - 정렬이 여러 조건(완료 여부, 조건부 마감·완료 시각)으로 구성되어 키셋 조건이 복잡하므로 오프셋 방식으로 하고, 오프셋을 커서로 인코딩해 응답 형식을 맞춘다
- `size`(기본 50, 1~100)와 커서 형식 검증·디코딩은 웹 경계에서 한다 (잘못된 커서 400)
- `openapi.yml`에 새 API와 응답 형식, `COMPLETED` 스마트 뷰 제거를 반영한다
- 테스트
  - 리포지토리/서비스: 완료 시각이 같은 항목이 페이지 경계에 걸쳐도 빠짐·중복 없음, 마지막 페이지 `nextCursor` null, 검색 페이지 이어 받기
  - 컨트롤러: `size` 범위 밖 400, 잘못된 커서 400, `smart/COMPLETED` 400

### 프론트엔드
- `reminders-api.ts`에 `getCompletedReminders(cursor)`, `searchReminders(query, cursor)`를 페이지 응답 타입으로 바꾼다
- 완료됨·검색 화면은 `useInfiniteQuery`로 조회하고, 목록 끝의 감지 요소(`IntersectionObserver`)가 보이면 다음 페이지를 불러온다
  - 주기적 동기화(`SYNC_INTERVAL_MS`)는 불러온 페이지 전체를 다시 조회하는 기본 동작을 그대로 쓴다
- 선택 상태 `smart/COMPLETED`는 화면 이름으로 유지하고, 조회만 새 API로 바꾼다
- 리마인더 변경 후 캐시 무효화가 무한 조회 캐시에도 적용되는지 확인한다
- 테스트: 페이지 응답 파싱, 커서 전달

### 완료 기준
- 완료 항목 120개를 만든 뒤 "완료됨" 첫 요청은 50개만 받고, 스크롤하면 120개 모두 순서대로 중복 없이 보인다.
- 검색 결과도 같은 방식으로 이어서 불러온다.
- 완료 해제·삭제 후 목록이 올바르게 갱신된다.

---

## Phase 36 — 로그인 시도 제한
**목표**: 비밀번호 무차별 대입을 막는다.

### 백엔드
- `app.auth.login-attempt` 설정(`max-failures-per-email`=5, `max-failures-per-ip`=20, `window`=15m, `lock-duration`=15m)을 `@ConfigurationProperties`로 받는다
- 로그인 시도 기록을 관리하는 `LoginAttemptLimiter`를 만든다
  - 이메일(정규화한 값)과 IP별로 실패 횟수와 차단 해제 시각을 메모리(`ConcurrentHashMap`)에 둔다
  - 시각은 주입한 `Clock` 기준으로 계산하고, 만료된 기록은 기록·조회 시 정리해 메모리가 계속 늘지 않게 한다
- `AuthService.login`의 흐름: 차단 여부 확인 → 인증 → 실패 시 실패 기록 / 성공 시 이메일 기록 초기화
  - 차단 중이면 인증을 시도하지 않고 예외를 던진다 (비밀번호가 맞아도 429)
- 컨트롤러가 `request.getRemoteAddr()`로 얻은 IP를 서비스에 함께 넘긴다
- `ResultCode.TOO_MANY_LOGIN_ATTEMPTS`(429)를 추가하고, 남은 차단 시간을 담은 예외를 `GlobalExceptionHandler`가 `Retry-After` 헤더와 함께 응답한다
- 차단이 시작될 때 이메일·IP를 WARN 로그로 남긴다 (비밀번호는 남기지 않는다)
- `openapi.yml`에 429 응답과 `Retry-After`를 추가한다
- 테스트
  - `LoginAttemptLimiter`: 이메일 5회 실패 시 차단, 기간 경과 후 해제, 성공 시 초기화, IP 기준 차단, 만료 기록 정리 (`MutableClock` 사용)
  - 서비스·컨트롤러: 차단 중 올바른 비밀번호도 429, `Retry-After` 헤더, 서로 다른 이메일의 실패가 이메일 기준 차단에 섞이지 않음

### 프론트엔드
- 로그인 폼에서 429 응답이면 `Retry-After`로 계산한 다시 시도 가능 시각을 안내한다
  - `ApiError`가 응답 헤더 값을 전달할 수 있게 확장한다
- 테스트: 429 응답의 `ApiError`에 재시도 시간이 담기는지

### 완료 기준
- 같은 이메일로 5번 틀리면 6번째는 올바른 비밀번호여도 로그인되지 않고 안내 문구가 보인다.
- 설정의 `lock-duration`을 짧게 두고 실행하면 시간이 지난 뒤 다시 로그인할 수 있다.
- 다른 이메일 계정의 로그인은 영향을 받지 않는다 (IP 기준 한도 이하일 때).

---

## Phase 37 — 로그인 시도 기록 공유 (서버 이중화 대응)
**목표**: 백엔드를 여러 대로 띄워도 로그인 실패 횟수와 차단 상태가 모든 서버에서 같게 적용되도록, 기록을 서버 메모리가 아닌 공유 저장소(Redis)에 둔다.

### 배경
- Phase 36의 `LoginAttemptLimiter`는 실패 기록을 각 서버의 메모리에 두므로, 서버가 N대면 이메일 한도가 사실상 5×N회가 되고 차단도 한 서버에만 걸린다.
- 로그인 흐름(차단 확인 → 인증 → 실패 기록 / 성공 시 초기화)과 API 응답(429, `Retry-After`)은 바꾸지 않고 저장 방식만 바꾼다.

### 백엔드
- 기록 저장을 `LoginAttemptStore` 인터페이스로 분리한다 (`remainingLock`, `recordFailure`, `reset`)
  - `InMemoryLoginAttemptStore`: Phase 36의 메모리 구현을 옮긴다 (로컬 개발·단일 서버, 테스트 기본값)
  - `RedisLoginAttemptStore`: Redis에 기록한다
  - `AuthService`는 인터페이스에만 의존한다
- `app.auth.login-attempt.store`(`memory` 기본, `redis`)로 구현을 고른다. `redis`일 때만 Redis 연결 설정(`spring.data.redis.*`)을 사용한다
- Redis 키와 만료
  - 실패 횟수: `login:fail:email:{정규화한 이메일}`, `login:fail:ip:{IP}` — 첫 실패 때 `window` 만큼 TTL을 건다
  - 차단: `login:lock:email:{이메일}`, `login:lock:ip:{IP}` — 한도에 닿으면 `lock-duration` 만큼 TTL로 만들고, 실패 횟수 키는 지운다
  - 횟수 증가·TTL 설정·차단 키 생성은 Lua 스크립트 하나로 실행해, 여러 서버의 동시 요청에서도 원자적으로 처리한다
  - 남은 차단 시간(`Retry-After`)은 차단 키의 남은 TTL(`PTTL`)로 구한다
  - 만료된 기록은 Redis TTL로 사라지므로 별도 정리 작업이 없다
- Redis 장애 시에는 로그인을 막지 않고(fail-open) ERROR 로그를 남긴다. 로그인 가용성을 우선하며, 이 결정을 `README.md`에 적는다
- 차단 시작 WARN 로그는 Lua 스크립트가 "이번 실패로 차단이 시작됨"을 돌려줄 때 한 번만 남긴다
- 로컬 실행용 Redis를 `docker-compose.yml`(또는 README 안내)로 제공하고, `README.md`에 이중화 실행 방법(`store=redis`, 서버 2대 실행)을 적는다
- `CLAUDE.md`의 시각 처리 관례에 예외를 적는다: Redis TTL로 관리하는 만료는 주입한 `Clock`이 아니라 Redis 서버 시각을 따른다
- `openapi.yml`의 로그인 시도 제한 설명에 "여러 서버가 같은 기록을 공유한다"를 반영한다 (응답 형식 변경 없음)
- 테스트
  - 두 구현에 같은 시나리오를 적용하는 공통 테스트: 이메일 5회 실패 차단, 기간 경과 후 해제, 성공 시 초기화, IP 기준 차단, 다른 이메일 분리
  - `RedisLoginAttemptStore`: Testcontainers로 실제 Redis를 띄우고 짧은 `window`·`lock-duration`으로 검증. 같은 Redis를 쓰는 저장소 객체 두 개(서버 2대 역할)에 실패를 나눠 기록해도 합산되어 차단되는지, 동시 실패 요청에서 횟수가 유실되지 않는지
  - Redis 연결 실패 시 로그인이 막히지 않는지 (fail-open)
  - 기존 `LoginAttemptLimiterTest`, 서비스·컨트롤러 테스트는 `memory` 구현으로 그대로 통과

### 프론트엔드
- 변경 없음 (429·`Retry-After` 응답 형식이 같다)

### 완료 기준
- 같은 Redis를 쓰는 백엔드 2대(`:8080`, `:8081`)에 실패 요청을 번갈아 보내면, 합쳐서 5번째 실패에서 차단되고 두 서버 모두 429를 응답한다.
- 한 서버에서 로그인에 성공하면 다른 서버에서도 그 이메일의 실패 횟수가 초기화되어 있다.
- `lock-duration`을 짧게 두면 시간이 지난 뒤 두 서버 모두에서 다시 로그인할 수 있다.
- Redis를 끈 상태에서도 로그인은 동작하고 ERROR 로그가 남는다.
- `store=memory`(기본값)로는 Redis 없이 지금처럼 실행된다.

---

## Phase 38 — 코드 리뷰 지적 사항 정리
**목표**: 백엔드 전체 리뷰(관례·정확성)에서 확인된 문제를 바로잡고, 실제 관례와 다르게 적힌 `CLAUDE.md` 문구를 명확히 한다.

### 배경
- 리뷰에서 확정되었거나 가능성이 있다고 판정된 항목을 아래 백엔드 8개로 묶어 다루고, 그 과정에서 화면 검증으로 찾은 프론트엔드 이메일 안내 문제를 함께 고친다. 기각된 항목(운영 기본 JWT 키, 프록시 IP 판별, 재발급 유예, sortOrder 중복, ListGroup 낙관적 락, H2 콘솔)은 의도된 설계이거나 v7 범위 밖이라 다루지 않는다.
- 아래 순서는 영향이 큰 것부터다. 각 항목은 독립적이라 순서를 바꿔도 된다.

### 백엔드
1. **리마인더 수정 시 `flagged` 필수 강제**
   - `ReminderUpdateRequest.flagged`를 `@NotNull Boolean`으로 바꾼다. 서비스는 값을 받아 쓰기만 한다
   - 먼저 `flagged` 없는 PUT 테스트로 현재 동작(200으로 `false` 덮어쓰기인지, 이미 400인지)을 확인하고 PR 설명에 적는다
   - `openapi.yml`은 이미 필수로 적혀 있으므로 바꾸지 않는다
   - 테스트(컨트롤러): `flagged` 누락 시 400(`BAD_REQUEST`)이고 플래그가 바뀌지 않음
2. **로그인·멤버 초대 이메일 길이 제한**
   - `LoginRequest.email`과 `ListMemberRequest.email`에 `@Size(max = User.EMAIL_MAX_LENGTH)`를 추가한다 (`SignupRequest`와 같은 제한)
   - `openapi.yml`의 두 요청 스키마에 `maxLength: 100`을 반영한다
   - 테스트(컨트롤러): 101자 이메일로 로그인·멤버 초대하면 400
3. **태그 목록·개수와 태그 필터의 기준을 이름 + 접근 가능한 리마인더로 통일**
   - 처음에는 소유자 기준으로 정했으나, 리마인더 하나에 같은 이름의 태그는 하나만 붙고 화면에는 누가 붙였든 이름이 보이므로, 소유자 기준이면 "보이는 태그로 필터해도 안 나오는" 경우가 생겨 이름 기준으로 바꿨다
   - "접근 가능한 리마인더" 조건(`ReminderRepositoryCustomImpl.accessible`의 조건)을 `AccessibleReminders`로 옮겨 태그 조회에서도 쓴다
   - `TagRepositoryCustomImpl.findAllInUseWithReminderCount`: 내 태그를 대상으로, 존재 확인과 개수 모두 "같은 이름의 태그가 붙은 접근 가능한 리마인더"로 센다
   - `ReminderRepositoryCustomImpl.findAccessibleByTagName`: 지금처럼 이름으로 찾는다
   - 멤버를 제거할 때 그 멤버의 `ReminderTag`는 지우지 않는다 (조회 시점에 제외)
   - `openapi.yml`의 `/api/tags`, `TagResponse.reminderCount`, `?tag=` 설명에 기준을 반영한다
   - 테스트(`SharedListAccessTest`)
     - 공유 리스트에서 제거된 멤버의 태그 목록과 개수에서 그 리스트의 리마인더가 빠지는지
     - 다시 초대되면 태그가 다시 보이는지
     - 다른 멤버가 붙인 같은 이름의 태그가 개수와 필터에 똑같이 포함되고, 멤버가 아닌 사용자에게는 포함되지 않는지
4. **리프레시 토큰 재발급 시 만료 행 정리**
   - `RefreshTokenService.rotate`에서 회전 결과를 flush한 뒤 해당 사용자의 만료된 행을 지운다
   - 삭제는 일괄 삭제 쿼리(`@Modifying`)로 한다. 행마다 `@Version`을 확인하는 파생 삭제는 두 기기가 동시에 재발급하며 같은 만료 행을 지울 때 낙관적 락 충돌로 401이 날 수 있다
   - 재사용 감지로 실패하는 경로(`noRollbackFor`)의 동작은 바꾸지 않는다
   - `README.md`의 "만료된 토큰은 로그인할 때 정리합니다"를 "로그인과 재발급 때 정리합니다"로 고친다
   - 테스트(`RefreshTokenServiceTest`): `MutableClock`으로 만료 행을 만든 뒤 재발급하면 그 행이 삭제되고, 만료되지 않은 행과 새 토큰은 남는지
5. **이메일 정규화를 요청 DTO 메서드로 이동**
   - `SignupRequest`와 `ListMemberRequest`에 `normalizedEmail()`을 두고, 내부에서 `User.normalizeEmail`에 위임한다
   - `AuthService.signup`은 `request.normalizedEmail()`을 쓰고, `ListMemberController`는 정규화한 값을 서비스에 넘긴다 (필드 하나짜리 요청이라 값을 넘긴다)
   - `LoginUserDetailsService`는 Spring Security가 원문을 넘기는 콜백이라 지금처럼 `User.normalizeEmail`을 쓴다
   - 테스트: 대문자가 섞인 이메일로 회원가입·멤버 초대가 기존처럼 동작하는지 (기존 테스트 호출부 갱신). 앞뒤 공백이 있는 이메일은 `@Email` 검증에서 400이다
6. **`ReminderApplication.java` 들여쓰기**
   - 탭 3줄을 4칸 공백으로 바꾼다
7. **JPA Auditing 시각에 `Clock` 적용**
   - `Instant.now(clock)`을 돌려주는 `DateTimeProvider` 빈을 두고 `@EnableJpaAuditing(dateTimeProviderRef = ...)`로 연결한다 (`JpaAuditingConfig`)
   - 테스트: 고정 `Clock`을 주입한 통합 테스트에서 저장한 엔티티의 `createdAt`/`updatedAt`이 그 시각인지, 시각을 옮긴 뒤 수정하면 `updatedAt`만 바뀌는지
8. **`CLAUDE.md` 테스트 관례 문구 정리** (코드 변경 없음)
   - 테스트 메서드명: 영어 camelCase 구간을 밑줄로 이어 `대상_조건_결과` 형태로 쓰는 것을 허용한다고 명시한다 (지금 저장소의 형식)
   - Service 테스트: 커밋 시점 동작(`noRollbackFor` 커밋, 커밋 시점 낙관적 락 충돌)을 검증하는 테스트는 `@Transactional` 없이 쓰고 `@AfterEach`로 직접 정리하는 것을 예외로 허용한다고 명시한다
   - 시각 처리 관례에 JPA Auditing도 주입한 `Clock`을 따른다는 점을 반영한다

### 프론트엔드
- 리마인더 수정은 항상 `flagged`를 보내고, 태그 필터는 이름을 그대로 쓰므로 바꿀 것이 없다
- 이메일 100자 제한을 화면에서도 먼저 검사한다
  - 이유: 서버가 100자를 넘는 이메일을 400으로 막자, 초대 폼은 400을 "이미 이 리스트의 멤버입니다."로, 로그인 폼은 일반 오류로 표시해 안내가 틀렸다
  - `lib/email.ts`에 기준값(`EMAIL_MAX_LENGTH`)과 판정 함수를 두고, 로그인·회원가입·멤버 초대 폼에서 "이메일은 100자 이하로 입력해 주세요."로 막는다 (요청을 보내지 않는다)
  - 테스트(`lib/email.test.ts`): 100자 허용, 101자 거부, 앞뒤 공백 무시

### 완료 기준
- `flagged` 없이 리마인더를 수정하면 400이고, 플래그 상태가 유지된다.
- 공유 리스트에서 멤버를 제거하면, 그 멤버의 사이드바 태그 개수에서 그 리스트의 리마인더가 빠진다.
- 사이드바 태그 개수와 그 태그로 필터한 미완료 리마인더 수가 일치한다(다른 멤버가 같은 이름의 태그를 붙인 공유 리마인더 포함).
- 재발급을 반복해도 만료된 리프레시 토큰 행이 남지 않는다.
- 로그인, 회원가입, 멤버 초대, 태그 필터가 화면에서 이전과 같게 동작한다.
- 화면에서 100자 초과 이메일로 로그인·회원가입·멤버 초대하면 길이 안내가 나오고 요청을 보내지 않는다.
- 고정 `Clock`으로 실행하면 `createdAt`/`updatedAt`도 그 `Clock`의 시각으로 기록된다.
- 백엔드 테스트와 프론트엔드 `npm test`가 모두 통과한다.

---

# v8 — E2E 테스트 (spec.md 16번)

현재 제공되는 모든 사용자 기능(spec.md 16번 "E2E 대상 기능")을 E2E로 확인한다. 한 PR이 너무 커지지 않도록,
테스트 기반과 인증을 먼저 만들고 기능 영역별로 시나리오를 나눠 추가한다.

| 순서 | Phase | 내용 |
|---|---|---|
| 1 | Phase 39 | E2E 기반(Playwright 설정, 헬퍼, CI) + 인증 |
| 2 | Phase 40 | 리스트·그룹·리마인더 기본 기능, 스마트 뷰, 완료 항목, 좁은 화면 |
| 3 | Phase 41 | 태그, 하위 작업, 반복, 리스트 이동, 검색, 드래그 정렬, 알림 |
| 4 | Phase 42 | 공유·협업, 동기화, 동시 편집 충돌 |
| 5 | Phase 43 | E2E를 `frontend` 하위에서 루트의 `e2e` 패키지로 분리, 테스트를 배포 전 필수·보조로 나누고 CI job 분리 |

### 공통 원칙 (Phase 39~43)
- 사용자에게 보이는 기능과 API는 바꾸지 않는다. 테스트를 쓰다가 실제 버그를 찾으면 같은 Phase에서 고치고 PR 설명에 따로 적는다.
- 새 시나리오는 처음부터 CI에서 실행한다. 로컬에서만 도는 테스트는 만들지 않는다.
- 각 테스트는 서로 독립적이다. 테스트마다 고유한 사용자를 만들고, 실행 순서나 다른 테스트가 만든 데이터에 의존하지 않는다.
- 화면 흐름 자체가 검증 대상이 아닌 준비 데이터는 API로 만든다. 검증하려는 동작은 반드시 화면 조작으로 한다.
- 요소는 화면에 있는 접근성 이름(`aria-label`, 버튼 이름, `label`)으로 찾는다. 찾을 수단이 없을 때만 컴포넌트에 `aria-label`을 추가한다 (`data-testid`는 쓰지 않는다).
- 고정 시간 대기(`waitForTimeout`)를 쓰지 않는다. Playwright의 자동 대기 단언(`toBeVisible`, `toHaveText`, `toHaveCount` 등)과 `expect.poll`을 쓴다.
- 시각에 따라 결과가 바뀌는 시나리오(오늘 뷰, 마감 지남, 알림)는 Playwright의 `page.clock`과 고정 시간대로 브라우저 시각을 정한다.
- 각 Phase가 끝날 때 E2E를 연속 3번 실행해 모두 통과하는지 확인한다.

---

## Phase 39 — E2E 기반 + 인증
**목표**: Playwright로 두 서버를 띄워 브라우저 테스트를 돌리는 기반과 CI job을 만들고, 인증 흐름을 확인한다.

### 구성
- `frontend`에 `@playwright/test`를 devDependency로 추가하고, 테스트는 `frontend/e2e/`에 둔다 (`*.spec.ts`)
  - Vitest는 `src/**/*.test.ts`만 실행하므로 `e2e/`와 겹치지 않는다
- 스크립트: `npm run e2e`(헤드리스), `npm run e2e:ui`(디버그용 UI 모드)
- `playwright.config.ts`
  - Chromium만 쓰고, `locale: "ko-KR"`, `timezoneId: "Asia/Seoul"`로 고정한다
  - `webServer`로 두 서버를 띄운다
    - 백엔드: `../backend/gradlew bootRun`에 E2E 전용 인자를 넘긴다
      - 인메모리 H2(`--spring.datasource.url=jdbc:h2:mem:e2e`)로 로컬 개발 DB를 건드리지 않는다
      - 로그인 시도 IP 한도를 높인다(`--app.auth.login-attempt.max-failures-per-ip=1000`). 모든 테스트가 같은 IP에서 실행되기 때문이다. 이메일 한도(5회)는 그대로 둔다
      - 액세스 토큰 수명을 짧게 둔다(`--app.jwt.access-token-ttl=30s`). 모든 시나리오가 실행 중에 자동 재발급을 거치게 되고, 만료 시나리오도 짧게 기다릴 수 있다
    - 프론트엔드: CI에서는 `npm run build && npm run start`, 로컬에서는 `npm run dev`. 이미 떠 있는 서버가 있으면 재사용한다(`reuseExistingServer: !process.env.CI`)
    - 준비 확인 URL: 백엔드는 `/api/auth/me`(401 응답으로 준비 판단), 프론트엔드는 `/login`
  - 실패 시 스크린샷과 trace를 남긴다 (`screenshot: "only-on-failure"`, `trace: "retain-on-failure"`)
  - CI에서만 재시도 1회. 재시도로 통과한 테스트(flaky)는 리포트에서 확인해 원인을 고친다
- `.gitignore`에 `playwright-report/`, `test-results/`를 추가한다
- `e2e/support/`
  - 고유 이메일 생성, API로 회원가입·로그인해 액세스 토큰을 얻는 헬퍼
  - API로 리스트·리마인더·그룹 등을 만드는 헬퍼 (`/api/...`를 Bearer 토큰으로 호출)
  - 화면으로 로그인하는 헬퍼, 사용자별 브라우저 컨텍스트를 만드는 fixture

### 시나리오 — `auth.spec.ts`
- 회원가입하면 메인 화면으로 이동하고 사이드바 하단에 내 이메일이 보인다
- 회원가입 입력 검증: 빈 값, 이메일 형식, 100자 초과 이메일, 비밀번호 8자 미만, 72바이트 초과, 비밀번호 확인 불일치 안내
- 이미 가입한 이메일로 가입하면 "이미 가입한 이메일입니다."가 보인다
- 로그아웃하면 로그인 화면으로 이동하고, 대문자가 섞인 이메일로 다시 로그인할 수 있다
- 비밀번호가 틀리거나 없는 이메일이면 "이메일 또는 비밀번호가 올바르지 않습니다."가 보인다. 100자 초과 이메일은 길이 안내가 나온다
- 로그인하지 않은 상태로 `/`에 가면 로그인 화면으로 이동하고, 로그인한 상태로 `/login`에 가면 메인으로 이동한다
- 새로고침해도 로그인이 유지된다. 같은 사용자의 탭 두 개를 동시에 새로고침해도 둘 다 로그인이 유지된다
- 액세스 토큰 수명이 지난 뒤에 리마인더를 추가해도 로그아웃되지 않고 요청이 이어서 처리된다
- 같은 이메일로 5번 틀리면 6번째는 올바른 비밀번호여도 로그인되지 않고, 다시 시도할 수 있는 시각이 안내된다

### CI
- `.github/workflows/ci.yml`에 `e2e` job을 추가한다
  - Java 21과 Node를 설정하고, `npx playwright install --with-deps chromium`
  - Gradle과 npm 캐시를 쓴다
  - 실패하면 `playwright-report/`와 `test-results/`를 artifact로 올린다
- 기존 `backend`, `frontend` job과 병렬로 실행한다

### 문서
- `README.md`에 E2E 실행 방법을 적는다 (처음 한 번 `npx playwright install chromium`, 이후 `npm run e2e`)
- `CLAUDE.md` 프론트엔드 테스트 관례에 추가
  - 사용자 흐름이 바뀌는 화면 변경은 해당 E2E 시나리오를 같은 작업 안에서 갱신한다
  - E2E 테스트 이름도 영어(camelCase)로 쓴다
  - 작업 방식의 "서버를 직접 실행한 뒤 직접 테스트"에 `npm run e2e` 결과를 쓸 수 있다

### 완료 기준
- 서버를 띄우지 않은 상태에서 `npm run e2e` 하나로 두 서버가 뜨고 인증 시나리오가 모두 통과한다.
- 실행 후 로컬 개발 DB(`backend/data/`)가 바뀌지 않는다.
- PR의 CI에서 `e2e` job이 통과하고, 시나리오 하나를 일부러 깨뜨리면 CI가 실패하며 리포트에서 실패 화면과 trace를 볼 수 있다.

---

## Phase 40 — 리스트·그룹·리마인더 기본 기능
**목표**: 리스트와 그룹 관리, 리마인더의 기본 속성, 스마트 뷰, 완료 항목 관리를 화면에서 확인한다.

### 시나리오
- `lists.spec.ts`
  - 리스트를 이름·색상·아이콘을 골라 만들면 사이드바에 그 색상과 아이콘으로 나타나고, 그 리스트가 선택된다
  - 리스트 이름·색상·아이콘을 편집하면 사이드바와 리스트 화면 제목에 반영된다
  - 리스트를 삭제하면 확인 창을 거쳐 사이드바에서 사라지고, 그 리스트의 리마인더도 다른 뷰에서 사라진다
  - 사이드바의 리스트 개수는 미완료 최상위 리마인더 수와 같고, 추가·완료·삭제에 따라 바뀐다
- `groups.spec.ts`
  - 그룹을 만들고 리스트 메뉴의 "그룹으로 이동"으로 리스트를 넣으면 그룹 아래에 표시된다
  - 그룹을 접으면 소속 리스트가 숨겨지고, 새로고침해도 접힌 상태가 유지된다
  - 그룹 이름을 바꾸면 반영되고, 그룹을 삭제하면 소속 리스트와 리마인더는 그룹 밖에 남는다
  - 리스트를 그룹에서 빼면 그룹 없는 리스트로 돌아간다
- `reminders.spec.ts`
  - 리스트 화면에서 리마인더를 추가하면 목록에 나타난다. 추가 폼에서 마감일과 반복 없이도 추가된다
  - 편집 창에서 제목·메모·마감일시(달력과 시각 선택)·우선순위·플래그를 바꿔 저장하면 목록에 반영된다 (우선순위는 `!`/`!!`/`!!!`, 마감일 표시)
  - 목록의 플래그 버튼으로 플래그를 켜고 끌 수 있다
  - 완료 체크하면 완료 상태가 되고, 다시 체크하면 완료가 취소된다
  - 삭제하면 목록에서 사라진다
  - 마감 시각이 지난 미완료 리마인더는 마감 지남으로 표시된다 (`page.clock`으로 시각 고정)
  - 시간대가 다른 두 브라우저(서울, 뉴욕)에서 같은 리마인더의 마감 시각이 각자의 현지 시각으로 표시된다
- `smart-views.spec.ts` (`page.clock`으로 "오늘"을 고정)
  - "오늘"에는 오늘 마감인 미완료 리마인더만, "예정됨"에는 마감일이 있는 미완료 리마인더만, "전체"에는 미완료 리마인더 전부, "플래그 지정됨"에는 플래그된 미완료 리마인더만, "완료됨"에는 완료한 리마인더만 보인다
  - 각 뷰에 해당 항목이 없으면 뷰별 빈 상태 안내가 보인다
  - 스마트 뷰의 리마인더에는 소속 리스트 이름이 표시된다
- `completed.spec.ts`
  - 리스트 화면에서 완료 항목을 숨기면 완료 항목이 사라지고, 새로고침해도 숨김이 유지되며, 다시 보이게 할 수 있다
  - "지우기"를 누르면 확인 창에 완료 항목 개수가 나오고, 확인하면 그 리스트의 완료 항목만 삭제된다 (다른 리스트의 완료 항목은 남는다)
  - 완료 항목 60개를 API로 만든 뒤 "완료됨"에서 처음 50개가 보이고, 목록 끝까지 스크롤하면 60개 모두 순서대로 보인다
- `layout.spec.ts` (좁은 뷰포트)
  - 사이드바가 숨겨져 있고, "메뉴 열기"로 열어 리스트를 고르면 사이드바가 닫히고 그 리스트가 보인다

### 완료 기준
- 위 시나리오가 모두 통과하고, 연속 3번 실행해도 통과한다.
- PR의 CI에서 `e2e` job이 통과한다.

---

## Phase 41 — 리마인더 확장 기능
**목표**: 태그, 하위 작업, 반복, 리스트 이동, 검색, 드래그 정렬, 알림을 화면에서 확인한다.

### 시나리오
- `tags.spec.ts`
  - 편집 창에서 태그를 입력하고 Enter로 칩을 추가하면 리마인더에 `#태그`가 표시되고, 사이드바 "태그"에 개수와 함께 나타난다
  - 칩의 X로 태그를 떼면 리마인더와 사이드바에서 빠진다
  - 사이드바에서 태그를 누르면 그 태그의 리마인더만 보이고, 개수와 목록 수가 같다
  - 사이드바에서 태그를 삭제하면 태그 목록에서 사라지고, 리마인더는 태그 없이 남는다
- `subtasks.spec.ts`
  - 편집 창에서 하위 작업을 추가하면 부모 아래에 들여쓰기로 표시되고, 펼치기/접기로 숨기고 보일 수 있다
  - 하위 작업을 따로 완료·편집·삭제할 수 있다
  - 부모를 완료하면 하위 작업도 함께 완료되고, 부모를 삭제하면 하위 작업도 함께 사라진다
  - 사이드바 리스트 개수에는 최상위 리마인더만 센다
- `repeat.spec.ts`
  - 반복 선택에서 매일/매주/매월/매년/평일마다를 고르면 리마인더에 반복 요약이 표시된다
  - "사용자 지정..."에서 간격과 단위, 주 단위 요일을 고르면 "2주마다 월·수"처럼 요약된다
  - 마감일 없이 반복을 고를 수 없거나 안내가 나온다 (현재 화면 동작대로 확인)
  - 매주 반복 리마인더를 완료하면 완료 항목은 "완료됨"으로 가고, 다음 마감일(7일 뒤)의 새 리마인더가 같은 제목으로 생긴다
- `move.spec.ts`
  - 편집 창의 리스트 선택으로 리마인더를 다른 리스트로 옮기면 하위 작업과 함께 이동하고, 사이드바 양쪽 개수가 바뀐다
  - 하위 작업의 편집 창에는 리스트 선택이 없다
- `search.spec.ts`
  - 검색창에 입력하면 제목·메모·태그가 일치하는 리마인더가 소속 리스트와 함께 보인다
  - 일치하는 항목이 없으면 결과 없음 안내가 보인다
  - 검색어를 지우면 검색 전에 보던 화면으로 돌아간다
  - 일치하는 리마인더 60개를 API로 만든 뒤 처음 50개가 보이고, 스크롤하면 60개 모두 보인다
- `reorder.spec.ts` (키보드 드래그: 손잡이에 포커스 → Space → 방향키 → Space)
  - 리스트 화면에서 리마인더 순서를 바꾸면 반영되고, 새로고침해도 유지된다
  - 사이드바에서 리스트 순서, 그룹 순서, 그룹 안 리스트 순서를 바꾸면 반영되고, 새로고침해도 유지된다
  - 스마트 뷰와 검색 결과에서는 순서 이동 손잡이가 없다
- `notifications.spec.ts` (`page.clock`과 알림 권한 부여, `Notification`을 기록용으로 교체하는 init script)
  - 알림 권한이 거부된 상태면 차단 안내가, 허용된 상태면 안내 없이 동작한다
  - 마감 시각이 되면 그 리마인더의 알림이 한 번만 뜨고, 시각을 더 진행해도 다시 뜨지 않는다
  - 마감일시를 바꾸면 새 시각에 다시 알림이 뜬다
  - 알림을 클릭하면 그 리마인더의 편집 창이 열린다

### 완료 기준
- 위 시나리오가 모두 통과하고, 연속 3번 실행해도 통과한다.
- PR의 CI에서 `e2e` job이 통과한다.

---

## Phase 42 — 공유·협업
**목표**: 여러 사용자가 리스트를 함께 쓰는 흐름과 동시 편집 충돌 처리를 화면에서 확인한다. 모든 시나리오는 사용자별 브라우저 컨텍스트를 따로 쓴다.

### 시나리오
- `sharing.spec.ts`
  - 소유자가 공유 창에서 대문자가 섞인 이메일로 초대하면 "○○님을 초대했습니다."가 나오고 멤버 목록에 편집자로 추가된다
  - 없는 사용자·이미 멤버인 사용자·100자 초과 이메일로 초대하면 각각 맞는 안내가 나온다 (100자 초과는 요청을 보내지 않는다)
  - 초대받은 사용자의 사이드바 "공유받은 리스트"에 리스트가 공유 아이콘·멤버 수와 함께 나타나고, 소유자 사이드바에도 공유 아이콘이 보인다
  - 편집자는 공유 리스트의 리마인더를 추가·편집·완료·플래그·삭제할 수 있다
  - 편집자의 화면에는 리스트 편집·삭제 버튼과 초대 폼이 없고, 공유 창은 멤버 보기로 열린다
  - 편집자가 "리스트 나가기"를 확인하면 그 리스트가 사이드바에서 사라진다
  - 소유자가 멤버를 제거하면 그 사용자의 화면에서 리스트가 사라진다
- `sharing-sync.spec.ts`
  - 한 사용자가 공유 리스트에 리마인더를 추가·완료하면, 다른 사용자 화면에 새로고침 없이 반영된다 (동기화 주기 안에서 `expect.poll`로 확인)
- `sharing-tags.spec.ts`
  - 공유 리마인더에 다른 멤버가 같은 이름의 태그를 붙였을 때, 사이드바 태그 개수와 태그를 눌렀을 때의 리마인더 수가 같다
  - 멤버가 제거되면 그 사용자의 태그 개수와 태그 보기에서 그 리스트의 리마인더가 빠진다
- `conflict.spec.ts`
  - 두 사용자가 같은 리마인더 편집 창을 열고 차례로 저장하면, 나중 쪽에 "다른 사용자가 먼저 수정했습니다. 최신 내용을 불러왔습니다."가 나오고 먼저 저장한 내용이 채워진다
  - 리스트 편집에서도 같은 충돌 처리가 동작한다

### 완료 기준
- 위 시나리오가 모두 통과하고, 연속 3번 실행해도 통과한다.
- PR의 CI에서 `e2e` job이 통과한다.
- spec.md 16번 "E2E 대상 기능" 표의 모든 항목이 시나리오로 확인된다.

---

## Phase 43 — E2E 패키지 분리 + 필수·보조 분리
**목표**: E2E를 `frontend` 하위에서 루트의 `e2e` 패키지로 옮겨 저장소를 `backend`·`frontend`·`e2e` 역할별로 나눈다. 테스트를 배포 전 필수와 보조로 나누고, 나중에 브랜치별로 실행 범위를 고를 수 있도록 CI job도 나눈다. 시나리오 내용은 바꾸지 않는다.

### 배경
- E2E는 백엔드와 프론트엔드를 모두 띄우는 시스템 테스트인데 `frontend` 아래에 있어, 설정이 `../backend`만 따로 실행하는 비대칭 구조였고 프론트엔드 의존성에 Playwright가 섞여 있었다.
- E2E 코드는 앱 코드를 import하지 않고 `@playwright/test`와 Node 내장 모듈만 쓰므로, 코드 수정 없이 옮길 수 있다.

### 구성
- `git mv`로 이력을 유지하며 옮긴다
  - `frontend/e2e/` → `e2e/tests/` (`support/` 포함, 상대 import는 그대로)
  - `frontend/playwright.config.ts` → `e2e/playwright.config.ts`
- `e2e/package.json`
  - devDependencies: `@playwright/test`, `@types/node`, `typescript` (버전은 `frontend`에서 쓰던 것과 같게)
  - 스크립트: `npm run e2e`, `npm run e2e:ui`, `npm run typecheck`(`tsc --noEmit`)
  - Volta Node·npm 버전은 `frontend`와 같게 둔다
- `e2e/tsconfig.json`: `playwright.config.ts`와 `tests/**/*.ts`만 검사한다
- `e2e/.gitignore`: `node_modules/`, `playwright-report/`, `test-results/`, `blob-report/`, `playwright/.cache/`
- `playwright.config.ts`
  - `testDir`를 `./tests`로 바꾼다
  - 프론트엔드 `webServer`에 `cwd: "../frontend"`를 지정해 백엔드(`cwd: "../backend"`)와 같은 방식으로 띄운다
- `frontend` 정리
  - `@playwright/test` devDependency와 `e2e`·`e2e:ui` 스크립트를 빼고 `package-lock.json`을 갱신한다
  - `.gitignore`와 `eslint.config.mjs`에서 Playwright 결과 폴더 항목을 뺀다

### 필수·보조 분리
- 기준: "실패하면 배포를 멈춰야 하는가"
  - **필수**(`@essential`): 서비스를 쓸 수 없게 되거나, 데이터가 사라지거나 덮어써지거나, 권한이 새는 경우
  - **보조**(태그 없음): 안내 문구, 표시, 편의 기능, 하위 계층 테스트로도 확인할 수 있는 규칙(개수, 페이지네이션, 반복 요약 등)
- 필수 테스트에 Playwright 태그 `{ tag: "@essential" }`을 붙인다. 테스트 이름과 내용은 바꾸지 않는다
- 필수 25개 (나머지 43개는 보조)

| 기준 | 테스트 |
|---|---|
| 서비스 사용 불가 | `auth`: 가입, 로그아웃 후 대소문자 이메일 로그인, 비로그인 접근 차단, 새로고침 유지, 두 탭 동시 새로고침 유지, 토큰 만료 후 요청 처리 |
| | `reminders`: 추가, 완료·완료 취소, 전체 필드 수정 / `lists`: 생성 / `smart-views`: 조건에 맞는 항목 표시 |
| 데이터 손실·덮어쓰기 | `reminders`: 삭제 / `lists`: 삭제 시 리마인더 함께 삭제 / `groups`: 이름 변경·삭제 시 리스트 유지 / `subtasks`: 부모 완료·삭제가 하위에 적용 |
| | `move`: 이동 시 하위 작업 함께 이동 / `repeat`: 매주 반복 완료 시 다음 회차 생성 / `completed`: 완료 항목 지우기는 해당 리스트만 / `tags`: 태그 삭제 시 리마인더 유지 |
| | `conflict`: 리마인더·리스트를 차례로 저장하면 충돌 안내와 최신 내용 |
| 권한 | `sharing`: 초대, 편집자의 리마인더 관리, 편집자에게 편집·삭제·초대 없음, 멤버 제거 후 접근 차단 |

- `e2e/package.json` 스크립트: `npm run e2e:essential`(`--grep @essential`), `npm run e2e:supplementary`(`--grep-invert @essential`). `npm run e2e`는 전체를 실행한다

### CI
- E2E job을 `E2E essential`과 `E2E supplementary` 두 job으로 나누고 병렬로 실행한다
  - 작업 디렉터리는 `e2e`로 둔다
  - E2E가 프론트엔드를 빌드·실행하므로 `frontend`와 `e2e`에서 각각 `npm ci`
  - npm 캐시 키에 두 `package-lock.json`을 모두 쓴다
  - `npx playwright install --with-deps chromium` 후 각각 `npm run e2e:essential`, `npm run e2e:supplementary`
  - 실패하면 `e2e/playwright-report/`와 `e2e/test-results/`를 각각 `e2e-essential-report`, `e2e-supplementary-report` artifact로 올린다
- E2E 코드는 더 이상 `frontend`의 ESLint와 `next build` 타입 검사 대상이 아니므로, 타입 검사(`npm run typecheck`)는 `E2E essential` job에서 한다
- 지금은 모든 브랜치에서 두 job을 모두 실행한다. 브랜치별로 범위를 나눌 때는 job에 `if:` 조건만 추가한다

### 문서
- `README.md`: 사전 준비의 Node 버전 기준, E2E 실행 방법(`cd e2e` → `npm install`, 브라우저 설치, `npm run typecheck`, `npm run e2e`·`e2e:essential`·`e2e:supplementary`), 필수·보조 기준, CI 설명
- `CLAUDE.md`
  - 작업 방식의 화면 검증 문구를 "`e2e`의 `npm run e2e`"로 바꾼다
  - E2E 관례를 프론트엔드 섹션에서 "e2e 하위" 섹션으로 옮긴다 (위치, 앱 코드 import 금지, 테스트 이름 규칙, 필수 태그 기준, 타입 검사)
  - 프론트엔드 섹션에는 "사용자 흐름이 바뀌는 화면 변경은 E2E 시나리오(`e2e/tests/`)를 함께 갱신"만 남긴다

### 완료 기준
- 서버를 띄우지 않은 상태에서 `e2e`의 `npm run e2e` 하나로 두 서버가 뜨고 모든 시나리오가 통과한다. 연속 3번 실행해도 통과한다.
- `e2e`의 `npm run typecheck`가 통과한다.
- `frontend`에서 `npm run lint`, `npm test`, `npm run build`가 Playwright 없이 통과한다.
- `npm run e2e:essential`은 필수 25개만, `npm run e2e:supplementary`는 보조 43개만 실행하고 모두 통과한다.
- PR의 CI에서 `Backend test`, `Frontend lint & build`, `E2E essential`, `E2E supplementary` job이 모두 통과한다.

---

## 참고
- 각 Phase는 이전 Phase 위에서 빌드/실행 가능한 상태를 유지하며 순차 진행한다.
- spec.md 3번의 Out-of-Scope 중 10번에 포함되지 않은 항목(자연어 날짜 입력, 첨부파일, 위치 기반 리마인더)과 Web Push, 실시간(WebSocket) 동기화는 이 계획 범위 밖이며 필요 시 별도 논의한다.
