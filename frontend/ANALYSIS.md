# frontend 프로젝트 분석 (백엔드 개발자 + Vue 경험자용)

> 작성 기준: 2026-10-11, 커밋 `3399597` 이후 Phase 20~21 작업분 (v3 Phase 19까지 기능 완료 + 사이드바 분리)

## 1. 한 줄 요약

**Next.js 16(App Router) + React 19 + TypeScript** 로 만든 Apple 미리 알림(Reminders) 웹 클론입니다. 서버(`localhost:8080`, Spring Boot)의 REST API를 세션 쿠키로 호출하는 **SPA에 가까운 구조**이고, 소스는 약 6,900줄(shadcn `ui/` 제외 약 5,600줄), 화면은 **메인(`/`), 로그인(`/login`), 회원가입(`/signup`)** 세 개입니다.

## 2. 기술 스택 (Vue 대응표)

| 역할 | 이 프로젝트 | Vue 세계에서는 |
|---|---|---|
| 프레임워크 | **Next.js** | Nuxt |
| UI 라이브러리 | **React** | Vue |
| 서버 상태(API 캐시) | **TanStack Query** | Vue Query / Pinia + axios |
| 폼 | **react-hook-form** | VeeValidate / FormKit |
| 드래그 정렬 | **@dnd-kit** (core, sortable) | vuedraggable |
| 스타일 | **Tailwind CSS v4** | (동일) |
| UI 컴포넌트 | **shadcn/ui** (내부는 `@base-ui/react`) | Vuetify, PrimeVue 등 |
| 날짜 | date-fns, react-day-picker | 동일 |
| 토스트 | sonner | vue-toastification |
| 아이콘 | lucide-react | lucide-vue |

Vue의 Pinia/Vuex 같은 **전역 상태 라이브러리는 없습니다.** 서버 데이터는 TanStack Query가, 화면 상태(현재 선택, 다이얼로그 열림 등)는 `useState`가, 새로고침 후에도 남길 사용자 설정(그룹 접힘, 완료 항목 보기, 알린 리마인더)은 `localStorage`가 맡습니다.

`shadcn`은 컴포넌트 소스를 복사해 주는 CLI라 `devDependencies`에 있습니다. `globals.css`의 `@import "shadcn/tailwind.css"`는 빌드 시에 번들됩니다.

## 3. 디렉터리 구조

