const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080"

type ApiResponse<T> = {
  resultCode: string
  resultMsg: string
  data: T
}

const SUCCESS = "SUCCESS"

export class ApiError extends Error {
  constructor(
    message: string,
    readonly status: number,
    readonly resultCode: string | undefined
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

export async function apiRequest<T>(
  path: string,
  init?: RequestInit
): Promise<T> {
  // 세션 쿠키(JSESSIONID)는 다른 포트(백엔드)로 보내야 하므로 credentials를 include로 둔다.
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...init,
    credentials: "include",
  })
  const body = (await response.json().catch(() => null)) as ApiResponse<T> | null
  if (!response.ok || body?.resultCode !== SUCCESS) {
    if (response.status === 401 && !path.startsWith("/api/auth/")) {
      unauthorizedHandler?.()
    }
    const message = body?.resultMsg ?? `status: ${response.status}`
    throw new ApiError(
      `요청에 실패했습니다. (${message})`,
      response.status,
      body?.resultCode
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
