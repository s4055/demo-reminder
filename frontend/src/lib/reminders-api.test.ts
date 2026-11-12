import { beforeEach, describe, expect, it, vi } from "vitest"
import {
  createReminder,
  getCompletedReminders,
  getReminder,
  getReminderIfExists,
  getSmartReminders,
  getUpcomingReminders,
  searchReminders,
} from "@/lib/reminders-api"

const BASE_URL = "http://localhost:8080"

function stubFetch(status: number, body: unknown) {
  const fetchMock = vi.fn<typeof fetch>(async () =>
    new Response(JSON.stringify(body), {
      status,
      headers: { "Content-Type": "application/json" },
    })
  )
  vi.stubGlobal("fetch", fetchMock)
  return fetchMock
}

const reminder = { id: 5, title: "우유 사기", subtasks: [] }

beforeEach(() => {
  vi.stubGlobal("navigator", {})
})

describe("getReminder", () => {
  it("requestsReminderById", async () => {
    const fetchMock = stubFetch(200, { resultCode: "SUCCESS", resultMsg: "성공", data: reminder })

    await expect(getReminder(5)).resolves.toEqual(reminder)

    expect(fetchMock).toHaveBeenCalledTimes(1)
    expect(fetchMock.mock.calls[0]).toEqual([
      `${BASE_URL}/api/reminders/5`,
      expect.objectContaining({ credentials: "include" }),
    ])
  })
})

describe("getReminderIfExists", () => {
  it("returnsReminderWhenFound", async () => {
    stubFetch(200, { resultCode: "SUCCESS", resultMsg: "성공", data: reminder })

    await expect(getReminderIfExists(5)).resolves.toEqual(reminder)
  })

  it("returnsUndefinedWhenNotFound", async () => {
    stubFetch(404, { resultCode: "NOT_FOUND", resultMsg: "Reminder not found: 5", data: null })

    await expect(getReminderIfExists(5)).resolves.toBeUndefined()
  })

  it("rethrowsOtherErrors", async () => {
    stubFetch(500, { resultCode: "INTERNAL_SERVER_ERROR", resultMsg: "서버 오류가 발생했습니다.", data: null })

    await expect(getReminderIfExists(5)).rejects.toMatchObject({ status: 500 })
  })
})

describe("getSmartReminders", () => {
  it("sendsBrowserZoneForTodayView", async () => {
    const fetchMock = stubFetch(200, { resultCode: "SUCCESS", resultMsg: "성공", data: [] })

    await getSmartReminders("today")

    expect(fetchMock.mock.calls[0][0]).toBe(`${BASE_URL}/api/reminders/smart/today?zone=Asia%2FSeoul`)
  })

  it("omitsZoneForOtherViews", async () => {
    const fetchMock = stubFetch(200, { resultCode: "SUCCESS", resultMsg: "성공", data: [] })

    await getSmartReminders("scheduled")

    expect(fetchMock.mock.calls[0][0]).toBe(`${BASE_URL}/api/reminders/smart/scheduled`)
  })
})

describe("getUpcomingReminders", () => {
  it("sendsRangeAsUtcInstants", async () => {
    const fetchMock = stubFetch(200, { resultCode: "SUCCESS", resultMsg: "성공", data: [] })

    await getUpcomingReminders(new Date(2026, 9, 5, 7, 0), new Date(2026, 9, 5, 7, 2))

    const url = new URL(String(fetchMock.mock.calls[0][0]))
    expect(url.pathname).toBe("/api/reminders/upcoming")
    expect(url.searchParams.get("from")).toBe("2026-10-04T22:00:00.000Z")
    expect(url.searchParams.get("to")).toBe("2026-10-04T22:02:00.000Z")
  })
})

describe("createReminder", () => {
  it("sendsDueAtWithTimeZone", async () => {
    const fetchMock = stubFetch(201, { resultCode: "SUCCESS", resultMsg: "성공", data: reminder })

    await createReminder({ title: "회의", dueAt: "2026-10-04T22:00:00.000Z", timeZone: "Asia/Seoul" })

    expect(JSON.parse(String(fetchMock.mock.calls[0][1]?.body))).toEqual({
      title: "회의",
      dueAt: "2026-10-04T22:00:00.000Z",
      timeZone: "Asia/Seoul",
    })
  })
})

describe("getCompletedReminders", () => {
  it("requestsFirstPageWithoutCursorAndParsesSlice", async () => {
    const page = { items: [reminder], nextCursor: "next-1" }
    const fetchMock = stubFetch(200, { resultCode: "SUCCESS", resultMsg: "성공", data: page })

    await expect(getCompletedReminders()).resolves.toEqual(page)

    expect(fetchMock.mock.calls[0][0]).toBe(`${BASE_URL}/api/reminders/completed`)
  })

  it("passesCursorForNextPage", async () => {
    const fetchMock = stubFetch(200, {
      resultCode: "SUCCESS",
      resultMsg: "성공",
      data: { items: [], nextCursor: null },
    })

    await expect(getCompletedReminders("MjAyNi0xMC0wNFQxMjowMDowMFosNQ")).resolves.toEqual({
      items: [],
      nextCursor: null,
    })

    expect(fetchMock.mock.calls[0][0]).toBe(
      `${BASE_URL}/api/reminders/completed?cursor=MjAyNi0xMC0wNFQxMjowMDowMFosNQ`
    )
  })
})

describe("searchReminders", () => {
  it("sendsQueryWithoutCursorOnFirstPage", async () => {
    const fetchMock = stubFetch(200, { resultCode: "SUCCESS", resultMsg: "성공", data: { items: [], nextCursor: null } })

    await searchReminders("우유 & 빵")

    const url = new URL(String(fetchMock.mock.calls[0][0]))
    expect(url.pathname).toBe("/api/reminders/search")
    expect([...url.searchParams]).toEqual([["q", "우유 & 빵"]])
  })

  it("passesCursorWithQuery", async () => {
    const page = { items: [reminder], nextCursor: "NTA" }
    const fetchMock = stubFetch(200, { resultCode: "SUCCESS", resultMsg: "성공", data: page })

    await expect(searchReminders("우유", "NTA")).resolves.toEqual(page)

    const url = new URL(String(fetchMock.mock.calls[0][0]))
    expect(url.searchParams.get("q")).toBe("우유")
    expect(url.searchParams.get("cursor")).toBe("NTA")
  })
})
