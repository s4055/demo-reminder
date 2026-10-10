export type RepeatRule = "NONE" | "DAILY" | "WEEKLY" | "MONTHLY" | "YEARLY"

export type DayOfWeek =
  | "MONDAY"
  | "TUESDAY"
  | "WEDNESDAY"
  | "THURSDAY"
  | "FRIDAY"
  | "SATURDAY"
  | "SUNDAY"

// 반복 규칙(주기 + 간격 + 요일). 요일은 WEEKLY일 때만 지정하며, 비어 있으면 마감일의 요일로 반복한다.
export type Recurrence = {
  rule: RepeatRule
  interval: number
  daysOfWeek: DayOfWeek[]
}

export const REPEAT_INTERVAL_MIN = 1
export const REPEAT_INTERVAL_MAX = 99

export const REPEAT_RULES: RepeatRule[] = [
  "NONE",
  "DAILY",
  "WEEKLY",
  "MONTHLY",
  "YEARLY",
]

export const REPEAT_RULE_LABELS: Record<RepeatRule, string> = {
  NONE: "반복 안 함",
  DAILY: "매일",
  WEEKLY: "매주",
  MONTHLY: "매월",
  YEARLY: "매년",
}

// "N일마다"처럼 간격 뒤에 붙는 단위
export const REPEAT_UNIT_LABELS: Record<Exclude<RepeatRule, "NONE">, string> = {
  DAILY: "일",
  WEEKLY: "주",
  MONTHLY: "개월",
  YEARLY: "년",
}

// 월요일부터 (서버 응답도 이 순서로 정렬된다)
export const DAYS_OF_WEEK: DayOfWeek[] = [
  "MONDAY",
  "TUESDAY",
  "WEDNESDAY",
  "THURSDAY",
  "FRIDAY",
  "SATURDAY",
  "SUNDAY",
]

export const DAY_OF_WEEK_LABELS: Record<DayOfWeek, string> = {
  MONDAY: "월",
  TUESDAY: "화",
  WEDNESDAY: "수",
  THURSDAY: "목",
  FRIDAY: "금",
  SATURDAY: "토",
  SUNDAY: "일",
}

export const WEEKDAYS: DayOfWeek[] = DAYS_OF_WEEK.slice(0, 5)

export const NO_REPEAT: Recurrence = { rule: "NONE", interval: 1, daysOfWeek: [] }

// "평일마다" 프리셋: 매주 월~금
export const WEEKDAYS_REPEAT: Recurrence = {
  rule: "WEEKLY",
  interval: 1,
  daysOfWeek: WEEKDAYS,
}

export function isRepeating(rule: RepeatRule): boolean {
  return rule !== "NONE"
}

export function recurrenceOf(reminder: {
  repeatRule: RepeatRule
  repeatInterval: number
  repeatDaysOfWeek: DayOfWeek[]
}): Recurrence {
  return {
    rule: reminder.repeatRule,
    interval: reminder.repeatInterval,
    daysOfWeek: reminder.repeatDaysOfWeek,
  }
}

// 간격 1, 요일 지정 없이 주기만 정한 반복
export function simpleRecurrence(rule: RepeatRule): Recurrence {
  return { rule, interval: 1, daysOfWeek: [] }
}

export function sortDays(days: DayOfWeek[]): DayOfWeek[] {
  return DAYS_OF_WEEK.filter((day) => days.includes(day))
}

export function isSameRecurrence(a: Recurrence, b: Recurrence): boolean {
  return (
    a.rule === b.rule &&
    a.interval === b.interval &&
    sortDays(a.daysOfWeek).join() === sortDays(b.daysOfWeek).join()
  )
}

// 반복을 한 줄로 요약한다. 예: "매주", "3일마다", "2주마다 월·수", "평일마다"
export function summarizeRecurrence(recurrence: Recurrence): string {
  const { rule, interval, daysOfWeek } = recurrence
  if (rule === "NONE") return REPEAT_RULE_LABELS.NONE
  if (isSameRecurrence(recurrence, WEEKDAYS_REPEAT)) return "평일마다"
  const base =
    interval === 1
      ? REPEAT_RULE_LABELS[rule]
      : `${interval}${REPEAT_UNIT_LABELS[rule]}마다`
  if (rule !== "WEEKLY" || daysOfWeek.length === 0) return base
  const days = sortDays(daysOfWeek)
    .map((day) => DAY_OF_WEEK_LABELS[day])
    .join("·")
  return `${base} ${days}`
}

// API 요청 본문에 넣을 반복 필드
export function toRepeatFields(recurrence: Recurrence) {
  return {
    repeatRule: recurrence.rule,
    repeatInterval: recurrence.interval,
    repeatDaysOfWeek:
      recurrence.rule === "WEEKLY" ? sortDays(recurrence.daysOfWeek) : [],
  }
}
