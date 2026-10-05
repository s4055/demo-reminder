import { apiRequest, jsonBody } from "@/lib/api"

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

// 가입하면 바로 로그인된 세션이 생긴다.
export function signup(input: SignupInput): Promise<User> {
  return apiRequest<User>("/api/auth/signup", jsonBody("POST", input))
}

export function login(input: LoginInput): Promise<User> {
  return apiRequest<User>("/api/auth/login", jsonBody("POST", input))
}

export function logout(): Promise<void> {
  return apiRequest<void>("/api/auth/logout", { method: "POST" })
}

// 로그인하지 않았으면 401(ApiError)로 실패한다.
export function getMe(): Promise<User> {
  return apiRequest<User>("/api/auth/me")
}
