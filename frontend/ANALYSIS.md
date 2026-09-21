# frontend 프로젝트 분석 (백엔드 개발자 + Vue 경험자용)

> 작성 기준: 2026-09-21, 커밋 `7599c5d` (Phase 5 완료 시점)

## 1. 한 줄 요약

**Next.js 16(App Router) + React 19 + TypeScript** 로 만든 Apple 미리 알림(Reminders) 웹 클론입니다. 서버(`localhost:8080`, Spring Boot)의 REST API를 호출하는 **SPA에 가까운 구조**이고, 소스는 약 2,600줄, 화면은 **단 하나(`/`)** 입니다.

## 2. 기술 스택 (Vue 대응표)

| 역할 | 이 프로젝트 | Vue 세계에서는 |
|---|---|---|
| 프레임워크 | **Next.js** | Nuxt |
| UI 라이브러리 | **React** | Vue |
| 서버 상태(API 캐시) | **TanStack Query** | Vue Query / Pinia + axios |
| 폼 | **react-hook-form** | VeeValidate / FormKit |
| 스타일 | **Tailwind CSS v4** | (동일) |
| UI 컴포넌트 | **shadcn/ui** (내부는 `@base-ui/react`) | Vuetify, PrimeVue 등 |
| 날짜 | date-fns, react-day-picker | 동일 |
| 토스트 | sonner | vue-toastification |
| 아이콘 | lucide-react | lucide-vue |

Vue의 Pinia/Vuex 같은 **전역 상태 라이브러리는 없습니다.** 서버 데이터는 TanStack Query가, 화면 상태(현재 선택 등)는 `useState`가 맡습니다.

## 3. 디렉터리 구조

```
frontend/src/
├─ app/                  ← Next.js 라우팅 영역
│  ├─ layout.tsx         전체 HTML 틀 (폰트, <Providers>)
│  ├─ page.tsx           "/" 페이지 → <RemindersApp /> 렌더만 함
│  ├─ providers.tsx      QueryClient(캐시) + 토스트 등록
│  └─ globals.css        Tailwind + 테마 색상 변수
├─ components/           화면 조각 (프로젝트 전용)
│  ├─ reminders-app.tsx  ★ 최상위 화면 (레이아웃 + 선택 상태)
│  ├─ sidebar.tsx        좌측 스마트리스트 + 내 리스트
│  ├─ reminder-list.tsx  리마인더 목록 + 항목
│  ├─ reminder-form.tsx  새 리마인더 입력
│  ├─ reminder-edit-dialog.tsx / list-form-dialog.tsx / list-delete-dialog.tsx
│  ├─ due-date-picker.tsx
│  └─ ui/                shadcn 범용 컴포넌트 (button, dialog, ...)
├─ hooks/                TanStack Query를 감싼 커스텀 훅
│  ├─ use-reminders.ts
│  └─ use-lists.ts
└─ lib/                  API 호출·순수 함수
   ├─ api.ts             fetch 공통 래퍼
   ├─ reminders-api.ts / lists-api.ts   엔드포인트별 함수 + 응답 타입
   ├─ selection.ts       "무엇을 보고 있나" 타입/헬퍼
   └─ due-date.ts        날짜 포맷 유틸
```

`@/` 는 `src/` 의 별칭입니다(`tsconfig.json`).

**백엔드 관점 매핑**: `lib/*-api.ts` ≈ 외부 API 클라이언트(+ DTO 타입), `hooks/` ≈ Service 계층, `components/` ≈ View. 레이어가 꽤 깔끔하게 나뉘어 있습니다.

## 4. React 핵심 개념 (Vue와 비교)

이 코드를 읽는 데 필요한 것만 추렸습니다.

**① 컴포넌트 = 함수.** `.vue`의 template/script/style 3분할이 없고, JSX(HTML 같은 문법)를 반환하는 함수 하나가 컴포넌트입니다.

**② props는 함수 인자.** `ReminderForm({ listId, defaultDueAt })`처럼 구조 분해로 받습니다. `emit` 대신 **콜백을 props로 내려줍니다** (`onSelect`, `onSaved`, `onOpenChange`).

**③ 상태는 `useState`.**

```tsx
const [selection, setSelection] = useState<Selection>(DEFAULT_SELECTION)
```

