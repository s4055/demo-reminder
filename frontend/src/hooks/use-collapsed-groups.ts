import { useSyncExternalStore } from "react"

/**
 * 사이드바에서 접어 둔 리스트 그룹을 localStorage에 기억한다.
 * 기본은 펼침이며, 접은 그룹의 id만 기록한다.
 * localStorage를 쓸 수 없는 환경(사생활 보호 모드 등)에서는 기록 없이 항상 펼친다.
 */

const STORAGE_KEY = "reminder-collapsed-list-groups"

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

function parseIds(raw: string | null): number[] {
  try {
    const parsed: unknown = raw ? JSON.parse(raw) : []
    return Array.isArray(parsed)
      ? parsed.filter((id): id is number => typeof id === "number")
      : []
  } catch {
    return []
  }
}

function setCollapsed(groupId: number, collapsed: boolean) {
  const ids = parseIds(getSnapshot()).filter((id) => id !== groupId)
  if (collapsed) ids.push(groupId)
  try {
    window.localStorage.setItem(STORAGE_KEY, JSON.stringify(ids))
  } catch {
    // 기록하지 못하면 설정이 바뀌지 않는다.
  }
  emit()
}

export function useCollapsedGroups(): {
  isCollapsed: (groupId: number) => boolean
  toggle: (groupId: number) => void
} {
  const raw = useSyncExternalStore(subscribe, getSnapshot, getServerSnapshot)
  const collapsed = parseIds(raw)
  return {
    isCollapsed: (groupId) => collapsed.includes(groupId),
    toggle: (groupId) => setCollapsed(groupId, !collapsed.includes(groupId)),
  }
}
