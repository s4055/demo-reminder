const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080"

type ApiResponse<T> = {
  resultCode: string
  resultMsg: string
  data: T
}

export type TokenResponse = {
  accessToken: string
}

const SUCCESS = "SUCCESS"
const REFRESH_PATH = "/api/auth/refresh"
const REFRESH_LOCK = "demoreminder-token-refresh"
const NO_REFRESH_RETRY_PATHS = new Set([
  "/api/auth/login",
  "/api/auth/signup",
  REFRESH_PATH,
  "/api/auth/logout",
])

export class ApiError extends Error {
  constructor(
    message: string,
    readonly status: number,
    readonly resultCode: string | undefined,
    readonly retryAfterSeconds?: number
  ) {
    super(message)
    this.name = "ApiError"
  }
}

export function isVersionConflict(error: unknown): boolean {
  return error instanceof ApiError && error.resultCode === "VERSION_CONFLICT"
}

export const VERSION_CONFLICT_MESSAGE =
  "다른 사용자가 먼저 수정했습니다. 최신 내용을 불러왔습니다."

let unauthorizedHandler: (() => void) | undefined

export function setUnauthorizedHandler(handler: (() => void) | undefined) {
  unauthorizedHandler = handler
}

let accessToken: string | null = null
let refreshInFlight: Promise<TokenResponse | null> | null = null

export function setAccessToken(token: string | null) {
  accessToken = token
}

export function hasAccessToken(): boolean {
  return accessToken !== null
}

export function refreshAccessToken(): Promise<TokenResponse | null> {
  refreshInFlight ??= withRefreshLock(requestRefresh).finally(() => {
    refreshInFlight = null
  })
  return refreshInFlight
}

function withRefreshLock<T>(task: () => Promise<T>): Promise<T> {
  if (typeof navigator === "undefined" || !navigator.locks) return task()
  return navigator.locks.request(REFRESH_LOCK, task) as Promise<T>
}

async function requestRefresh(): Promise<TokenResponse | null> {
  try {
    const tokens = await readData<TokenResponse>(
      await send(REFRESH_PATH, { method: "POST" })
    )
    accessToken = tokens.accessToken
    return tokens
  } catch (error) {
    if (error instanceof ApiError && error.status === 401) {
      accessToken = null
      return null
    }
    throw error
  }
}

async function renewAccessToken(sentToken: string | null): Promise<boolean> {
  if (accessToken !== null && accessToken !== sentToken) return true
  return (await refreshAccessToken()) !== null
}

export async function apiRequest<T>(
  path: string,
  init?: RequestInit
): Promise<T> {
  const sentToken = accessToken
  let response = await send(path, init)
  if (response.status === 401 && !NO_REFRESH_RETRY_PATHS.has(path)) {
    if (await renewAccessToken(sentToken)) {
      response = await send(path, init)
    }
    if (response.status === 401) {
      accessToken = null
      unauthorizedHandler?.()
    }
  }
  return readData<T>(response)
}

function send(path: string, init?: RequestInit): Promise<Response> {
  const headers = new Headers(init?.headers)
  if (accessToken) headers.set("Authorization", `Bearer ${accessToken}`)
  // 리프레시 토큰 쿠키(refresh_token)는 다른 포트(백엔드)로 보내야 하므로 credentials를 include로 둔다.
  return fetch(`${API_BASE_URL}${path}`, {
    ...init,
    headers,
    credentials: "include",
  })
}

function parseRetryAfter(value: string | null): number | undefined {
  const seconds = value?.trim()
  if (!seconds || !/^\d+$/.test(seconds)) return undefined
  return Number(seconds)
}

async function readData<T>(response: Response): Promise<T> {
  const body = (await response.json().catch(() => null)) as ApiResponse<T> | null
  if (!response.ok || body?.resultCode !== SUCCESS) {
    const message = body?.resultMsg ?? `status: ${response.status}`
    throw new ApiError(
      `요청에 실패했습니다. (${message})`,
      response.status,
      body?.resultCode,
      parseRetryAfter(response.headers.get("Retry-After"))
    )
  }
  return body.data
}

// POST, PUT, PATCH 요청 시 Request Body 생성
export function jsonBody(
  method: "POST" | "PUT" | "PATCH",
  body: unknown
): RequestInit {
  return {
    method,
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  }
}
