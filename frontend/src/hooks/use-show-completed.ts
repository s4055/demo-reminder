import { useSyncExternalStore } from "react"

/**
 * 리스트 화면에서 완료된 항목을 보일지를 리스트별로 localStorage에 기억한다.
 * 기본은 보이기이며, 숨긴 리스트의 id만 기록한다.
 * localStorage를 쓸 수 없는 환경(사생활 보호 모드 등)에서는 기록 없이 항상 보인다.
 */

const STORAGE_KEY = "reminder-hidden-completed-lists"

const listeners = new Set<() => void>()

function emit() {
  listeners.forEach((listener) => listener())
}

// 다른 탭에서 바꾼 설정도 반영한다.
function onStorage(event: StorageEvent) {
  if (event.key === STORAGE_KEY) emit()
}

function subscribe(listener: () => void) {
  listeners.add(listener)
  window.addEventListener("storage", onStorage)
  return () => {
    listeners.delete(listener)
    window.removeEventListener("storage", onStorage)
  }
}

// 스냅샷은 원본 문자열로 돌려줘야 값이 같을 때 같은 참조가 되어 불필요한 렌더링이 없다.
function getSnapshot(): string | null {
  try {
    return window.localStorage.getItem(STORAGE_KEY)
  } catch {
    return null
  }
}

function getServerSnapshot(): string | null {
  return null
}

function parseHiddenIds(raw: string | null): number[] {
  try {
    const parsed: unknown = raw ? JSON.parse(raw) : []
    return Array.isArray(parsed)
      ? parsed.filter((id): id is number => typeof id === "number")
      : []
  } catch {
    return []
  }
}

function setHidden(listId: number, hidden: boolean) {
  const ids = parseHiddenIds(getSnapshot()).filter((id) => id !== listId)
  if (hidden) ids.push(listId)
  try {
    window.localStorage.setItem(STORAGE_KEY, JSON.stringify(ids))
  } catch {
    // 기록하지 못하면 설정이 바뀌지 않는다.
  }
  emit()
}

// listId가 null(스마트 뷰/태그 화면)이면 항상 보인다.
export function useShowCompleted(
  listId: number | null
): [boolean, (show: boolean) => void] {
  const raw = useSyncExternalStore(subscribe, getSnapshot, getServerSnapshot)
  const show = listId === null || !parseHiddenIds(raw).includes(listId)
  const setShow = (next: boolean) => {
    if (listId !== null) setHidden(listId, !next)
  }
  return [show, setShow]
}