Vue의 `ref`와 비슷하지만 `.value`가 없고, **값을 직접 바꾸지 않고 `setSelection(새 값)`으로만 교체**합니다. 호출하면 컴포넌트 함수가 **처음부터 다시 실행**됩니다. Vue의 세밀한 반응성과 가장 다른 점입니다. 그래서 `computed`도 없이, 본문에서 그냥 계산합니다(`const title = ...`, `reminders-app.tsx:34`).

**④ 조건/반복 렌더링.** `v-if`는 `{cond && <X/>}` 또는 삼항, `v-for`는 `.map()` + `key` 입니다 (`sidebar.tsx:100`).

**⑤ 훅(`use...`).** `useState`, `useForm`, `useQuery` 등 `use`로 시작하는 함수는 Composition API의 composable과 같은 개념입니다. 컴포넌트 최상단에서만 호출해야 합니다.

**⑥ `"use client"`.** Next.js 고유 개념입니다. 기본은 **서버 컴포넌트**(서버에서만 실행)이고, `useState`나 클릭 이벤트가 필요한 파일은 맨 위에 `"use client"`를 써서 브라우저 컴포넌트로 만듭니다. 이 프로젝트는 거의 전부 클라이언트 컴포넌트이고, `layout.tsx`/`page.tsx`만 서버 컴포넌트입니다.

## 5. 앱의 동작 흐름

### 진입 경로

```
layout.tsx  → Providers(QueryClient, Toaster) → page.tsx → RemindersApp
```

`providers.tsx`가 전역 설정 지점입니다. 여기서 **모든 mutation 실패 시 공통 토스트**를 띄웁니다(`MutationCache.onError`). 백엔드의 `@ControllerAdvice`와 비슷한 역할입니다.

### 핵심 상태: `Selection`

`selection.ts`의 이 타입이 화면 전체를 조종합니다.

```ts
type Selection =
  | { type: "smart"; view: "today"|"scheduled"|"all"|"flagged"|"completed" }
  | { type: "list"; listId: number }
```

Java의 sealed interface 같은 **유니온 타입**입니다. `RemindersApp`이 `useState`로 보관하고, 사이드바에서 클릭하면 바뀌며, 이 값에 따라 제목·목록·API 호출이 결정됩니다.

라우터(URL)를 쓰지 않으므로 **새로고침하면 "전체" 뷰로 돌아갑니다.**

### 화면 구성

```
RemindersApp  (selection 상태 보유)
├─ Sidebar            (데스크톱: 고정 / 모바일: Sheet 슬라이드)
├─ 제목 h1
├─ ReminderForm       새 항목 추가
└─ ReminderList       목록
    └─ ReminderItem × N   (체크박스, 제목, 플래그, 삭제)
        → 클릭 시 ReminderEditDialog
```

상태를 부모(`RemindersApp`)에 두고 자식에게 내려주는 **상태 끌어올리기(lifting state up)** 패턴입니다.

### 데이터 흐름 (예: 체크박스 클릭)

```
Checkbox 클릭
 → toggleComplete.mutate(id)                      [hooks/use-reminders.ts]
 → PATCH /api/reminders/{id}/complete             [lib/reminders-api.ts → api.ts]
 → 성공 시 invalidateQueries(["reminders"], ["lists"])
 → TanStack Query가 해당 조회를 자동 재요청
 → 화면 자동 갱신 (사이드바 개수 배지 포함)
```

**핵심은 "서버 데이터를 로컬에 복사해 직접 수정하지 않는다"** 는 점입니다. 변경 요청 후 캐시를 무효화(invalidate)하면 다시 GET을 받아옵니다. 백엔드 개발자에게 익숙한 "서버가 진실의 원천(source of truth)" 모델입니다.

`useQuery`의 `queryKey`(`["reminders", selection]`)는 캐시 키입니다. `selection`이 바뀌면 키가 달라져서 자동으로 새 요청이 나갑니다. Vue의 `watch` + 재요청을 직접 안 써도 되는 이유입니다.

## 6. 파일별로 볼 만한 포인트

