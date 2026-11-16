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

export type DayOfWeek =
  | "MONDAY"
  | "TUESDAY"
  | "WEDNESDAY"
  | "THURSDAY"
  | "FRIDAY"
  | "SATURDAY"
  | "SUNDAY"

export type RepeatRule = "NONE" | "DAILY" | "WEEKLY" | "MONTHLY" | "YEARLY"

export type Priority = "NONE" | "LOW" | "MEDIUM" | "HIGH"

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
  memo: string | null
  completed: boolean
  flagged: boolean
  priority: Priority
  dueAt: string | null
  timeZone: string | null
  repeatRule: RepeatRule
  repeatInterval: number
  repeatDaysOfWeek: DayOfWeek[]
  listId: number | null
  tags: string[]
  parentId: number | null
  version: number
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
  priority?: Priority
  tagNames?: string[]
  parentId?: number
  repeatRule?: RepeatRule
  repeatInterval?: number
  repeatDaysOfWeek?: DayOfWeek[]
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

  async createCompletedReminder(input: ReminderInput): Promise<CreatedReminder> {
    const reminder = await this.createReminder(input)
    await this.toggleComplete(reminder.id)
    return reminder
  }

  getReminder(id: number): Promise<CreatedReminder> {
    return this.send("GET", `/api/reminders/${id}`)
  }

  toggleComplete(id: number): Promise<void> {
    return this.send("PATCH", `/api/reminders/${id}/complete`)
  }

  toggleFlag(id: number): Promise<void> {
    return this.send("PATCH", `/api/reminders/${id}/flag`)
  }

  async rescheduleReminder(id: number, dueAt: string): Promise<CreatedReminder> {
    const current = await this.getReminder(id)
    return this.send("PUT", `/api/reminders/${id}`, {
      title: current.title,
      memo: current.memo,
      dueAt,
      timeZone: current.timeZone ?? "Asia/Seoul",
      flagged: current.flagged,
      priority: current.priority,
      tagNames: current.tags,
      repeatRule: current.repeatRule,
      repeatInterval: current.repeatInterval,
      repeatDaysOfWeek: current.repeatDaysOfWeek,
      version: current.version,
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