```
frontend/src/
├─ app/                        ← Next.js 라우팅 영역
│  ├─ layout.tsx               전체 HTML 틀 (폰트, <Providers>)
│  ├─ page.tsx                 "/" → <AuthGate><RemindersApp /></AuthGate>
│  ├─ login/page.tsx, signup/page.tsx   로그인/회원가입 화면
│  ├─ providers.tsx            QueryClient(캐시) + 공통 에러 토스트 + 401 처리
│  └─ globals.css              Tailwind + 테마 색상 변수
├─ components/                 화면 조각 (프로젝트 전용)
│  ├─ auth-gate.tsx            로그인 확인 후 children 표시, 아니면 /login
│  ├─ auth-forms.tsx           로그인/회원가입 폼
│  ├─ reminders-app.tsx        ★ 최상위 화면 (레이아웃 + 선택 상태 + 검색 + 알림)
│  ├─ sidebar/                 좌측 사이드바 (Phase 21에서 분리)
│  │  ├─ index.tsx             조립 + 리스트/그룹 다이얼로그 상태
│  │  ├─ sidebar-search.tsx    검색창
│  │  ├─ smart-views.tsx       오늘/예정됨/전체/플래그/완료됨
│  │  ├─ list-section.tsx      "나의 리스트" (그룹 + 리스트, DndContext)
│  │  ├─ use-sidebar-dnd.ts    드래그 센서/범위 제한/드롭 시 순서 계산
│  │  ├─ sortable-group.tsx, sortable-list-item.tsx   드래그 가능한 그룹/리스트 항목
│  │  ├─ list-item-content.tsx, move-to-group-menu.tsx   리스트 표시와 메뉴
│  │  ├─ shared-list-section.tsx   공유받은 리스트
│  │  ├─ tag-section.tsx       태그 목록
│  │  └─ sidebar-item.tsx      공통 항목 버튼
│  ├─ reminder-list.tsx        리마인더 목록 + 항목 + 하위 작업 + 드래그 정렬
│  ├─ reminder-form.tsx        새 리마인더 입력
│  ├─ reminder-edit-dialog.tsx 상세 편집 (메모/마감일/반복/우선순위/태그/리스트 이동/하위 작업)
│  ├─ repeat-select.tsx, due-date-picker.tsx, tag-input.tsx   편집 폼 부품
│  ├─ list-form-dialog.tsx / list-delete-dialog.tsx / list-share-dialog.tsx / list-group-dialogs.tsx
│  ├─ clear-completed-dialog.tsx, notification-toggle.tsx, user-menu.tsx, list-icon-mark.tsx
│  └─ ui/                      shadcn 범용 컴포넌트 (button, dialog, ...)
├─ hooks/                      TanStack Query를 감싼 커스텀 훅 + 브라우저 상태 훅
│  ├─ query-keys.ts            캐시 키와 동기화 주기(SYNC_INTERVAL_MS) 한 곳 관리
│  ├─ use-reminders.ts, use-lists.ts, use-list-groups.ts, use-tags.ts, use-auth.ts
│  ├─ use-reminder-notifications.ts, use-notification-permission.ts   브라우저 알림
│  └─ use-collapsed-groups.ts, use-show-completed.ts, use-debounced-value.ts
└─ lib/                        API 호출·순수 함수
   ├─ api.ts                   fetch 공통 래퍼 (ApiResponse 해제, ApiError, 401 핸들러)
   ├─ reminders-api.ts / lists-api.ts / list-groups-api.ts / tags-api.ts / auth-api.ts
   ├─ selection.ts             "무엇을 보고 있나" 타입/헬퍼
   ├─ due-date.ts, repeat.ts, priority.ts, list-icons.ts   표시용 규칙
   └─ notified-reminders.ts    알린 리마인더 기록(localStorage)
```

`@/` 는 `src/` 의 별칭입니다(`tsconfig.json`).

**백엔드 관점 매핑**: `lib/*-api.ts` ≈ 외부 API 클라이언트(+ DTO 타입), `hooks/` ≈ Service 계층, `components/` ≈ View. 레이어가 꽤 깔끔하게 나뉘어 있습니다.

## 4. React 핵심 개념 (Vue와 비교)

이 코드를 읽는 데 필요한 것만 추렸습니다.

**① 컴포넌트 = 함수.** `.vue`의 template/script/style 3분할이 없고, JSX(HTML 같은 문법)를 반환하는 함수 하나가 컴포넌트입니다.

**② props는 함수 인자.** `ReminderForm({ listId, defaultDueAt })`처럼 구조 분해로 받습니다. `emit` 대신 **콜백을 props로 내려줍니다** (`onSelect`, `onSaved`, `onOpenChange`). 사이드바의 `ListSection`이 `onEditList(list)`를 호출하면 `sidebar/index.tsx`가 다이얼로그를 여는 식입니다.

**③ 상태는 `useState`.**

```tsx
const [baseSelection, setBaseSelection] = useState<Selection>(DEFAULT_SELECTION)
```

Vue의 `ref`와 비슷하지만 `.value`가 없고, **값을 직접 바꾸지 않고 `setXxx(새 값)`으로만 교체**합니다. 호출하면 컴포넌트 함수가 **처음부터 다시 실행**됩니다. Vue의 세밀한 반응성과 가장 다른 점입니다. 그래서 `computed`도 없이 본문에서 그냥 계산합니다(`reminders-app.tsx`의 `selection`, `title`).

**④ 조건/반복 렌더링.** `v-if`는 `{cond && <X/>}` 또는 삼항, `v-for`는 `.map()` + `key` 입니다 (`sidebar/smart-views.tsx`).

**⑤ 훅(`use...`).** `useState`, `useForm`, `useQuery` 등 `use`로 시작하는 함수는 Composition API의 composable과 같은 개념입니다. 컴포넌트 최상단에서만 호출해야 합니다. `sidebar/use-sidebar-dnd.ts`처럼 컴포넌트에서 로직만 떼어 낸 커스텀 훅도 같은 규칙을 따릅니다.

