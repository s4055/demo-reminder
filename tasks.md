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
- [x] `Reminder`에 `dueAt`(LocalDateTime, nullable), `flagged`(boolean) 필드 추가
- [x] `ReminderService`에 스마트 뷰 조회/플래그 토글 로직 추가
- [x] `GET /api/reminders/smart/{view}` 구현
  - [x] `today` — 마감일이 오늘인 미완료
  - [x] `scheduled` — 마감일이 설정된 모든 미완료
  - [x] `all` — 모든 미완료
  - [x] `flagged` — flagged=true 인 미완료
  - [x] `completed` — completed=true
- [x] `PATCH /api/reminders/{id}/flag` 추가
- [x] 생성/수정 API에 `dueAt` 필드 반영

### 프론트엔드
- [x] 사이드바 상단 스마트 리스트 메뉴 추가 (오늘/예정됨/전체/플래그 지정됨/완료됨)
- [x] 리마인더 항목에 마감일 표시 (date-fns 포맷)
- [x] 리마인더 항목에 플래그 아이콘 + 토글 인터랙션
- [x] 생성 폼에 날짜/시간 선택 UI 추가 (shadcn `Popover` + `Calendar`)
- [x] 스마트 뷰 선택 상태 관리 및 API 연동

### 완료 기준 검증
- [x] 오늘 날짜로 마감일 설정 시 "오늘" 뷰에 노출 확인
- [x] 플래그 토글 시 "플래그 지정됨" 뷰에 즉시 반영 확인

## Phase 4 — 리마인더 상세 편집 + 리스트 관리 고도화
### 백엔드
- [x] `ReminderService`에 전체 수정 로직 추가
- [x] `PUT /api/reminders/{id}` 전체 수정(제목/메모/마감일/플래그) 완성
- [x] `ListService`에 이름/색상 수정 로직 추가
- [x] `PUT /api/lists/{id}` 이름/색상 수정 완성

### 프론트엔드
- [x] React Hook Form 도입
- [x] 리마인더 상세/편집 모달(또는 우측 패널) 컴포넌트 작성
- [x] 리마인더 클릭 시 상세 편집 열기 인터랙션
- [x] 리스트 이름/색상 편집 UI (색상 피커 포함)

### 완료 기준 검증
- [x] 리마인더 메모/마감일/플래그 수정 후 목록 즉시 반영 확인
- [x] 리스트 이름/색상 변경이 사이드바에 즉시 반영되는지 확인

## Phase 5 — UI/UX 폴리싱 & 안정화
- [x] 전체 UI를 shadcn/ui 컴포넌트로 통일 (버튼, 체크박스, 모달, 토스트)
- [x] 로딩 상태 UI 처리 (skeleton 등)
- [x] 에러 상태 UI 처리 (toast/알림)
- [x] 빈 상태(empty state) UI 처리
- [x] 반응형 레이아웃 대응 (모바일에서 사이드바 토글)
- [x] 완료된 항목 정렬 로직 적용 (완료일 최신순 등)
- [x] 전체 기능 수동 QA
- [x] README 작성 (실행 방법: 백엔드 `:8080`, 프론트엔드 `:3000`)

### 완료 기준 검증
- [x] `spec.md` "9. 성공 기준(Acceptance Criteria)" 항목 전체 수동 테스트 통과

## 추가 작업
- [x] 필터로 요청/응답 전문 로깅 (`HttpLoggingFilter`: [Request]/[Header]/[Session]/[Response] 로그, 요청 로그 → 비즈니스 로직 → 응답 로그 순서, UUID request_id를 MDC에 넣어 요청 처리 중 모든 로그에 출력)
- [x] 서비스 계층 예외를 `BusinessException(ResultCode)`로 통일 (`ResponseStatusException` 직접 사용 제거, `ResultCode`에 HTTP 상태 추가, `GlobalExceptionHandler`에서 변환)
- [x] `HttpLoggingFilter` 적용 후 `/h2-console` 로그인 불가 수정: `/h2-console` 요청은 로깅 없이 `doFilter` 후 바로 반환 (본문을 미리 읽으면 H2 로그인 폼 파라미터가 비기 때문)
- [x] 완료일시(`completedAt`) 도입: 완료 시 기록/완료 취소 시 null, `completed` 스마트 뷰와 프론트 완료 항목을 `updatedAt` 대신 완료일시 최신순으로 정렬
- [x] 서비스가 응답 DTO를 반환하도록 변경하고 `spring.jpa.open-in-view: false` 설정: 엔티티→DTO 변환을 트랜잭션 안으로 옮겨 컨트롤러에서 지연 로딩하지 않게 함 (수정 응답에 갱신된 `updatedAt`이 담기도록 변환 전 flush)

