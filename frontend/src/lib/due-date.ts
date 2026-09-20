import { format, isBefore, parseISO } from "date-fns"
import { ko } from "date-fns/locale"

// 서버의 LocalDateTime 은 타임존 없는 문자열("2026-09-20T18:30:00")이므로 로컬 시각으로 그대로 다룬다.
const DUE_AT_FORMAT = "yyyy-MM-dd'T'HH:mm:ss"
const DUE_AT_DISPLAY_FORMAT = "M월 d일 (EEE) HH:mm"

export function toDueAtParam(date: Date): string {
  return format(date, DUE_AT_FORMAT)
}

export function formatDueDate(date: Date): string {
  return format(date, DUE_AT_DISPLAY_FORMAT, { locale: ko })
}

export function formatDueAt(dueAt: string): string {
  return formatDueDate(parseISO(dueAt))
}

export function isOverdue(dueAt: string, now: Date = new Date()): boolean {
  return isBefore(parseISO(dueAt), now)
}
