# 작업 목록: Apple Reminders 웹 클론 (demoreminder)

`plan.md`를 기준으로 Phase별 세부 작업을 체크리스트로 정리한다.

## Phase 0 — 공통 준비
- [x] `frontend/` 하위에 Next.js 프로젝트 생성 (`create-next-app`, TypeScript, App Router, Tailwind)
- [x] shadcn/ui 초기 설정 (`npx shadcn@latest init`)
- [x] TanStack Query Provider 설정 (`QueryClientProvider`)
- [x] 백엔드에 CORS 설정 추가 (`WebMvcConfigurer` — `http://localhost:3000` 허용)
- [x] `spring-boot-starter-validation` 의존성 추가

## Phase 1 — 최소 기능: 단일 리마인더 목록
### 백엔드
- [x] `Reminder` 엔티티 작성 (id, title, memo, completed, createdAt)
- [x] `ReminderRepository` (JpaRepository) 작성
- [x] `ReminderService` 작성
- [x] `ReminderController` 작성
  - [x] `GET /api/reminders`
  - [x] `POST /api/reminders`
  - [x] `PATCH /api/reminders/{id}/complete`
  - [x] `DELETE /api/reminders/{id}`
- [x] 요청/응답 DTO 정의 및 유효성 검증 (`title` 필수)

### 프론트엔드
- [x] 리마인더 API 클라이언트 함수 작성 (`fetch` 기반)
- [x] 리마인더 입력 폼 컴포넌트 (제목 입력 + 추가 버튼)
- [x] 리마인더 목록 컴포넌트 (체크박스 + 삭제 버튼)
- [x] TanStack Query로 목록 조회(`useQuery`) 연동
- [x] 생성/완료/삭제 mutation(`useMutation`) + 캐시 무효화 연동
- [x] 단일 페이지(`/`)에 폼 + 목록 배치

### 완료 기준 검증
- [x] 리마인더 추가 → 목록 즉시 표시 확인
- [x] 체크 시 취소선/완료 표시 확인
- [x] 삭제 시 목록에서 제거 확인

## Phase 2 — 리스트(List) 기능 추가
### 백엔드
- [x] `List` 엔티티 작성 (id, name, color, createdAt)
- [x] `Reminder`에 `listId` FK(연관관계) 추가 + 마이그레이션 확인(H2 `ddl-auto: update`)
- [x] `ListRepository` 작성
- [x] `ListService` 작성
- [x] `ListController` 작성
  - [x] `GET /api/lists` (리마인더 개수 포함)
  - [x] `POST /api/lists`
  - [x] `PUT /api/lists/{id}`
  - [x] `DELETE /api/lists/{id}` (cascade로 소속 리마인더 삭제)
- [x] `GET /api/reminders?listId=` 쿼리 파라미터 지원

### 프론트엔드
- [x] 좌측 사이드바 레이아웃 컴포넌트 추가
- [x] 리스트 생성 UI (다이얼로그/인풋)
- [x] 리스트 삭제 UI (확인 포함)
- [x] 사이드바 리스트 클릭 시 해당 리마인더만 조회하도록 상태 관리(선택된 listId)
- [x] 사이드바에 리스트별 리마인더 개수 뱃지 표시

### 완료 기준 검증
- [x] 리스트 생성 후 리마인더를 해당 리스트에 추가
- [x] 사이드바에서 리스트 선택 시 해당 리마인더만 노출되는지 확인

## Phase 3 — 마감일/플래그 + 스마트 리스트
### 백엔드
- [ ] `Reminder`에 `dueAt`(LocalDateTime, nullable), `flagged`(boolean) 필드 추가
- [ ] `ReminderService`에 스마트 뷰 조회/플래그 토글 로직 추가
- [ ] `GET /api/reminders/smart/{view}` 구현
  - [ ] `today` — 마감일이 오늘인 미완료
  - [ ] `scheduled` — 마감일이 설정된 모든 미완료
  - [ ] `all` — 모든 미완료
  - [ ] `flagged` — flagged=true 인 미완료
  - [ ] `completed` — completed=true
- [ ] `PATCH /api/reminders/{id}/flag` 추가
- [ ] 생성/수정 API에 `dueAt` 필드 반영

### 프론트엔드
- [ ] 사이드바 상단 스마트 리스트 메뉴 추가 (오늘/예정됨/전체/플래그 지정됨/완료됨)
- [ ] 리마인더 항목에 마감일 표시 (date-fns 포맷)
- [ ] 리마인더 항목에 플래그 아이콘 + 토글 인터랙션
- [ ] 생성 폼에 날짜/시간 선택 UI 추가 (shadcn `Popover` + `Calendar`)
- [ ] 스마트 뷰 선택 상태 관리 및 API 연동

### 완료 기준 검증
- [ ] 오늘 날짜로 마감일 설정 시 "오늘" 뷰에 노출 확인
- [ ] 플래그 토글 시 "플래그 지정됨" 뷰에 즉시 반영 확인

## Phase 4 — 리마인더 상세 편집 + 리스트 관리 고도화
### 백엔드
- [ ] `ReminderService`에 전체 수정 로직 추가
- [ ] `PUT /api/reminders/{id}` 전체 수정(제목/메모/마감일/플래그) 완성
- [ ] `ListService`에 이름/색상 수정 로직 추가
- [ ] `PUT /api/lists/{id}` 이름/색상 수정 완성

### 프론트엔드
- [ ] React Hook Form 도입
- [ ] 리마인더 상세/편집 모달(또는 우측 패널) 컴포넌트 작성
- [ ] 리마인더 클릭 시 상세 편집 열기 인터랙션
- [ ] 리스트 이름/색상 편집 UI (색상 피커 포함)

### 완료 기준 검증
- [ ] 리마인더 메모/마감일/플래그 수정 후 목록 즉시 반영 확인
- [ ] 리스트 이름/색상 변경이 사이드바에 즉시 반영되는지 확인

## Phase 5 — UI/UX 폴리싱 & 안정화
- [ ] 전체 UI를 shadcn/ui 컴포넌트로 통일 (버튼, 체크박스, 모달, 토스트)
- [ ] 로딩 상태 UI 처리 (skeleton 등)
- [ ] 에러 상태 UI 처리 (toast/알림)
- [ ] 빈 상태(empty state) UI 처리
- [ ] 반응형 레이아웃 대응 (모바일에서 사이드바 토글)
- [ ] 완료된 항목 정렬 로직 적용 (완료일 최신순 등)
- [ ] 전체 기능 수동 QA
- [ ] README 작성 (실행 방법: 백엔드 `:8080`, 프론트엔드 `:3000`)

### 완료 기준 검증
- [ ] `spec.md` "9. 성공 기준(Acceptance Criteria)" 항목 전체 수동 테스트 통과