---

# v2 — 향후 고려 사항 (spec.md 10번)

`plan.md`의 Phase 6~13을 기준으로 한다. 각 Phase에서 API를 바꾸면 `backend/openapi.yml`을 함께 갱신하고, 기능마다 테스트를 함께 작성한다.

## Phase 6 — 우선순위
### 백엔드
- [x] `Priority` enum 추가 (`NONE`, `LOW`, `MEDIUM`, `HIGH`)
- [x] `Reminder`에 `priority` 필드 추가 (기본값 `NONE`), `update(...)`에 우선순위 변경 포함
- [x] 생성/수정 요청 DTO와 응답 DTO에 `priority` 반영 (생략 시 `NONE`)
- [x] `openapi.yml` 갱신
- [x] 테스트: 도메인(기본값, 수정), 서비스(생성/수정 시 저장), 컨트롤러(응답 필드)

### 프론트엔드
- [x] `Reminder` 타입과 API 클라이언트에 `priority` 추가
- [x] 상세 편집 모달에 우선순위 선택 UI (`Select`)
- [x] 리마인더 항목 제목 앞에 `!` / `!!` / `!!!` 표시

### 완료 기준 검증
- [x] 우선순위 지정/변경 시 목록 표시가 즉시 바뀌고 새로고침 후에도 유지되는지 확인

## Phase 7 — 드래그앤드롭 순서 변경
### 백엔드
- [x] `ReminderList`, `Reminder`에 `sortOrder` 필드 추가
- [x] 새 리스트/리마인더 생성 시 같은 범위의 마지막 순서로 지정
- [x] `PATCH /api/lists/order` — `ids` 순서대로 리스트 순서 일괄 갱신
- [x] `PATCH /api/reminders/order` — `listId` + `ids` 순서대로 리마인더 순서 일괄 갱신
- [x] `ids`가 대상 범위의 항목과 일치하지 않으면 400 (`BusinessException`)
- [x] `GET /api/lists`, `GET /api/reminders?listId=` 정렬 기준을 `sortOrder`로 변경 (스마트 뷰 정렬 유지)
- [x] `openapi.yml` 갱신
- [x] 테스트: 생성 시 순서 부여, 순서 변경 반영, 잘못된 `ids` 400, 스마트 뷰 정렬 유지

### 프론트엔드
- [x] `@dnd-kit/core`, `@dnd-kit/sortable` 도입
- [x] 사이드바 리스트 드래그 정렬
- [x] 사용자 리스트 화면의 미완료 리마인더 드래그 정렬
- [x] 드롭 시 optimistic update, 실패 시 원래 순서 복구 + 토스트
- [x] 스마트 뷰에서 드래그 비활성화

### 완료 기준 검증
- [x] 리스트/리마인더 순서 변경 후 새로고침해도 순서가 유지되는지 확인

## Phase 8 — 태그
### 백엔드
- [x] `Tag` 엔티티(id, name 고유, createdAt)와 `TagRepository` 작성
- [x] `Reminder` ↔ `Tag` 다대다 연관관계 추가 (조인 테이블 `reminder_tag`)
- [x] 리마인더 생성/수정 요청에 `tagNames` 추가 (없는 태그 자동 생성), 응답에 태그 이름 목록 포함
- [x] `GET /api/tags` — 태그 목록 (미완료 리마인더 개수 포함, 사용되지 않는 태그 제외)
- [x] `DELETE /api/tags/{id}` — 태그 삭제 (리마인더는 유지)
- [x] `GET /api/reminders?tag=` — 태그별 리마인더 조회
- [x] `openapi.yml` 갱신
- [x] 테스트: 태그 자동 생성/재사용, 태그 교체, 태그별 조회, 태그 삭제 시 리마인더 유지