- **`lib/api.ts`**: `fetch` 래퍼. `NEXT_PUBLIC_API_BASE_URL` 환경변수(기본 `http://localhost:8080`), 실패 시 `Error` throw, 204는 `undefined` 반환. 브라우저가 직접 호출하므로 백엔드 **CORS 설정이 필요**합니다.
- **`lib/reminders-api.ts`**: `Reminder` 타입이 백엔드 응답 DTO(record)와 1:1 대응합니다. `backend/openapi.yml`과 맞춰서 봐야 합니다. 이 타입은 손으로 작성한 것이라 백엔드가 바뀌면 수동 동기화가 필요합니다.
- **`lib/due-date.ts`**: 서버 `LocalDateTime`(타임존 없는 문자열)을 그대로 로컬 시각으로 다루는 규칙이 주석으로 명시돼 있습니다.
- **`reminders-app.tsx`**: `key={selectionKey(selection)}`로 `ReminderForm`을 뷰 전환 때 **강제 재생성(입력값 초기화)** 합니다. React의 `key`는 반복문 외에도 "이 키가 바뀌면 컴포넌트를 새로 만든다"는 용도로 씁니다.
- **`reminder-edit-dialog.tsx`, `list-form-dialog.tsx`**: `react-hook-form`으로 폼 처리(`register`로 입력 연결, `validate`로 검증). 커스텀 컴포넌트(날짜 선택, 체크박스)는 `Controller`로 연결합니다. 수정/생성 겸용 폼(`list` prop 유무로 분기)도 볼만합니다.
- **`reminder-list.tsx`**: 로딩(Skeleton) → 에러(재시도 버튼) → 빈 상태 → 목록의 순서로 **early return** 하는 패턴입니다. `sortForDisplay`는 미완료 → 완료(최근 수정순) 정렬을 클라이언트에서 합니다.

## 7. 스타일과 UI 컴포넌트

- **Tailwind**: CSS 파일 대신 `className="flex items-center gap-2 ..."`처럼 유틸리티 클래스를 HTML에 직접 씁니다. 처음엔 지저분해 보이지만 `.vue`의 `<style scoped>`가 필요 없어집니다.
- **`components/ui/`**: shadcn/ui는 라이브러리를 설치하는 게 아니라 **컴포넌트 소스 코드를 프로젝트에 복사**해 두는 방식입니다. 그래서 `ui/` 안 파일은 우리 코드이며 수정해도 됩니다. `button.tsx`의 `cva(...)`는 `variant`/`size` prop별 클래스를 정의하는 도구입니다.
- **`cn(...)`**: 조건부로 클래스를 합치고 Tailwind 충돌을 정리하는 헬퍼입니다(`clsx` + `tailwind-merge` 대체 패키지 `cn`, shadcn 저장소에서 배포).
- **다크 모드**: 색상은 CSS 변수 기반으로 준비돼 있습니다(`globals.css`). `next-themes`는 토스트(`ui/sonner.tsx`의 `useTheme()`)에서만 쓰이고, `providers.tsx`에 `ThemeProvider`가 없어서 테마는 항상 `"system"`(OS 설정 따름)입니다. 사용자가 직접 전환하는 UI는 없습니다.
- **반응형**: `hidden md:block` 같은 접두사로 처리합니다(`md:` = 768px 이상). 모바일에서는 사이드바가 슬라이드 메뉴가 됩니다.

## 8. 주의할 점

1. **AGENTS.md 경고**: 이 Next.js는 학습 데이터와 다른 **최신 버전(16.x)** 이라, 코드 작성 전에 `node_modules/next/dist/docs/`의 문서를 확인하라고 되어 있습니다. `LayoutProps<"/">` 같은 타입은 Next가 자동 생성하는 전역 타입입니다.
2. **테스트 코드가 없습니다.** `package.json`에 test 스크립트나 테스트 라이브러리가 없습니다. 백엔드는 테스트를 필수로 하는 관례인데 프론트는 비어 있습니다.
3. **`@base-ui/react`**: 흔한 shadcn(Radix 기반)과 달리 Base UI를 씁니다. 예를 들어 `PopoverTrigger render={<Button />}`처럼 `asChild` 대신 `render` prop을 쓰는 게 차이입니다.
4. **정리 대상**: `README.md`는 create-next-app 기본 템플릿이고, `public/`의 Next 기본 SVG들(`next.svg`, `vercel.svg` 등)은 코드에서 참조되는지 확인이 필요합니다.

## 9. 추천 읽기 순서

1. `lib/selection.ts` → 앱의 핵심 개념 이해
2. `lib/reminders-api.ts` + `lib/api.ts` → 백엔드 API와의 접점
3. `hooks/use-reminders.ts` → TanStack Query 패턴
4. `components/reminders-app.tsx` → 전체 조립
5. `components/reminder-list.tsx` → 가장 전형적인 React 컴포넌트
6. `components/reminder-edit-dialog.tsx` → 폼 처리
