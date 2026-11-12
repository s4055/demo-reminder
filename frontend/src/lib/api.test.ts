import { beforeEach, describe, expect, it, vi } from "vitest"

type ApiModule = typeof import("@/lib/api")

const BASE_URL = "http://localhost:8080"

function jsonResponse(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  })
}

function success(data: unknown, status = 200): Response {
  return jsonResponse(status, { resultCode: "SUCCESS", resultMsg: "성공", data })
}

function failure(status: number, resultCode: string, resultMsg: string): Response {
  return jsonResponse(status, { resultCode, resultMsg, data: null })
}

type FetchCall = { path: string; authorization: string | null; init: RequestInit }

function stubFetch(handler: (call: FetchCall) => Response | Promise<Response>) {
  const calls: FetchCall[] = []
  const fetchMock = vi.fn(async (input: RequestInfo | URL, init: RequestInit = {}) => {
    const path = String(input).replace(BASE_URL, "")
    const call = {
      path,
      authorization: new Headers(init.headers).get("Authorization"),
      init,
    }
    calls.push(call)
    return handler(call)
  })
  vi.stubGlobal("fetch", fetchMock)
  return calls
}

function refreshCalls(calls: FetchCall[]) {
  return calls.filter((call) => call.path === "/api/auth/refresh")
}

let api: ApiModule

beforeEach(async () => {
  vi.resetModules()
  vi.stubGlobal("navigator", {})
  api = await import("@/lib/api")
})

describe("apiRequest", () => {
  it("returnsDataOfSuccessResponse", async () => {
    stubFetch(() => success({ id: 1 }))

    await expect(api.apiRequest("/api/lists")).resolves.toEqual({ id: 1 })
  })

  it("sendsAccessTokenAndCredentialsToBaseUrl", async () => {
    const calls = stubFetch(() => success(null))
    api.setAccessToken("token-1")

    await api.apiRequest("/api/lists", { method: "GET" })

    expect(calls).toHaveLength(1)
    expect(calls[0].path).toBe("/api/lists")
    expect(calls[0].authorization).toBe("Bearer token-1")
    expect(calls[0].init.credentials).toBe("include")
    expect(calls[0].init.method).toBe("GET")
  })

  it("omitsAuthorizationHeaderWithoutAccessToken", async () => {
    const calls = stubFetch(() => success(null))

    await api.apiRequest("/api/auth/login")

    expect(calls[0].authorization).toBeNull()
  })

  it("throwsApiErrorWithStatusAndResultCodeOnFailure", async () => {
    stubFetch(() => failure(409, "VERSION_CONFLICT", "다른 사용자가 먼저 수정했습니다."))

    const error = await api.apiRequest("/api/reminders/1").catch((e: unknown) => e)

    expect(error).toBeInstanceOf(api.ApiError)
    expect(error).toMatchObject({
      status: 409,
      resultCode: "VERSION_CONFLICT",
      message: "요청에 실패했습니다. (다른 사용자가 먼저 수정했습니다.)",
    })
    expect(api.isVersionConflict(error)).toBe(true)
  })

  it("throwsApiErrorWhenSuccessStatusHasFailureResultCode", async () => {
    stubFetch(() => failure(200, "BAD_REQUEST", "잘못된 요청입니다."))

    await expect(api.apiRequest("/api/lists")).rejects.toMatchObject({
      status: 200,
      resultCode: "BAD_REQUEST",
    })
  })

  it("usesStatusInMessageWhenBodyIsNotJson", async () => {
    stubFetch(() => new Response("Bad Gateway", { status: 502 }))

    await expect(api.apiRequest("/api/lists")).rejects.toMatchObject({
      status: 502,
      resultCode: undefined,
      message: "요청에 실패했습니다. (status: 502)",
    })
  })
})