**⑥ `"use client"`.** Next.js 고유 개념입니다. 기본은 **서버 컴포넌트**(서버에서만 실행)이고, `useState`나 클릭 이벤트가 필요한 파일은 맨 위에 `"use client"`를 써서 브라우저 컴포넌트로 만듭니다. `"use client"` 파일이 import하는 파일도 클라이언트 쪽으로 함께 묶이므로, `sidebar/` 안의 하위 파일은 지시어 없이도 클라이언트에서 실행됩니다. `layout.tsx`와 각 `page.tsx`만 서버 컴포넌트입니다.

## 5. 앱의 동작 흐름

### 진입 경로와 인증

```
layout.tsx → Providers(QueryClient, Toaster) → page.tsx → AuthGate → RemindersApp
                                               login/page.tsx → LoginForm
```

- 인증은 **세션 쿠키(JSESSIONID)** 입니다. `api.ts`가 모든 요청에 `credentials: "include"`를 붙입니다.
- `AuthGate`가 `GET /api/auth/me`(`useMe`)로 로그인 여부를 확인하고, 401이면 `/login`으로 보냅니다.
- 사용 중 세션이 만료되어 일반 API가 401을 받으면, `providers.tsx`에 등록된 핸들러가 캐시를 비우고 `/login`으로 보냅니다.
- `providers.tsx`는 **모든 mutation 실패 시 공통 토스트**도 띄웁니다(`MutationCache.onError`, `meta.errorMessage`/`meta.silent`로 조정). 백엔드의 `@ControllerAdvice`와 비슷한 역할입니다.

### 핵심 상태: `Selection`

`selection.ts`의 이 타입이 화면 전체를 조종합니다.

```ts
type Selection =
  | { type: "smart"; view: "today"|"scheduled"|"all"|"flagged"|"completed" }
  | { type: "list"; listId: number }
  | { type: "tag"; name: string }
  | { type: "search"; query: string }
```

Java의 sealed interface 같은 **유니온 타입**입니다. `RemindersApp`은 사이드바에서 고른 `baseSelection`과 검색어를 따로 보관하고, 검색어가 있으면(300ms 디바운스) `search` 선택으로 덮어씁니다. 검색어를 지우면 `baseSelection`으로 돌아갑니다. 이 값에 따라 제목·목록·API 호출이 결정됩니다.

라우터(URL)를 쓰지 않으므로 **새로고침하면 "전체" 뷰로 돌아갑니다.**

### 화면 구성

```
RemindersApp  (selection / 검색어 / 알림 클릭 편집 상태 보유)
├─ Sidebar (sidebar/index.tsx)    데스크톱: 고정 / 모바일: Sheet 슬라이드
│  ├─ SidebarSearch
│  ├─ SmartViews
│  ├─ ListSection                 그룹(SortableGroup) + 리스트(SortableListItem), 드래그 정렬
│  ├─ SharedListSection           공유받은 리스트
│  ├─ TagSection
│  └─ 리스트/그룹/공유 다이얼로그
├─ 제목 h1 (리스트 색상)
├─ ReminderForm                   새 항목 추가 (검색 화면에서는 숨김)
└─ ReminderList                   목록 (리스트 화면에서만 드래그 정렬, 완료 항목 보기/지우기)
    └─ ReminderItem × N (+ 하위 작업)
        → 클릭 시 ReminderEditDialog
```

상태를 부모에 두고 자식에게 내려주는 **상태 끌어올리기(lifting state up)** 패턴입니다. 사이드바에서는 여러 섹션이 여는 다이얼로그 상태를 `sidebar/index.tsx`에 모았고, 데이터(리스트·그룹·태그)는 각 섹션이 훅으로 직접 조회합니다. 같은 쿼리 키를 여러 컴포넌트가 써도 TanStack Query가 요청을 하나로 합칩니다.

### 데이터 흐름 (예: 체크박스 클릭)

```
Checkbox 클릭
 → toggleComplete.mutate(id)                      [hooks/use-reminders.ts]
 → PATCH /api/reminders/{id}/complete             [lib/reminders-api.ts → api.ts]
 → 성공 시 invalidateQueries(["reminders"], ["lists"], ["tags"])
 → TanStack Query가 해당 조회를 자동 재요청
 → 화면 자동 갱신 (사이드바 개수 배지, 반복 리마인더의 다음 회차 포함)
```