### 프론트엔드
- [x] 태그 API 클라이언트와 TanStack Query 훅 작성
- [x] 상세 편집 모달에 태그 칩 입력 UI (Enter로 추가, X로 제거)
- [x] 리마인더 항목에 `#태그` 표시
- [x] 사이드바 "태그" 섹션 추가, 태그 선택 시 해당 리마인더 표시 (선택 상태에 태그 추가)

### 완료 기준 검증
- [x] 태그를 붙이면 사이드바 태그 목록에 나타나고, 태그 선택 시 해당 리마인더만 보이는지 확인

## Phase 9 — 하위 작업(subtask)
### 백엔드
- [x] `Reminder`에 `parent` 자기 참조 연관관계 추가 (1단계 깊이, 부모와 같은 리스트)
- [x] 생성 요청에 `parentId` 추가 — 부모 없음 404, 부모가 하위 작업이면 400
- [x] 리스트별 조회는 최상위 리마인더만 반환하고 응답에 `subtasks` 포함
- [x] 스마트 뷰에 하위 작업도 개별 항목으로 포함
- [x] 부모 완료 시 하위 작업 모두 완료, 부모 삭제 시 하위 작업 함께 삭제
- [x] 리스트 리마인더 개수는 최상위 리마인더만 집계
- [x] `openapi.yml` 갱신
- [x] 테스트: 깊이 제한, 완료/삭제 전파, 조회 구조, 개수 집계

### 프론트엔드
- [x] 리마인더 항목 아래 하위 작업 들여쓰기 표시 + 펼치기/접기
- [x] 상세 편집 모달에서 하위 작업 추가
- [x] 하위 작업 완료 체크/삭제/편집 연동

### 완료 기준 검증
- [x] 하위 작업이 부모 아래에 표시되고, 부모 완료 시 하위 작업도 완료되는지 확인

## Phase 10 — 반복 리마인더
### 백엔드
- [x] `RepeatRule` enum 추가 (`NONE`, `DAILY`, `WEEKLY`, `MONTHLY`, `YEARLY`)
- [x] `Reminder`에 `repeatRule` 필드 추가 (기본값 `NONE`), 마감일시 없이 반복 설정 시 400
- [x] 다음 마감일시 계산 도메인 로직 (월말 처리 포함)
- [x] 반복 리마인더 완료 시 다음 회차 리마인더 생성 (제목/메모/플래그/우선순위/태그/리스트 복사)
- [x] 완료 취소 시 이미 생성된 다음 회차는 유지
- [x] 생성/수정 요청과 응답에 `repeatRule` 반영, `openapi.yml` 갱신
- [x] 테스트: 주기별 다음 날짜 계산(월말/윤년 포함), 완료 시 다음 회차 생성, 마감일 없는 반복 400

### 프론트엔드
- [x] 생성 폼과 상세 편집 모달에 반복 선택 UI
- [x] 리마인더 항목에 반복 아이콘과 주기 표시
- [x] 완료 시 다음 회차가 목록에 나타나도록 캐시 무효화

### 완료 기준 검증
- [x] 매주 반복 리마인더 완료 시 "완료됨"에 현재 항목, "예정됨"에 7일 뒤 새 항목이 나타나는지 확인

## Phase 11 — 알림(브라우저 알림)
### 백엔드
- [x] `GET /api/reminders/upcoming?from=&to=` — 기간 내 마감 미완료 리마인더 조회
- [x] `openapi.yml` 갱신
- [x] 테스트: 기간 경계, 완료 항목 제외

### 프론트엔드
- [x] 알림 권한 요청 UI (알림 켜기 버튼, 거부 상태 안내)
- [x] 주기적으로 다가오는 리마인더를 조회해 마감 시각에 브라우저 알림 표시
- [x] 알린 리마인더(id + dueAt)를 `localStorage`에 기록해 중복 알림 방지
- [x] 알림 클릭 시 해당 리마인더 상세 편집 열기

