import { describe, expect, it, vi } from "vitest"
import { formatDueAt, formatDueDate, isOverdue, toDueAtParam } from "@/lib/due-date"

describe("toDueAtParam", () => {
  it("sendsLocalTimeAsUtcInstant", () => {
    expect(toDueAtParam(new Date(2026, 8, 20, 18, 30, 5))).toBe("2026-09-20T09:30:05.000Z")
  })

  it("movesToPreviousUtcDateBeforeNineInSeoul", () => {
    expect(toDueAtParam(new Date(2026, 0, 2, 3, 4, 5))).toBe("2026-01-01T18:04:05.000Z")
  })
})

describe("formatDueDate", () => {
  it("formatsInKorean", () => {
    expect(formatDueDate(new Date(2026, 8, 20, 18, 30))).toBe("9월 20일 (일) 18:30")
  })
})

describe("formatDueAt", () => {
  it("showsUtcStringInBrowserZone", () => {
    expect(formatDueAt("2026-09-20T09:30:00Z")).toBe("9월 20일 (일) 18:30")
  })

  it("showsSameInstantFromOtherOffsetIdentically", () => {
    expect(formatDueAt("2026-09-20T05:30:00-04:00")).toBe("9월 20일 (일) 18:30")
    expect(formatDueAt("2026-09-19T23:05:00Z")).toBe("9월 20일 (일) 08:05")
  })

  it("showsSameInstantInOtherBrowserZone", () => {
    vi.stubEnv("TZ", "America/New_York")

    expect(formatDueAt("2026-09-20T09:30:00Z")).toBe("9월 20일 (일) 05:30")
  })
})

describe("isOverdue", () => {
  const now = new Date("2026-09-20T09:30:00Z")

  it("isTrueBeforeNow", () => {
    expect(isOverdue("2026-09-20T09:29:59Z", now)).toBe(true)
    expect(isOverdue("2026-09-20T18:29:59+09:00", now)).toBe(true)
  })

  it("isFalseAtOrAfterNow", () => {
    expect(isOverdue("2026-09-20T09:30:00Z", now)).toBe(false)
    expect(isOverdue("2026-09-20T18:30:00+09:00", now)).toBe(false)
    expect(isOverdue("2026-09-20T15:00:00Z", now)).toBe(false)
  })
})
