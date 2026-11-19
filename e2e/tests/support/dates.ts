export const SEOUL = "Asia/Seoul"

const WEEKDAY_KEYS = ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"]
const WEEKDAY_LABELS = ["일", "월", "화", "수", "목", "금", "토"]

export type CalendarDate = {
  year: number
  month: number
  day: number
}

function partsOf(date: Date, timeZone: string) {
  const parts = new Intl.DateTimeFormat("en-US", {
    timeZone,
    year: "numeric",
    month: "numeric",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
    hourCycle: "h23",
    weekday: "short",
  }).formatToParts(date)
  const value = (type: string) => parts.find((part) => part.type === type)?.value ?? ""
  return {
    year: Number(value("year")),
    month: Number(value("month")),
    day: Number(value("day")),
    hour: value("hour"),
    minute: value("minute"),
    weekday: WEEKDAY_KEYS.indexOf(value("weekday")),
  }
}

export function formatDueAt(date: Date, timeZone: string = SEOUL): string {
  const { month, day, hour, minute, weekday } = partsOf(date, timeZone)
  return `${month}월 ${day}일 (${WEEKDAY_LABELS[weekday]}) ${hour}:${minute}`
}

export function calendarDateOf(date: Date, timeZone: string = SEOUL): CalendarDate {
  const { year, month, day } = partsOf(date, timeZone)
  return { year, month, day }
}

export function seoulDate(daysFromToday: number, hour: number, now: Date = new Date()): Date {
  const { year, month, day } = partsOf(now, SEOUL)
  return new Date(Date.UTC(year, month - 1, day + daysFromToday, hour - 9))
}

export function minutesFrom(date: Date, minutes: number): Date {
  return new Date(date.getTime() + minutes * 60_000)
}