### 완료 기준 검증
- [x] 1~2분 뒤로 마감일시를 설정하면 그 시각에 알림이 한 번만 뜨는지 확인

## Phase 12 — 사용자 인증/멀티 유저 + 영구 저장(H2 파일 모드)
### 백엔드 — 인증
- [x] `spring-boot-starter-security` 의존성 추가
- [x] `User` 엔티티(id, email 고유, password BCrypt, name, createdAt)와 `UserRepository` 작성
- [x] 세션(쿠키) 기반 로그인 `SecurityConfig` 작성
- [x] `POST /api/auth/signup`, `POST /api/auth/login`, `POST /api/auth/logout`, `GET /api/auth/me`
- [x] 미인증 요청 401을 `ApiResponse` 형식으로 응답 (`ResultCode.UNAUTHORIZED` 추가)
- [x] CORS `allowCredentials` 설정 (또는 Next.js rewrites 프록시)
- [x] 세션 쿠키 `SameSite=Lax`, `HttpOnly` 명시 (CSRF 토큰 미사용 전제) + 쿠키 속성 테스트

### 백엔드 — 멀티 유저
- [x] `ReminderList`, `Reminder`, `Tag`에 소유자 `user` FK 추가
- [x] 태그 이름 고유 조건을 사용자별 고유로 변경
- [x] 모든 조회/수정/삭제를 현재 사용자 데이터로 제한 (다른 사용자 리소스는 404)
- [x] `openapi.yml` 갱신 (인증 API, 보안 스키마)
- [x] 테스트: 회원가입/로그인/로그아웃, 미인증 401, 사용자 간 데이터 격리

### 백엔드 — 영구 저장
- [x] H2를 파일 모드(`jdbc:h2:file:./data/reminderdb`)로 전환, 스키마는 `ddl-auto: update` 유지
- [x] 테스트는 in-memory DB 사용
- [x] DB 파일(`backend/data/`)을 `.gitignore`에 추가

### 프론트엔드
- [x] `/login`, `/signup` 페이지 (React Hook Form + 검증)
- [x] 비로그인 상태 접근 시 `/login`으로 이동
- [x] API 클라이언트 `credentials: "include"` 적용, 401 시 로그인 페이지로 이동
- [x] 사용자 이름 표시 + 로그아웃 버튼

### 완료 기준 검증
- [x] 두 계정이 서로의 리스트/리마인더를 볼 수 없는지 확인
- [x] 서버 재시작 후 데이터(계정 포함)가 유지되는지 확인
- [x] README에 DB 파일 위치와 초기화 방법 추가

## Phase 13 — 리스트 공유/협업
### 백엔드
- [x] `ListMember` 엔티티(id, list, user, role `OWNER`/`EDITOR`, createdAt) 작성, 리스트 생성 시 소유자를 `OWNER`로 저장
- [x] `GET /api/lists/{id}/members`
- [x] `POST /api/lists/{id}/members` — 이메일로 초대 (소유자만, 없는 사용자 404, 이미 멤버 400)
- [x] `DELETE /api/lists/{id}/members/{userId}` — 멤버 제거(소유자) / 본인 나가기
- [x] 접근 제어: 멤버는 리마인더 CRUD 가능, 리스트 수정/삭제·멤버 관리는 소유자만
- [x] `GET /api/lists`와 스마트 뷰에 공유받은 리스트/리마인더 포함
- [x] `openapi.yml` 갱신
- [x] 테스트: 초대/제거/나가기, 권한별 허용·거부, 비멤버 접근 차단, 스마트 뷰 포함

### 프론트엔드
- [x] 리스트 공유 다이얼로그 (이메일 초대, 멤버 목록, 제거)
- [x] 사이드바의 공유 리스트에 공유 아이콘 표시
- [x] 소유자가 아닌 경우 리스트 편집/삭제 메뉴 숨김
- [x] TanStack Query `refetchInterval`/창 포커스 재조회로 다른 사용자 변경 반영

### 완료 기준 검증
- [x] A가 B를 초대하면 B 사이드바에 리스트가 나타나고, B가 추가한 리마인더가 A 화면에 반영되는지 확인
- [x] 멤버가 아닌 사용자가 리스트에 접근할 수 없는지 확인