**핵심은 "서버 데이터를 로컬에 복사해 직접 수정하지 않는다"** 는 점입니다. 변경 요청 후 캐시를 무효화(invalidate)하면 다시 GET을 받아옵니다. 예외는 **드래그 정렬**로, 끌어 놓는 즉시 순서가 바뀌어 보이도록 캐시를 먼저 고치고(optimistic update) 실패하면 원래 순서로 되돌린 뒤 토스트를 띄웁니다(`useReorderLists`, `useReorderListGroups`, `useReorderReminders`).

`useQuery`의 `queryKey`(`["reminders", selection]`)는 캐시 키입니다. `selection`이 바뀌면 키가 달라져서 자동으로 새 요청이 나갑니다. Vue의 `watch` + 재요청을 직접 안 써도 되는 이유입니다.

공유 리스트에서 다른 멤버가 바꾼 내용은 `refetchInterval`(10초, `SYNC_INTERVAL_MS`)과 창 포커스 시 재조회로 반영합니다. 실시간(WebSocket) 동기화는 없습니다.

## 6. 파일별로 볼 만한 포인트

- **`lib/api.ts`**: `fetch` 래퍼. `NEXT_PUBLIC_API_BASE_URL` 환경변수(기본 `http://localhost:8080`). 모든 응답이 `ApiResponse { resultCode, resultMsg, data }`이므로 `data`만 꺼내 돌려주고, 실패하면 HTTP 상태와 `resultCode`를 담은 `ApiError`를 throw합니다. 화면에서 `error.status === 409`처럼 구분합니다(회원가입 이메일 중복 등).
- **`lib/reminders-api.ts`**: `Reminder` 타입이 백엔드 응답 DTO(record)와 1:1 대응합니다. `backend/openapi.yml`과 맞춰서 봐야 하며, 손으로 작성한 타입이라 백엔드가 바뀌면 수동 동기화가 필요합니다.
- **`lib/due-date.ts`, `lib/repeat.ts`**: 서버 `LocalDateTime`(타임존 없는 문자열)을 로컬 시각으로 다루는 규칙, "2주마다 월·수"/"평일마다" 같은 반복 요약 문구를 만드는 순수 함수입니다.
- **`reminders-app.tsx`**: `key={selectionKey(selection)}`로 `ReminderForm`을 뷰 전환 때 **강제 재생성(입력값 초기화)** 합니다. React의 `key`는 반복문 외에도 "이 키가 바뀌면 컴포넌트를 새로 만든다"는 용도로 씁니다. 렌더 중에 `setBaseSelection`을 부르는 부분(보던 리스트가 사라지면 기본 화면으로)도 React가 허용하는 "렌더 중 상태 조정" 패턴입니다.
- **`sidebar/use-sidebar-dnd.ts`**: 그룹 순서와 그룹 안 리스트 순서를 한 `DndContext`에서 다루므로 드래그 id에 `group-`/`list-` 접두사를 붙이고, 충돌 판정과 키보드 이동을 같은 범위로 제한합니다. 리스트 순서는 서버에서 그룹과 관계없는 전체 순서라, 그룹 안에서 바꾼 순서를 전체 순서에 다시 끼워 넣어 보냅니다.
- **`reminder-edit-dialog.tsx`, `list-form-dialog.tsx`**: `react-hook-form`으로 폼 처리(`register`로 입력 연결, `validate`로 검증). 커스텀 컴포넌트(날짜 선택, 반복 선택, 태그 입력)는 `Controller`로 연결합니다. 수정/생성 겸용 폼(`list` prop 유무로 분기)도 볼만합니다.
- **`reminder-list.tsx`**: 로딩(Skeleton) → 에러(재시도 버튼) → 빈 상태 → 목록의 순서로 **early return** 하는 패턴입니다. 미완료 → 완료(완료일시 최신순) 정렬, 하위 작업 펼치기, 리스트 화면 전용 드래그 정렬이 들어 있습니다.
- **`use-reminder-notifications.ts`**: 다가오는 리마인더를 백그라운드에서도 주기적으로 조회해 마감 시각에 브라우저 알림을 띄우고, 알린 항목(id + dueAt)을 `localStorage`에 남겨 중복 알림을 막습니다.

