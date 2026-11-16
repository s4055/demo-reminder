import { randomUUID } from "node:crypto"

export const BACKEND_URL = "http://localhost:8080"

export const DEFAULT_PASSWORD = "password1234"

export type TestUser = {
  email: string
  name: string
  password: string
}

export function uniqueEmail(prefix = "e2e"): string {
  return `${prefix}-${randomUUID()}@example.com`
}

export function newTestUser(overrides: Partial<TestUser> = {}): TestUser {
  return {
    email: uniqueEmail(),
    name: "테스트 사용자",
    password: DEFAULT_PASSWORD,
    ...overrides,
  }
}