describe("apiRequest token renewal", () => {
  it("retriesWithRenewedTokenAfterUnauthorized", async () => {
    const calls = stubFetch(({ path, authorization }) => {
      if (path === "/api/auth/refresh") return success({ accessToken: "new" })
      return authorization === "Bearer new" ? success("ok") : failure(401, "UNAUTHORIZED", "만료")
    })
    api.setAccessToken("old")

    await expect(api.apiRequest("/api/lists")).resolves.toBe("ok")

    expect(calls.map((call) => call.path)).toEqual([
      "/api/lists",
      "/api/auth/refresh",
      "/api/lists",
    ])
    expect(calls[2].authorization).toBe("Bearer new")
  })

  it("callsUnauthorizedHandlerWhenRenewalFails", async () => {
    stubFetch(() => failure(401, "UNAUTHORIZED", "로그인이 필요합니다."))
    const onUnauthorized = vi.fn()
    api.setUnauthorizedHandler(onUnauthorized)
    api.setAccessToken("old")

    await expect(api.apiRequest("/api/lists")).rejects.toMatchObject({ status: 401 })

    expect(onUnauthorized).toHaveBeenCalledTimes(1)
    expect(api.hasAccessToken()).toBe(false)
  })

  it("callsUnauthorizedHandlerWhenRetryIsStillUnauthorized", async () => {
    const calls = stubFetch(({ path }) =>
      path === "/api/auth/refresh"
        ? success({ accessToken: "new" })
        : failure(401, "UNAUTHORIZED", "로그인이 필요합니다.")
    )
    const onUnauthorized = vi.fn()
    api.setUnauthorizedHandler(onUnauthorized)

    await expect(api.apiRequest("/api/lists")).rejects.toMatchObject({ status: 401 })

    expect(refreshCalls(calls)).toHaveLength(1)
    expect(onUnauthorized).toHaveBeenCalledTimes(1)
    expect(api.hasAccessToken()).toBe(false)
  })

  it("sharesSingleRefreshAcrossConcurrentUnauthorizedRequests", async () => {
    const calls = stubFetch(({ path, authorization }) => {
      if (path === "/api/auth/refresh") return success({ accessToken: "new" })
      return authorization === "Bearer new" ? success(path) : failure(401, "UNAUTHORIZED", "만료")
    })
    api.setAccessToken("old")

    const results = await Promise.all([
      api.apiRequest("/api/lists"),
      api.apiRequest("/api/tags"),
      api.apiRequest("/api/list-groups"),
    ])

    expect(results).toEqual(["/api/lists", "/api/tags", "/api/list-groups"])
    expect(refreshCalls(calls)).toHaveLength(1)
  })

  it("skipsRefreshWhenAnotherRequestAlreadyRenewedToken", async () => {
    let releaseFirst: () => void = () => {}
    const firstResponse = new Promise<Response>((resolve) => {
      releaseFirst = () => resolve(failure(401, "UNAUTHORIZED", "만료"))
    })
    const calls = stubFetch(({ path, authorization }) => {
      if (path === "/api/auth/refresh") return success({ accessToken: "new" })
      if (path === "/api/slow" && authorization === "Bearer old") return firstResponse
      return authorization === "Bearer new" ? success(path) : failure(401, "UNAUTHORIZED", "만료")
    })
    api.setAccessToken("old")

    const slow = api.apiRequest("/api/slow")
    await api.apiRequest("/api/fast")
    releaseFirst()

    await expect(slow).resolves.toBe("/api/slow")
    expect(refreshCalls(calls)).toHaveLength(1)
  })

  it.each(["/api/auth/login", "/api/auth/signup", "/api/auth/refresh", "/api/auth/logout"])(
    "doesNotRetryAuthPath %s",
    async (path) => {
      const calls = stubFetch(() => failure(401, "UNAUTHORIZED", "로그인이 필요합니다."))
      const onUnauthorized = vi.fn()
      api.setUnauthorizedHandler(onUnauthorized)

      await expect(api.apiRequest(path)).rejects.toMatchObject({ status: 401 })

      expect(calls).toHaveLength(1)
      expect(onUnauthorized).not.toHaveBeenCalled()
    }
  )

  it("doesNotRetryNonUnauthorizedFailure", async () => {
    const calls = stubFetch(() => failure(403, "FORBIDDEN", "권한이 없습니다."))

    await expect(api.apiRequest("/api/lists/1")).rejects.toMatchObject({ status: 403 })

    expect(calls).toHaveLength(1)
  })
})