---

# v3 — 추가 개선 (spec.md 11번)

`plan.md`의 Phase 14~19를 기준으로 한다. Phase 14(CI) 이후 작업은 CI가 통과한 PR로 머지한다.

## Phase 14 — CI (GitHub Actions)
- [x] `backend/gradlew` 실행 권한 부여 (`git update-index --chmod=+x`)
- [x] `.github/workflows/ci.yml` 작성 (`pull_request`→`master`, `push`→`master` 트리거)
- [x] backend 잡: Temurin 21 + Gradle 캐시, `./gradlew test`, 실패 시 테스트 리포트 아티팩트 업로드
- [x] frontend 잡: Node 24 + npm 캐시, `npm ci` → `npm run lint` → `npm run build`
- [x] 현재 코드에서 lint/build/test가 모두 통과하는지 확인 (실패 항목 수정)
- [x] README에 CI 배지와 설명 추가
- [x] `master` 브랜치 보호 규칙에 필수 상태 검사 지정 (`gh api`, 관리자 우회 허용, 강제 푸시·삭제 금지)

### 완료 기준 검증
- [x] PR에서 두 잡이 실행되고, 일부러 실패시킨 테스트가 PR에 실패로 표시되는지 확인 (머지 차단·리포트 아티팩트 업로드 확인)

## Phase 15 — 완료 항목 숨기기/일괄 삭제
### 백엔드
- [ ] `DELETE /api/reminders/completed?listId=` — 완료 항목 일괄 삭제, `deletedCount` 반환
- [ ] 완료된 최상위 리마인더(하위 작업 포함)와 미완료 부모 아래의 완료된 하위 작업 삭제
- [ ] 접근 불가 리스트 404, `listId` 누락 400, 리스트 멤버(EDITOR)도 실행 가능
- [ ] `openapi.yml` 갱신
- [ ] 테스트: 완료 항목만 삭제, 다른 리스트 영향 없음, 하위 작업 처리, 멤버 허용/비멤버 404

### 프론트엔드
- [ ] 리스트 화면에 "완료된 항목 보기/숨기기" 토글 (리스트별 `localStorage` 저장, 기본 보기)
- [ ] "완료 항목 지우기" 버튼 + `AlertDialog` 확인 (개수 표시)
- [ ] 삭제 후 리마인더/리스트/태그 쿼리 무효화

### 완료 기준 검증
- [ ] 숨김 설정이 새로고침 후에도 유지되고, 일괄 삭제 시 해당 리스트의 완료 항목만 사라지는지 확인

## Phase 16 — 검색
### 백엔드
- [ ] `GET /api/reminders/search?q=` — 제목/메모 부분 일치(대소문자 무시) 또는 태그 이름 부분 일치
- [ ] 내 리스트 + 공유받은 리스트 범위, 하위 작업도 개별 항목으로 포함
- [ ] 정렬: 미완료(마감일 → 생성일) 먼저, 완료(완료일시 최신순) 나중
- [ ] `q` 공백 제거 후 1~100자 검증(위반 시 400), LIKE 특수문자 이스케이프
- [ ] `openapi.yml` 갱신
- [ ] 테스트: 제목/메모/태그 일치, 대소문자 무시, 특수문자, 다른 사용자 데이터 제외, 공유 리스트 포함, 빈 검색어 400

### 프론트엔드
- [ ] 검색 API 클라이언트와 TanStack Query 훅
- [ ] 사이드바 상단 검색 입력창 (300ms 디바운스)
- [ ] 선택 상태에 `search` 타입 추가, 검색어를 지우면 이전 선택으로 복귀
- [ ] 결과 항목에 소속 리스트 이름/색상 표시, 빈 결과 UI, 드래그 비활성화

### 완료 기준 검증
- [ ] 제목·메모·태그로 검색 시 일치 항목만 보이고 공유받은 리스트 항목도 검색되는지 확인

