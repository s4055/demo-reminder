/**
 * react-query 캐시 키를 한 곳에서 관리하는 파일
 */

export const listsQueryKey = ["lists"] as const // 쿼리 캐시를 ["lists"] 선언
export const remindersQueryKey = ["reminders"] as const // 쿼리 캐시를 ["reminders"] 선언
export const tagsQueryKey = ["tags"] as const // 쿼리 캐시를 ["tags"] 선언
export const meQueryKey = ["auth", "me"] as const // 쿼리 캐시를 ["auth", "me"] 선언 (로그인한 사용자)

// 리스트 멤버 목록. listsQueryKey로 시작하므로 리스트를 무효화하면 함께 다시 조회된다.
export function listMembersQueryKey(listId: number) {
  return [...listsQueryKey, listId, "members"] as const
}

// 공유 리스트에서 다른 멤버의 변경을 반영하기 위해 리스트/리마인더/태그를 주기적으로 다시 조회하는 간격.
// 창으로 돌아올 때도 다시 조회한다 (TanStack Query 기본값 refetchOnWindowFocus). 실시간(WebSocket) 동기화는 범위 밖이다.
export const SYNC_INTERVAL_MS = 10_000
