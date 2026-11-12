import { format, isBefore, parseISO } from "date-fns"
import { ko } from "date-fns/locale"

// 서버는 시각을 UTC 기준 ISO-8601 문자열("2026-09-20T09:30:00Z")로 주고받는다.
// 파싱한 Date는 같은 순간을 가리키므로, 표시는 브라우저 시간대로 하고 요청에는 toISOString()으로 UTC 문자열을 보낸다.
const DUE_AT_DISPLAY_FORMAT = "M월 d일 (EEE) HH:mm"

export function toDueAtParam(date: Date): string {
  return date.toISOString()
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
