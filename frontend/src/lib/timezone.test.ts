import { describe, expect, it, vi } from "vitest"
import { browserTimeZone } from "@/lib/timezone"

describe("browserTimeZone", () => {
  it("returnsIanaZoneOfRuntime", () => {
    expect(browserTimeZone()).toBe("Asia/Seoul")
  })

  it("followsChangedRuntimeZone", () => {
    vi.stubEnv("TZ", "America/New_York")

    expect(browserTimeZone()).toBe("America/New_York")
  })
})