describe("refreshAccessToken", () => {
  it("storesRenewedAccessToken", async () => {
    stubFetch(() => success({ accessToken: "new" }))

    await expect(api.refreshAccessToken()).resolves.toEqual({ accessToken: "new" })

    expect(api.hasAccessToken()).toBe(true)
  })

  it("returnsNullAndClearsTokenWhenRefreshIsUnauthorized", async () => {
    stubFetch(() => failure(401, "UNAUTHORIZED", "로그인이 필요합니다."))
    api.setAccessToken("old")

    await expect(api.refreshAccessToken()).resolves.toBeNull()

    expect(api.hasAccessToken()).toBe(false)
  })

  it("propagatesNonUnauthorizedRefreshFailure", async () => {
    stubFetch(() => failure(500, "INTERNAL_SERVER_ERROR", "서버 오류가 발생했습니다."))

    await expect(api.refreshAccessToken()).rejects.toMatchObject({ status: 500 })
  })

  it("runsRefreshInsideWebLockWhenAvailable", async () => {
    const request = vi.fn((_name: string, task: () => Promise<unknown>) => task())
    vi.stubGlobal("navigator", { locks: { request } })
    stubFetch(() => success({ accessToken: "new" }))

    await api.refreshAccessToken()

    expect(request).toHaveBeenCalledTimes(1)
    expect(request.mock.calls[0][0]).toBe("demoreminder-token-refresh")
  })
})

describe("jsonBody", () => {
  it("buildsJsonRequestInit", () => {
    expect(api.jsonBody("PATCH", { ids: [2, 1] })).toEqual({
      method: "PATCH",
      headers: { "Content-Type": "application/json" },
      body: '{"ids":[2,1]}',
    })
  })
})

describe("isVersionConflict", () => {
  it("isFalseForOtherErrors", () => {
    expect(api.isVersionConflict(new api.ApiError("x", 409, "CONFLICT"))).toBe(false)
    expect(api.isVersionConflict(new Error("x"))).toBe(false)
  })
})

describe("ApiError retryAfterSeconds", () => {
  it("carriesRetryAfterSecondsOfTooManyRequests", async () => {
    stubFetch(() =>
      new Response(
        JSON.stringify({ resultCode: "TOO_MANY_LOGIN_ATTEMPTS", resultMsg: "로그인 시도가 너무 많습니다.", data: null }),
        { status: 429, headers: { "Content-Type": "application/json", "Retry-After": "900" } }
      )
    )

    await expect(api.apiRequest("/api/auth/login", api.jsonBody("POST", {}))).rejects.toMatchObject({
      status: 429,
      resultCode: "TOO_MANY_LOGIN_ATTEMPTS",
      retryAfterSeconds: 900,
    })
  })

  it("leavesRetryAfterUndefinedWhenHeaderMissingOrNotSeconds", async () => {
    stubFetch(() =>
      new Response(JSON.stringify({ resultCode: "TOO_MANY_LOGIN_ATTEMPTS", resultMsg: "x", data: null }), {
        status: 429,
        headers: { "Retry-After": "Wed, 21 Oct 2026 07:28:00 GMT" },
      })
    )

    const error = await api.apiRequest("/api/auth/login", api.jsonBody("POST", {})).catch((e) => e)

    expect(error).toBeInstanceOf(api.ApiError)
    expect((error as InstanceType<ApiModule["ApiError"]>).retryAfterSeconds).toBeUndefined()
  })
})
