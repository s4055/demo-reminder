import {
  ApiError,
  apiRequest,
  hasAccessToken,
  jsonBody,
  refreshAccessToken,
  setAccessToken,
} from "@/lib/api"

export type User = {
  id: number
  email: string
  name: string
  createdAt: string
}

export type SignupInput = {
  email: string
  password: string
  name: string
}

export type LoginInput = {
  email: string
  password: string
}

type AuthResponse = {
  accessToken: string
  user: User
}

function startSession(response: AuthResponse): User {
  setAccessToken(response.accessToken)
  return response.user
}

export async function signup(input: SignupInput): Promise<User> {
  return startSession(
    await apiRequest<AuthResponse>("/api/auth/signup", jsonBody("POST", input))
  )
}

export async function login(input: LoginInput): Promise<User> {
  return startSession(
    await apiRequest<AuthResponse>("/api/auth/login", jsonBody("POST", input))
  )
}

export async function logout(): Promise<void> {
  await apiRequest<void>("/api/auth/logout", { method: "POST" })
  setAccessToken(null)
}

export async function getMe(): Promise<User> {
  if (hasAccessToken()) return apiRequest<User>("/api/auth/me")
  const session = (await refreshAccessToken()) as AuthResponse | null
  if (session === null) {
    throw new ApiError("요청에 실패했습니다. (로그인이 필요합니다.)", 401, "UNAUTHORIZED")
  }
  return session.user
}
