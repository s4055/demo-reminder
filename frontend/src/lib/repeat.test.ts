import { describe, expect, it } from "vitest"
import {
  NO_REPEAT,
  WEEKDAYS_REPEAT,
  isRepeating,
  isSameRecurrence,
  recurrenceOf,
  simpleRecurrence,
  sortDays,
  summarizeRecurrence,
  toRepeatFields,
  type Recurrence,
} from "@/lib/repeat"

describe("recurrenceOf", () => {
  it("mapsReminderRepeatFields", () => {
    expect(
      recurrenceOf({
        repeatRule: "WEEKLY",
        repeatInterval: 2,
        repeatDaysOfWeek: ["FRIDAY", "MONDAY"],
      })
    ).toEqual({ rule: "WEEKLY", interval: 2, daysOfWeek: ["FRIDAY", "MONDAY"] })
  })
})

describe("isRepeating", () => {
  it("isFalseOnlyForNone", () => {
    expect(isRepeating("NONE")).toBe(false)
    expect(isRepeating("DAILY")).toBe(true)
    expect(isRepeating("YEARLY")).toBe(true)
  })
})

describe("sortDays", () => {
  it("ordersFromMondayToSunday", () => {
    expect(sortDays(["SUNDAY", "WEDNESDAY", "MONDAY"])).toEqual([
      "MONDAY",
      "WEDNESDAY",
      "SUNDAY",
    ])
  })
})

describe("isSameRecurrence", () => {
  it("ignoresDayOrder", () => {
    const a: Recurrence = { rule: "WEEKLY", interval: 1, daysOfWeek: ["FRIDAY", "MONDAY"] }
    const b: Recurrence = { rule: "WEEKLY", interval: 1, daysOfWeek: ["MONDAY", "FRIDAY"] }

    expect(isSameRecurrence(a, b)).toBe(true)
  })

  it("comparesRuleIntervalAndDays", () => {
    const base: Recurrence = { rule: "WEEKLY", interval: 1, daysOfWeek: ["MONDAY"] }

    expect(isSameRecurrence(base, { ...base, rule: "DAILY" })).toBe(false)
    expect(isSameRecurrence(base, { ...base, interval: 2 })).toBe(false)
    expect(isSameRecurrence(base, { ...base, daysOfWeek: ["TUESDAY"] })).toBe(false)
  })
})

describe("summarizeRecurrence", () => {
  it.each<[Recurrence, string]>([
    [NO_REPEAT, "반복 안 함"],
    [simpleRecurrence("DAILY"), "매일"],
    [simpleRecurrence("YEARLY"), "매년"],
    [{ rule: "MONTHLY", interval: 3, daysOfWeek: [] }, "3개월마다"],
    [{ rule: "DAILY", interval: 2, daysOfWeek: [] }, "2일마다"],
    [WEEKDAYS_REPEAT, "평일마다"],
    [{ rule: "WEEKLY", interval: 1, daysOfWeek: ["FRIDAY", "MONDAY"] }, "매주 월·금"],
    [{ rule: "WEEKLY", interval: 2, daysOfWeek: ["SUNDAY"] }, "2주마다 일"],
    [{ rule: "WEEKLY", interval: 1, daysOfWeek: [] }, "매주"],
  ])("summarizes %o as %s", (recurrence, expected) => {
    expect(summarizeRecurrence(recurrence)).toBe(expected)
  })

  it("doesNotTreatBiweeklyWeekdaysAsWeekdays", () => {
    expect(summarizeRecurrence({ ...WEEKDAYS_REPEAT, interval: 2 })).toBe(
      "2주마다 월·화·수·목·금"
    )
  })
})

describe("toRepeatFields", () => {
  it("sendsSortedDaysForWeekly", () => {
    expect(
      toRepeatFields({ rule: "WEEKLY", interval: 2, daysOfWeek: ["FRIDAY", "MONDAY"] })
    ).toEqual({
      repeatRule: "WEEKLY",
      repeatInterval: 2,
      repeatDaysOfWeek: ["MONDAY", "FRIDAY"],
    })
  })

  it("dropsDaysForNonWeeklyRule", () => {
    expect(
      toRepeatFields({ rule: "MONTHLY", interval: 1, daysOfWeek: ["MONDAY"] })
    ).toEqual({ repeatRule: "MONTHLY", repeatInterval: 1, repeatDaysOfWeek: [] })
  })
})
