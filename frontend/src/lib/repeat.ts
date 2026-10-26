export type RepeatRule = "NONE" | "DAILY" | "WEEKLY" | "MONTHLY" | "YEARLY"

export type DayOfWeek =
  | "MONDAY"
  | "TUESDAY"
  | "WEDNESDAY"
  | "THURSDAY"
  | "FRIDAY"
  | "SATURDAY"
  | "SUNDAY"

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

export const REPEAT_UNIT_LABELS: Record<Exclude<RepeatRule, "NONE">, string> = {
  DAILY: "일",
  WEEKLY: "주",
  MONTHLY: "개월",
  YEARLY: "년",
}

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

export function toRepeatFields(recurrence: Recurrence) {
  return {
    repeatRule: recurrence.rule,
    repeatInterval: recurrence.interval,
    repeatDaysOfWeek:
      recurrence.rule === "WEEKLY" ? sortDays(recurrence.daysOfWeek) : [],
  }
}
