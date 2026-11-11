import { describe, expect, it } from "vitest"
import { formatDueAt, formatDueDate, isOverdue, toDueAtParam } from "@/lib/due-date"

describe("toDueAtParam", () => {
  it("formatsLocalTimeWithoutZone", () => {
    expect(toDueAtParam(new Date(2026, 8, 20, 18, 30, 5))).toBe("2026-09-20T18:30:05")
  })

  it("padsSingleDigitFields", () => {
    expect(toDueAtParam(new Date(2026, 0, 2, 3, 4, 5))).toBe("2026-01-02T03:04:05")
  })
})

describe("formatDueDate", () => {
  it("formatsInKorean", () => {
    expect(formatDueDate(new Date(2026, 8, 20, 18, 30))).toBe("9월 20일 (일) 18:30")
  })
})

describe("formatDueAt", () => {
  it("parsesServerStringAsLocalTime", () => {
    expect(formatDueAt("2026-09-20T08:05:00")).toBe("9월 20일 (일) 08:05")
  })
})

describe("isOverdue", () => {
  const now = new Date(2026, 8, 20, 18, 30)

  it("isTrueBeforeNow", () => {
    expect(isOverdue("2026-09-20T18:29:59", now)).toBe(true)
  })

  it("isFalseAtOrAfterNow", () => {
    expect(isOverdue("2026-09-20T18:30:00", now)).toBe(false)
    expect(isOverdue("2026-09-21T00:00:00", now)).toBe(false)
  })
})
