const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080"

// 모든 API 응답의 공통 형태. 성공 시 data에 결과가, 실패 시 data는 null이다.
type ApiResponse<T> = {
  resultCode: string
  resultMsg: string
  data: T
}

const SUCCESS = "SUCCESS"

// 실패한 요청의 HTTP 상태와 resultCode를 담는다. 화면에서 401/409 등을 구분해 안내할 때 쓴다.
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

// 로그인이 풀린 상태(401)로 일반 API를 호출했을 때 실행할 동작. Providers에서 로그인 페이지 이동을 등록한다.
// /api/auth/** 의 401(로그인 실패, 비로그인 상태의 me 조회)은 각 화면이 직접 처리하므로 제외한다.
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
