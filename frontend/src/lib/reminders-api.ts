import { apiRequest, jsonBody } from "@/lib/api"
import { toDueAtParam } from "@/lib/due-date"
import type { Priority } from "@/lib/priority"
import type { DayOfWeek, RepeatRule } from "@/lib/repeat"
import type { SmartView } from "@/lib/selection"

export type Reminder = {
  id: number
  title: string
  memo: string | null
  completed: boolean
  flagged: boolean
  priority: Priority
  dueAt: string | null
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

export type CreateReminderInput = {
  title: string
  memo?: string | null
  listId?: number | null
  dueAt?: string | null
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

export function getRemindersByTag(tag: string): Promise<Reminder[]> {
  return apiRequest<Reminder[]>(
    `/api/reminders?tag=${encodeURIComponent(tag)}`
  )
}

export function getSmartReminders(view: SmartView): Promise<Reminder[]> {
  return apiRequest<Reminder[]>(`/api/reminders/smart/${view}`)
}

export function searchReminders(query: string): Promise<Reminder[]> {
  return apiRequest<Reminder[]>(
    `/api/reminders/search?q=${encodeURIComponent(query)}`
  )
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
): Promise<Reminder> {
  return apiRequest<Reminder>(`/api/reminders/${id}`, jsonBody("PUT", input))
}

export function reorderReminders(listId: number, ids: number[]): Promise<void> {
  return apiRequest<void>(
    "/api/reminders/order",
    jsonBody("PATCH", { listId, ids })
  )
}

export function moveReminder(id: number, listId: number): Promise<Reminder> {
  return apiRequest<Reminder>(
    `/api/reminders/${id}/list`,
    jsonBody("PATCH", { listId })
  )
}

export function toggleReminderComplete(id: number): Promise<Reminder> {
  return apiRequest<Reminder>(`/api/reminders/${id}/complete`, {
    method: "PATCH",
  })
}

export function toggleReminderFlag(id: number): Promise<Reminder> {
  return apiRequest<Reminder>(`/api/reminders/${id}/flag`, {
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
