import { expect, request, type APIRequestContext } from "@playwright/test"
import { BACKEND_URL, newTestUser, type TestUser } from "./users"

type ApiResponse<T> = {
  resultCode: string
  resultMsg: string
  data: T
}

type AuthData = {
  accessToken: string
}

export type CreatedList = {
  id: number
  name: string
  color: string | null
  icon: string | null
}

export type CreatedGroup = {
  id: number
  name: string
}

export type CreatedReminder = {
  id: number
  title: string
  listId: number | null
}

export type ListInput = {
  name: string
  color?: string
  icon?: string
}

export type ReminderInput = {
  title: string
  listId?: number
  memo?: string
  dueAt?: string
  timeZone?: string
  priority?: "NONE" | "LOW" | "MEDIUM" | "HIGH"
  tagNames?: string[]
  parentId?: number
}

export async function signupViaApi(
  context: APIRequestContext,
  user: TestUser
): Promise<string> {
  return authenticate(context, "/api/auth/signup", user)
}

export async function loginViaApi(
  context: APIRequestContext,
  user: Pick<TestUser, "email" | "password">
): Promise<string> {
  return authenticate(context, "/api/auth/login", user)
}

async function authenticate(
  context: APIRequestContext,
  path: string,
  body: object
): Promise<string> {
  const response = await context.post(`${BACKEND_URL}${path}`, { data: body })
  expect(response.ok(), `${path} 응답 ${response.status()}`).toBe(true)
  const json = (await response.json()) as ApiResponse<AuthData>
  return json.data.accessToken
}

export class ApiClient {
  private constructor(
    private readonly context: APIRequestContext,
    readonly user: TestUser,
    private accessToken: string
  ) {}

  static async signup(user: TestUser = newTestUser()): Promise<ApiClient> {
    const context = await request.newContext()
    return new ApiClient(context, user, await signupViaApi(context, user))
  }

  static async login(user: TestUser): Promise<ApiClient> {
    const context = await request.newContext()
    return new ApiClient(context, user, await loginViaApi(context, user))
  }

  createList(input: ListInput): Promise<CreatedList> {
    return this.send("POST", "/api/lists", input)
  }

  createGroup(name: string): Promise<CreatedGroup> {
    return this.send("POST", "/api/list-groups", { name })
  }

  assignListToGroup(listId: number, groupId: number | null): Promise<void> {
    return this.send("PATCH", `/api/lists/${listId}/group`, { groupId })
  }

  createReminder(input: ReminderInput): Promise<CreatedReminder> {
    return this.send("POST", "/api/reminders", {
      timeZone: "Asia/Seoul",
      ...input,
    })
  }

  async dispose(): Promise<void> {
    await this.context.dispose()
  }

  private async send<T>(method: string, path: string, data?: unknown): Promise<T> {
    let response = await this.fetch(method, path, data)
    if (response.status() === 401) {
      this.accessToken = await loginViaApi(this.context, this.user)
      response = await this.fetch(method, path, data)
    }
    expect(response.ok(), `${method} ${path} 응답 ${response.status()}`).toBe(true)
    const json = (await response.json()) as ApiResponse<T>
    return json.data
  }

  private fetch(method: string, path: string, data?: unknown) {
    return this.context.fetch(`${BACKEND_URL}${path}`, {
      method,
      data,
      headers: { Authorization: `Bearer ${this.accessToken}` },
    })
  }
}
