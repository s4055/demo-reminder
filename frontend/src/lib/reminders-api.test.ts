import { beforeEach, describe, expect, it, vi } from "vitest"
import { getReminder, getReminderIfExists } from "@/lib/reminders-api"

const BASE_URL = "http://localhost:8080"

function stubFetch(status: number, body: unknown) {
  const fetchMock = vi.fn(async () =>
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
