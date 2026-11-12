import { ApiError, apiRequest, jsonBody } from "@/lib/api"
import { toDueAtParam } from "@/lib/due-date"
import type { Priority } from "@/lib/priority"
import type { DayOfWeek, RepeatRule } from "@/lib/repeat"
import type { SmartView } from "@/lib/selection"
import { browserTimeZone } from "@/lib/timezone"

export type Reminder = {
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
  completedAt: string | null
  listId: number | null
  sortOrder: number
  tags: string[]
  parentId: number | null
  subtasks: Reminder[]
  version: number
  createdAt: string
  updatedAt: string
}

export type Slice<T> = {
  items: T[]
  nextCursor: string | null
}

export type ListedSmartView = Exclude<SmartView, "completed">

export type CreateReminderInput = {
  title: string
  memo?: string | null
  listId?: number | null
  dueAt?: string | null
  timeZone?: string
  priority?: Priority
  tagNames?: string[]
  parentId?: number | null
  repeatRule?: RepeatRule
  repeatInterval?: number
  repeatDaysOfWeek?: DayOfWeek[]
}

export type UpdateReminderInput = {
  title: string
  memo: string | null
  dueAt: string | null
  timeZone: string
  flagged: boolean
  priority: Priority
  tagNames: string[]
  repeatRule: RepeatRule
  repeatInterval: number
  repeatDaysOfWeek: DayOfWeek[]
  version: number
}

export function getReminders(listId?: number): Promise<Reminder[]> {
  const query = listId === undefined ? "" : `?listId=${listId}`
  return apiRequest<Reminder[]>(`/api/reminders${query}`)
}

export function getReminder(id: number): Promise<Reminder> {
  return apiRequest<Reminder>(`/api/reminders/${id}`)
}

export async function getReminderIfExists(
  id: number
): Promise<Reminder | undefined> {
  try {
    return await getReminder(id)
  } catch (error) {
    if (error instanceof ApiError && error.status === 404) return undefined
    throw error
  }
}

export function getRemindersByTag(tag: string): Promise<Reminder[]> {
  return apiRequest<Reminder[]>(
    `/api/reminders?tag=${encodeURIComponent(tag)}`
  )
}

export function getSmartReminders(view: ListedSmartView): Promise<Reminder[]> {
  const query =
    view === "today"
      ? `?${new URLSearchParams({ zone: browserTimeZone() })}`
      : ""
  return apiRequest<Reminder[]>(`/api/reminders/smart/${view}${query}`)
}

export function getCompletedReminders(
  cursor: string | null = null
): Promise<Slice<Reminder>> {
  return apiRequest<Slice<Reminder>>(
    `/api/reminders/completed${pageQuery({}, cursor)}`
  )
}

export function searchReminders(
  query: string,
  cursor: string | null = null
): Promise<Slice<Reminder>> {
  return apiRequest<Slice<Reminder>>(
    `/api/reminders/search${pageQuery({ q: query }, cursor)}`
  )
}

function pageQuery(params: Record<string, string>, cursor: string | null) {
  const query = new URLSearchParams(params)
  if (cursor !== null) query.set("cursor", cursor)
  return query.size > 0 ? `?${query}` : ""
}

export function getUpcomingReminders(from: Date, to: Date): Promise<Reminder[]> {
  const query = new URLSearchParams({
    from: toDueAtParam(from),
    to: toDueAtParam(to),
  })
  return apiRequest<Reminder[]>(`/api/reminders/upcoming?${query}`)
}

export function createReminder(input: CreateReminderInput): Promise<Reminder> {
  return apiRequest<Reminder>("/api/reminders", jsonBody("POST", input))
}

export function updateReminder(
  id: number,
  input: UpdateReminderInput
): Promise<void> {
  return apiRequest<void>(`/api/reminders/${id}`, jsonBody("PUT", input))
}

export function reorderReminders(listId: number, ids: number[]): Promise<void> {
  return apiRequest<void>(
    "/api/reminders/order",
    jsonBody("PATCH", { listId, ids })
  )
}

export function moveReminder(id: number, listId: number): Promise<void> {
  return apiRequest<void>(
    `/api/reminders/${id}/list`,
    jsonBody("PATCH", { listId })
  )
}

export function toggleReminderComplete(id: number): Promise<void> {
  return apiRequest<void>(`/api/reminders/${id}/complete`, {
    method: "PATCH",
  })
}

export function toggleReminderFlag(id: number): Promise<void> {
  return apiRequest<void>(`/api/reminders/${id}/flag`, {
    method: "PATCH",
  })
}

export function deleteReminder(id: number): Promise<void> {
  return apiRequest<void>(`/api/reminders/${id}`, { method: "DELETE" })
}

export function deleteCompletedReminders(
  listId: number
): Promise<{ deletedCount: number }> {
  return apiRequest<{ deletedCount: number }>(
    `/api/reminders/completed?listId=${listId}`,
    { method: "DELETE" }
  )
}
