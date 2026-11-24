import { describe, expect, it } from "vitest"
import type { TransportItem } from "@grafana/faro-web-sdk"
import { createFaroConfig, sanitizeText, sanitizeTransportItem } from "./faro"

const JWT =
  "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIn0.c2lnbmF0dXJlLXZhbHVlLWZvci10ZXN0"

function exceptionItem(message: string, pageUrl: string): TransportItem {
  return {
    type: "exception",
    payload: {
      type: "Error",
      value: message,
      timestamp: "2026-11-24T00:00:00.000Z",
      stacktrace: {
        frames: [
          {
            filename: "http://localhost:3000/_next/static/chunks/app.js?dpl=abc",
            function: "render",
            lineno: 1,
            colno: 2,
          },
        ],
      },
    },
    meta: { page: { id: "/", url: pageUrl } },
  } as TransportItem
}

describe("createFaroConfig", () => {
  it("returnsNullWithoutUrl", () => {
    expect(createFaroConfig({})).toBeNull()
    expect(createFaroConfig({ url: "  " })).toBeNull()
  })

  it("setsAppInfo", () => {
    const config = createFaroConfig({
      url: "http://localhost:12347/collect",
      appName: "reminder",
      appVersion: "1.2.3",
      environment: "production",
    })

    expect(config?.url).toBe("http://localhost:12347/collect")
    expect(config?.app).toEqual({
      name: "reminder",
      version: "1.2.3",
      environment: "production",
    })
  })

  it("usesDefaultAppNameWhenMissing", () => {
    const config = createFaroConfig({ url: "http://localhost:12347/collect" })

    expect(config?.app.name).toBe("reminder-frontend")
  })

  it("collectsOnlyErrorsWebVitalsAndSession", () => {
    const config = createFaroConfig({ url: "http://localhost:12347/collect" })

    expect(config?.instrumentations?.map((instrumentation) => instrumentation.name)).toEqual([
      "@grafana/faro-web-sdk:instrumentation-errors",
      "@grafana/faro-web-sdk:instrumentation-web-vitals",
      "@grafana/faro-web-sdk:instrumentation-session",
    ])
  })

  it("doesNotSetUser", () => {
    const config = createFaroConfig({ url: "http://localhost:12347/collect" })

    expect(config?.user).toBeUndefined()
  })

  it("usesPathnameAsPageId", () => {
    const config = createFaroConfig({ url: "http://localhost:12347/collect" })
    const location = { pathname: "/login", search: "?next=/" } as Location

    expect(config?.pageTracking?.generatePageId?.(location)).toBe("/login")
  })
})

describe("sanitizeText", () => {
  it("removesUrlQueryAndFragment", () => {
    expect(sanitizeText("failed at http://localhost:3000/login?token=abc#top now")).toBe(
      "failed at http://localhost:3000/login now"
    )
  })

  it("removesRelativeUrlQueryAndFragment", () => {
    expect(sanitizeText("failed at /?token=abc and /lists/3?tab=done#top now")).toBe(
      "failed at / and /lists/3 now"
    )
  })

  it("removesRelativeUrlQueryInsideQuotesAndParentheses", () => {
    expect(sanitizeText(`fetch("/api/reminders?q=secret") (./page?x=1) url=../a?b=2`)).toBe(
      `fetch("/api/reminders") (./page) url=../a`
    )
  })

  it("keepsQuestionMarkInPlainText", () => {
    expect(sanitizeText("why did it fail? see /help")).toBe("why did it fail? see /help")
  })

  it("keepsUrlWithoutQuery", () => {
    expect(sanitizeText("http://localhost:3000/signup")).toBe("http://localhost:3000/signup")
  })

  it("masksBearerToken", () => {
    expect(sanitizeText("Authorization: Bearer abc.def-ghi")).toBe(
      "Authorization: Bearer [REDACTED]"
    )
  })

  it("masksJwt", () => {
    expect(sanitizeText(`token ${JWT} expired`)).toBe("token [REDACTED] expired")
  })

  it("masksEmail", () => {
    expect(sanitizeText("user user@example.com not found")).toBe("user [REDACTED] not found")
  })
})

describe("sanitizeTransportItem", () => {
  it("sanitizesPayloadAndMeta", () => {
    const item = exceptionItem(
      `Bearer ${JWT} rejected for user@example.com`,
      "http://localhost:3000/?listId=3"
    )

    const sanitized = sanitizeTransportItem(item)

    expect(sanitized.meta.page?.url).toBe("http://localhost:3000/")
    expect(JSON.stringify(sanitized)).not.toMatch(/eyJ|@example\.com|\?listId|\?dpl/)
    expect(sanitized.payload).toMatchObject({
      value: "Bearer [REDACTED] rejected for [REDACTED]",
      stacktrace: {
        frames: [{ filename: "http://localhost:3000/_next/static/chunks/app.js" }],
      },
    })
  })

  it("doesNotMutateOriginalItem", () => {
    const item = exceptionItem("plain", "http://localhost:3000/?listId=3")

    sanitizeTransportItem(item)

    expect(item.meta.page?.url).toBe("http://localhost:3000/?listId=3")
  })

  it("keepsNonStringValues", () => {
    const item = {
      type: "measurement",
      payload: { type: "web-vitals", values: { lcp: 1234.5 } },
      meta: { browser: { mobile: false } },
    } as unknown as TransportItem

    expect(sanitizeTransportItem(item)).toEqual(item)
  })
})
