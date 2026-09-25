const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080"

// 모든 API 응답의 공통 형태. 성공 시 data에 결과가, 실패 시 data는 null이다.
type ApiResponse<T> = {
  resultCode: string
  resultMsg: string
  data: T
}

const SUCCESS = "SUCCESS"

export async function apiRequest<T>(
  path: string,
  init?: RequestInit
): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, init)
  const body = (await response.json().catch(() => null)) as ApiResponse<T> | null
  if (!response.ok || body?.resultCode !== SUCCESS) {
    const message = body?.resultMsg ?? `status: ${response.status}`
    throw new Error(`요청에 실패했습니다. (${message})`)
  }
  return body.data
}

// POST, PUT 요청 시 Request Boby 생성
export function jsonBody(method: "POST" | "PUT", body: unknown): RequestInit {
  return {
    method,
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  }
}
