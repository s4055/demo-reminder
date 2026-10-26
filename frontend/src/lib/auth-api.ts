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

export function signup(input: SignupInput): Promise<User> {
  return apiRequest<User>("/api/auth/signup", jsonBody("POST", input))
}

export function login(input: LoginInput): Promise<User> {
  return apiRequest<User>("/api/auth/login", jsonBody("POST", input))
}

export function logout(): Promise<void> {
  return apiRequest<void>("/api/auth/logout", { method: "POST" })
}

export function getMe(): Promise<User> {
  return apiRequest<User>("/api/auth/me")
}
