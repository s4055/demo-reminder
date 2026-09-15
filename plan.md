# 개발 계획: Apple Reminders 웹 클론 (demoreminder)

`spec.md`의 3번(범위) 이후 내용을 기준으로, **가장 단순한 기능부터 시작해서 점진적으로
기능을 추가**하는 방식으로 개발한다. 각 Phase는 그 자체로 실행 가능한 상태(빌드/구동 가능)를 유지한다.

## 기술 스택

### 공통 / 개발 환경
- 백엔드: `C:\project\demo-reminder` (기존 Spring Boot 4 프로젝트 재사용)
- 프론트엔드: `C:\project\demo-reminder\frontend` 하위에 신규 Next.js 프로젝트 생성 (모노레포 구조)
- 로컬 포트: 백엔드 `:8080`, 프론트엔드 `:3000`
- 인증 없음 (spec.md 기준)

### 백엔드
- Spring Boot 4.1.1, Gradle(Groovy DSL), Java 21
- Spring Data JPA + Hibernate, H2 (in-memory)
- Lombok (`@Getter/@Setter/@NoArgsConstructor/@AllArgsConstructor`)
- Bean Validation (`spring-boot-starter-validation`)
- CORS: `WebMvcConfigurer`로 `http://localhost:3000` 허용

### 프론트엔드
- Next.js latest (App Router), TypeScript
- Tailwind CSS + shadcn/ui
- 데이터 페칭: TanStack Query (React Query) — 서버 상태 캐싱/재조회에 유리, Phase 1부터 도입
- 날짜 처리: `date-fns`
- 폼: React Hook Form (Phase 4 상세 편집부터 도입)

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

## 참고
- 각 Phase는 이전 Phase 위에서 빌드/실행 가능한 상태를 유지하며 순차 진행한다.
- spec.md의 "10. 향후 고려 사항"(인증, 태그, 하위작업, 공유, 알림, 드래그앤드롭 등)은 이 계획 범위 밖이며 Phase 5 이후 별도 논의한다.