## Phase 17 — 리스트 간 이동
### 백엔드
- [ ] `Reminder.moveTo(list, sortOrder)` 도메인 메서드 (하위 작업도 함께 이동)
- [ ] `PATCH /api/reminders/{id}/list` — 대상 리스트 마지막 순서로 이동
- [ ] 접근 불가 대상 리스트 404, 하위 작업 단독 이동 400, 같은 리스트 이동은 변경 없음
- [ ] `openapi.yml` 갱신
- [ ] 테스트: 도메인(하위 작업 함께 이동), 서비스(순서, 개수 변화, 권한, 하위 작업 400), 컨트롤러

### 프론트엔드
- [ ] 리마인더 이동 API 클라이언트와 mutation
- [ ] 상세 편집 모달에 리스트 선택 UI (하위 작업 편집 시 숨김)
- [ ] 이동 후 원래/대상 리스트와 개수 쿼리 무효화 + 토스트

### 완료 기준 검증
- [ ] 이동 시 하위 작업도 함께 옮겨지고 사이드바 양쪽 개수가 즉시 바뀌는지 확인

## Phase 18 — 사용자 지정 반복
### 백엔드
- [ ] `Reminder`에 `repeatInterval`(1~99, 기본 1), `repeatDaysOfWeek`(`@ElementCollection`) 추가, 기존 행은 간격 1로 동작
- [ ] 반복 규칙 값 객체로 다음 마감일 계산 (간격, 주 단위 요일, 월말 규칙 유지)
- [ ] 다음 회차에 간격/요일 복사
- [ ] 생성/수정 요청·응답 반영, 간격 범위 위반·`WEEKLY` 외 요일 지정 시 400
- [ ] `openapi.yml` 갱신
- [ ] 테스트: 간격별 다음 날짜, 요일 반복(같은 주/다음 간격 주), 월말·윤년, 검증 400, 완료 시 다음 회차 규칙 복사

### 프론트엔드
- [ ] `Reminder` 타입과 API 클라이언트에 간격/요일 추가
- [ ] 반복 선택에 "평일마다" 프리셋과 "사용자 지정..." 항목
- [ ] 사용자 지정 다이얼로그 (간격 + 단위 + 요일 토글)
- [ ] 리마인더 항목 반복 요약 표시 ("2주마다 월·수", "평일마다")

### 완료 기준 검증
- [ ] "2주마다 월·수" 리마인더 완료 시 규칙에 맞는 다음 회차가 "예정됨"에 나타나는지 확인

## Phase 19 — 리스트 아이콘 + 그룹
### 백엔드
- [ ] `ReminderList`에 `icon` 추가, 생성/수정 요청·응답 반영 (허용 목록 밖이면 400)
- [ ] `ListGroup` 엔티티(id, user, name, sortOrder, createdAt)와 `ListGroupRepository`
- [ ] `ReminderList`에 `group` FK 추가, 리스트 응답에 `groupId` 포함
- [ ] `GET/POST/PUT/DELETE /api/list-groups` (삭제 시 소속 리스트는 그룹 해제만)
- [ ] `PATCH /api/list-groups/order` (`ids` 불일치 400)
- [ ] `PATCH /api/lists/{id}/group` — 소유자만, 다른 사용자의 그룹 404, `null`이면 그룹 해제
- [ ] `openapi.yml` 갱신
- [ ] 테스트: 아이콘 검증, 그룹 CRUD/순서, 그룹 삭제 시 리스트 유지, 소유자 외 거부, 사용자 간 격리

### 프론트엔드
- [ ] 리스트 생성/편집 다이얼로그에 아이콘 선택 그리드
- [ ] 사이드바 리스트 항목에 아이콘 표시
- [ ] 그룹 API 클라이언트와 TanStack Query 훅
- [ ] 사이드바 그룹(폴더) 표시 + 펼치기/접기 (`localStorage` 저장)
- [ ] 그룹 생성/이름 변경/삭제 UI, 리스트 메뉴 "그룹으로 이동"
- [ ] 그룹 순서 및 그룹 안 리스트 드래그 정렬

### 완료 기준 검증
- [ ] 아이콘 변경이 사이드바에 즉시 반영되는지 확인
- [ ] 리스트를 그룹으로 묶고 접을 수 있으며, 그룹 삭제 후에도 리스트와 리마인더가 남는지 확인
