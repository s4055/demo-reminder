# PRD: Apple Reminders 웹 클론 (demoreminder)

## 1. 개요
Apple Reminders 앱의 핵심 사용 경험(리스트 관리, 할 일 등록/완료, 스마트 뷰)을
웹에서 재현하는 데모 프로젝트. 단일 사용자 기준으로 인증 없이 동작하며,
Spring Boot REST API + Next.js 프론트엔드로 구성한다.

## 2. 목표
- Apple Reminders의 핵심 UX(리스트, 리마인더, 완료 처리, 스마트 리스트)를 웹에서 사용 가능하게 구현
- 기존 Spring Boot(JPA/H2) 백엔드를 API 서버로 확장
- Next.js(App Router, latest) + Tailwind CSS + shadcn/ui로 프론트엔드 구현

## 3. 범위 (v1 / MVP)
### In-Scope
- 리스트(List) 생성/수정/삭제/조회
- 리마인더(Reminder) 생성/수정/삭제/조회, 완료 토글
- 리마인더 속성: 제목, 메모, 마감일시, 플래그(flagged), 완료 여부, 소속 리스트
- 스마트 리스트(자동 필터 뷰):
  - **오늘(Today)**: 마감일이 오늘인 미완료 리마인더
  - **예정됨(Scheduled)**: 마감일이 설정된 모든 미완료 리마인더
  - **전체(All)**: 모든 미완료 리마인더
  - **플래그 지정됨(Flagged)**: flagged=true 인 미완료 리마인더
  - **완료됨(Completed)**: completed=true 인 리마인더
- 리스트별 리마인더 개수 표시(사이드바)

### Out-of-Scope (v1 이후 고려)
- 사용자 인증/멀티 유저
- 태그, 우선순위 세분화, 하위 작업(subtask)
- 리스트 공유(협업)
- 자연어 날짜 입력, 알림/푸시
- 첨부파일, 위치 기반 리마인더
- 드래그앤드롭 정렬

## 4. 사용자 스토리
- 사용자로서, 새 리스트를 만들어 할 일을 카테고리별로 관리하고 싶다.
- 사용자로서, 리마인더에 마감일시를 설정해 "오늘"/"예정됨" 뷰에서 확인하고 싶다.
- 사용자로서, 중요한 리마인더에 플래그를 지정해 빠르게 찾고 싶다.
- 사용자로서, 완료한 항목을 체크하면 "완료됨" 뷰로 이동시키고 싶다.
- 사용자로서, 리마인더/리스트를 수정하거나 삭제하고 싶다.

## 5. 데이터 모델
### List
| 필드 | 타입 | 설명 |
|---|---|---|
| id | Long | PK |
| name | String | 리스트 이름 |
| color | String | 표시 색상 (hex, 선택) |
| createdAt | LocalDateTime | 생성 일시 |

### Reminder
| 필드 | 타입 | 설명 |
|---|---|---|
| id | Long | PK |
| title | String | 제목 (필수) |
| memo | String | 메모 (선택) |
| dueAt | LocalDateTime | 마감 일시 (선택) |
| flagged | boolean | 플래그 여부 |
| completed | boolean | 완료 여부 |
| listId | Long | 소속 리스트 (FK) |
| createdAt | LocalDateTime | 생성 일시 |
| updatedAt | LocalDateTime | 수정 일시 |

관계: List 1 --- N Reminder

## 6. API 설계 (Spring Boot, REST/JSON)
Base path: `/api`

### Lists
| Method | Path | 설명 |
|---|---|---|
| GET | /api/lists | 리스트 전체 조회 (리마인더 개수 포함) |
| POST | /api/lists | 리스트 생성 |
| PUT | /api/lists/{id} | 리스트 수정 |
| DELETE | /api/lists/{id} | 리스트 삭제 (소속 리마인더 함께 삭제) |

### Reminders
| Method | Path | 설명 |
|---|---|---|
| GET | /api/reminders?listId= | 리스트별 리마인더 조회 |
| GET | /api/reminders/smart/{view} | 스마트 뷰 조회 (today, scheduled, all, flagged, completed) |
| POST | /api/reminders | 리마인더 생성 |
| PUT | /api/reminders/{id} | 리마인더 수정 |
| PATCH | /api/reminders/{id}/complete | 완료 토글 |
| PATCH | /api/reminders/{id}/flag | 플래그 토글 |
| DELETE | /api/reminders/{id} | 리마인더 삭제 |

- 인증 없음 (전체 공개 API)
- CORS: Next.js 개발 서버(localhost:3000) 허용

## 7. 프론트엔드 설계 (Next.js latest, App Router)
- **스타일링**: Tailwind CSS + shadcn/ui
- **레이아웃**: 좌측 사이드바(스마트 리스트 + 사용자 리스트, 개수 뱃지) + 우측 리마인더 목록/상세
- **데이터 페칭**: 클라이언트 컴포넌트에서 fetch 기반 (React state) 또는 SWR/TanStack Query 중 택1
- **주요 화면**
  - 사이드바: 오늘/예정됨/전체/플래그 지정됨/완료됨 + 사용자 리스트 목록 + 리스트 추가 버튼
  - 리마인더 목록: 선택된 리스트/스마트뷰의 리마인더를 카드/체크리스트 형태로 표시, 완료 체크박스
  - 리마인더 상세/편집: 제목, 메모, 마감일시, 플래그 편집 (모달 또는 우측 패널)
  - 리스트 관리: 리스트 이름/색상 편집, 삭제

