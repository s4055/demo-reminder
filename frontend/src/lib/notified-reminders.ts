import type { Reminder } from "@/lib/reminders-api"

/**
 * 이미 브라우저 알림을 띄운 리마인더를 localStorage에 기록해 중복 알림을 막는다.
 * 키는 id + dueAt 조합이라 마감일시를 바꾸면 새 마감일시에 다시 알린다.
 * localStorage를 쓸 수 없는 환경(사생활 보호 모드 등)에서는 기록 없이 동작한다.
 */

const STORAGE_KEY = "reminder-notified"
// 오래된 기록이 끝없이 쌓이지 않도록 최근 것만 남긴다.
const MAX_ENTRIES = 200

export function notificationKey(reminder: Pick<Reminder, "id" | "dueAt">): string {
  return `${reminder.id}@${reminder.dueAt}`
}

function readEntries(): string[] {
  try {
    const raw = window.localStorage.getItem(STORAGE_KEY)
    const parsed: unknown = raw ? JSON.parse(raw) : []
    return Array.isArray(parsed)
      ? parsed.filter((entry): entry is string => typeof entry === "string")
      : []
  } catch {
    return []
  }
}

export function hasNotified(key: string): boolean {
  return readEntries().includes(key)
}

export function markNotified(key: string): void {
  try {
    const entries = readEntries().filter((entry) => entry !== key)
    entries.push(key)
    window.localStorage.setItem(
      STORAGE_KEY,
      JSON.stringify(entries.slice(-MAX_ENTRIES))
    )
  } catch {
    // 기록하지 못해도 알림 자체는 동작한다.
  }
}