## 7. 스타일과 UI 컴포넌트

- **Tailwind**: CSS 파일 대신 `className="flex items-center gap-2 ..."`처럼 유틸리티 클래스를 HTML에 직접 씁니다. 처음엔 지저분해 보이지만 `.vue`의 `<style scoped>`가 필요 없어집니다. 사이드바의 `group/item`, `md:group-hover/item:opacity-100`은 "항목에 마우스를 올리면 버튼 표시"를 CSS만으로 처리하는 Tailwind 그룹 기능입니다.
- **`components/ui/`**: shadcn/ui는 라이브러리를 설치하는 게 아니라 **컴포넌트 소스 코드를 프로젝트에 복사**해 두는 방식입니다. 그래서 `ui/` 안 파일은 우리 코드이며 수정해도 됩니다. `button.tsx`의 `cva(...)`는 `variant`/`size` prop별 클래스를 정의하는 도구입니다.
- **`cn(...)`**: 조건부로 클래스를 합치고 Tailwind 충돌을 정리하는 헬퍼입니다(`clsx` + `tailwind-merge` 대체 패키지 `cn`, shadcn 저장소에서 배포). 런타임 의존성입니다.
- **다크 모드**: 색상은 CSS 변수 기반으로 준비돼 있습니다(`globals.css`). `next-themes`는 토스트(`ui/sonner.tsx`의 `useTheme()`)에서만 쓰이고, `providers.tsx`에 `ThemeProvider`가 없어서 테마는 항상 `"system"`(OS 설정 따름)입니다. 사용자가 직접 전환하는 UI는 없습니다.
- **반응형**: `hidden md:block` 같은 접두사로 처리합니다(`md:` = 768px 이상). 모바일에서는 사이드바가 슬라이드 메뉴가 되고, 마우스를 올려야 보이던 버튼이 항상 보입니다.

## 8. 주의할 점

1. **AGENTS.md 경고**: 이 Next.js는 학습 데이터와 다른 **최신 버전(16.x)** 이라, 코드 작성 전에 `node_modules/next/dist/docs/`의 문서를 확인하라고 되어 있습니다. `LayoutProps<"/">` 같은 타입은 Next가 자동 생성하는 전역 타입입니다.
2. **테스트 코드가 없습니다.** `package.json`에 test 스크립트나 테스트 라이브러리가 없고, CI(`.github/workflows/ci.yml`)는 `npm run lint`와 `npm run build`만 확인합니다. 백엔드는 테스트를 필수로 하는 관례인데 프론트는 비어 있습니다.
3. **`@base-ui/react`**: 흔한 shadcn(Radix 기반)과 달리 Base UI를 씁니다. 예를 들어 `PopoverTrigger render={<Button />}`처럼 `asChild` 대신 `render` prop을 쓰는 게 차이입니다(`sidebar/move-to-group-menu.tsx`).
4. **정리 대상**: `frontend/README.md`는 create-next-app 기본 템플릿이고(실행 방법은 루트 `README.md`에 있음), `public/`의 Next 기본 SVG들(`next.svg`, `vercel.svg` 등)은 코드에서 참조되지 않습니다.
5. **큰 컴포넌트**: 사이드바는 Phase 21에서 나눴지만 `reminder-list.tsx`(약 560줄), `reminder-edit-dialog.tsx`(약 410줄)는 아직 한 파일입니다.

## 9. 추천 읽기 순서

1. `lib/selection.ts` → 앱의 핵심 개념 이해
2. `lib/api.ts` + `lib/reminders-api.ts` → 백엔드 API와의 접점
3. `hooks/use-reminders.ts` + `hooks/query-keys.ts` → TanStack Query 패턴
4. `components/auth-gate.tsx` + `app/providers.tsx` → 인증과 전역 설정
5. `components/reminders-app.tsx` → 전체 조립
6. `components/sidebar/index.tsx` → `list-section.tsx` → `use-sidebar-dnd.ts` → 컴포넌트 분리와 드래그 정렬
7. `components/reminder-list.tsx` → 가장 전형적인 React 컴포넌트
8. `components/reminder-edit-dialog.tsx` → 폼 처리