## 8. 비기능 요구사항
- 백엔드: Spring Boot 4, Gradle, Java 21, H2 in-memory (재시작 시 데이터 초기화됨을 감수)
- 프론트엔드: Next.js latest, TypeScript
- 로컬 개발: 백엔드 `:8080`, 프론트엔드 `:3000`, CORS 허용 설정 필요
- 인증 없음 → 별도 보안 요구사항 없음 (데모 목적)

## 9. 성공 기준 (Acceptance Criteria)
- 리스트를 생성/삭제할 수 있고 사이드바에 실시간 반영된다.
- 리마인더를 생성하면 소속 리스트와 해당 스마트 뷰(오늘/예정됨 등)에 정확히 나타난다.
- 완료 체크 시 해당 항목이 "완료됨" 뷰로 이동하고, 다른 미완료 뷰에서는 사라진다.
- 플래그 토글 시 "플래그 지정됨" 뷰에 정확히 반영된다.
- 리마인더/리스트 수정·삭제가 즉시 화면에 반영된다.

## 10. 향후 고려 사항 (Out of Scope, 참고용)
- 사용자 인증 및 멀티 유저 지원 (H2 파일 모드로 영구 저장 포함)
- 하위 작업(subtask), 태그, 우선순위
- 리스트 공유/협업
- 알림(브라우저 푸시), 반복 리마인더
- 드래그앤드롭 순서 변경

> 위 항목은 v2(plan.md Phase 6~13)에서 모두 구현되었다.

## 11. v3 범위 (추가 개선)
v2 이후 Apple Reminders 사용 경험에 가까워지도록 기능을 보강하고, 변경을 안전하게 쌓을 수 있도록 CI를 갖춘다.
세부 계획은 `plan.md` Phase 14~19, 체크리스트는 `tasks.md`를 따른다.

### In-Scope
- **CI**: PR/`master` 푸시마다 백엔드 테스트와 프론트엔드 lint/build를 GitHub Actions로 자동 실행
- **완료 항목 관리**: 리스트 화면에서 완료 항목 보기/숨기기, 완료 항목 일괄 삭제
- **검색**: 제목/메모/태그로 접근 가능한 모든 리마인더 검색
- **리스트 간 이동**: 리마인더를 다른 리스트로 옮기기 (하위 작업 함께 이동)
- **사용자 지정 반복**: "N일/주/개월/년마다", 매주 특정 요일 반복 (예: 2주마다, 평일마다)
- **리스트 아이콘과 그룹**: 리스트에 아이콘 지정, 여러 리스트를 그룹(폴더)으로 묶어 사이드바 정리

### Out-of-Scope (v3)
- 반복 종료 조건(종료일/횟수), "매월 첫째 월요일" 같은 서수 요일 반복
- 그룹 간 드래그앤드롭 이동 (메뉴로 이동), 공유받은 리스트의 그룹 지정
- 검색어 하이라이트, 전문 검색 엔진(형태소 분석 등)
- 배포 파이프라인(CD), Docker 이미지 빌드

### 데이터 모델 변경
| 대상 | 필드 | 설명 |
|---|---|---|
| Reminder | repeatInterval | 반복 간격 (1~99, 기본 1) |
| Reminder | repeatDaysOfWeek | 매주 반복할 요일 집합 (`WEEKLY`일 때만, 비어 있으면 마감일의 요일) |
| ReminderList | icon | 아이콘 키 (선택) |
| ReminderList | group | 소속 그룹 (선택, FK) |
| ListGroup (신규) | id, user, name, sortOrder, createdAt | 소유자별 리스트 그룹 |

### API 추가/변경
| Method | Path | 설명 |
|---|---|---|
| DELETE | /api/reminders/completed?listId= | 리스트의 완료 항목 일괄 삭제 |
| GET | /api/reminders/search?q= | 리마인더 검색 |
| PATCH | /api/reminders/{id}/list | 리마인더를 다른 리스트로 이동 |
| POST/PUT | /api/reminders, /api/reminders/{id} | `repeatInterval`, `repeatDaysOfWeek` 추가 |
| POST/PUT | /api/lists, /api/lists/{id} | `icon` 추가 |
| PATCH | /api/lists/{id}/group | 리스트의 그룹 지정/해제 |
| GET/POST/PUT/DELETE | /api/list-groups, /api/list-groups/{id} | 리스트 그룹 CRUD |
| PATCH | /api/list-groups/order | 그룹 순서 일괄 변경 |

### 성공 기준
- PR을 올리면 CI가 실행되고, 테스트나 빌드가 실패하면 PR에 실패로 표시된다.
- 완료 항목을 숨기거나 일괄 삭제할 수 있고, 다른 리스트의 항목에는 영향이 없다.
- 검색어를 입력하면 내 리스트와 공유받은 리스트의 리마인더 중 일치하는 항목만 보인다.
- 리마인더를 다른 리스트로 옮기면 하위 작업과 함께 이동하고 양쪽 리스트 개수가 즉시 갱신된다.
- "2주마다 월·수" 반복 리마인더를 완료하면 규칙에 맞는 다음 회차가 생성된다.
- 리스트를 그룹으로 묶으면 사이드바에 그룹 아래 표시되고, 그룹을 삭제해도 리스트는 남는다.
