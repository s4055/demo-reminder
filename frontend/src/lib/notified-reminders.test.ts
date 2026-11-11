// @vitest-environment jsdom
import { beforeEach, describe, expect, it, vi } from "vitest"
import { hasNotified, markNotified, notificationKey } from "@/lib/notified-reminders"

const STORAGE_KEY = "reminder-notified"

beforeEach(() => {
  window.localStorage.clear()
})

describe("notificationKey", () => {
  it("combinesIdAndDueAt", () => {
    expect(notificationKey({ id: 3, dueAt: "2026-09-20T18:30:00" })).toBe(
      "3@2026-09-20T18:30:00"
    )
  })
})

describe("markNotified", () => {
  it("remembersNotifiedKey", () => {
    markNotified("1@a")

    expect(hasNotified("1@a")).toBe(true)
    expect(hasNotified("2@a")).toBe(false)
  })

  it("doesNotDuplicateKey", () => {
    markNotified("1@a")
    markNotified("1@a")

    expect(JSON.parse(window.localStorage.getItem(STORAGE_KEY) ?? "[]")).toEqual(["1@a"])
  })

  it("keepsOnlyLatest200Keys", () => {
    for (let i = 0; i < 201; i++) markNotified(`${i}@a`)

    expect(hasNotified("0@a")).toBe(false)
    expect(hasNotified("1@a")).toBe(true)
    expect(hasNotified("200@a")).toBe(true)
  })

  it("ignoresStorageFailure", () => {
    vi.spyOn(Storage.prototype, "setItem").mockImplementation(() => {
      throw new Error("quota exceeded")
    })

    expect(() => markNotified("1@a")).not.toThrow()
  })
})

describe("hasNotified", () => {
  it("treatsMalformedStorageAsEmpty", () => {
    window.localStorage.setItem(STORAGE_KEY, "{not json")

    expect(hasNotified("1@a")).toBe(false)
  })

  it("ignoresNonStringEntries", () => {
    window.localStorage.setItem(STORAGE_KEY, JSON.stringify([1, "1@a", null]))

    expect(hasNotified("1@a")).toBe(true)
  })
})
