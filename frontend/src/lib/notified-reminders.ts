import type { Reminder } from "@/lib/reminders-api"

const STORAGE_KEY = "reminder-notified"
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
  }
}
