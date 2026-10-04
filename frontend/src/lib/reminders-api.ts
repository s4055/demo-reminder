import { apiRequest, jsonBody } from "@/lib/api"
import type { Priority } from "@/lib/priority"
import type { SmartView } from "@/lib/selection"

export type Reminder = {
  id: number
  title: string
  memo: string | null
  completed: boolean
  flagged: boolean
  priority: Priority
  dueAt: string | null
  completedAt: string | null
  listId: number | null
  sortOrder: number
  // 붙은 태그 이름 (이름순)
  tags: string[]
  // 부모 리마인더 id. 최상위 리마인더이면 null
  parentId: number | null
  // 하위 작업 (부모 안의 순서대로). 하위 작업 자신은 항상 빈 배열이다.
  subtasks: Reminder[]
  createdAt: string
  updatedAt: string
}

// parentId를 지정하면 하위 작업으로 만들어지고, 리스트는 listId와 관계없이 부모를 따른다.
export type CreateReminderInput = {
  title: string
  memo?: string | null
  listId?: number | null
  dueAt?: string | null
  priority?: Priority
  tagNames?: string[]
  parentId?: number | null
}

// 전체 교체 방식이라 tagNames를 빈 배열로 보내면 태그가 모두 떨어진다.
export type UpdateReminderInput = {
  title: string
  memo: string | null
  dueAt: string | null
  flagged: boolean
  priority: Priority
  tagNames: string[]
}

// listId를 지정하면 최상위 리마인더만 오고, 하위 작업은 각 항목의 subtasks에 담긴다.
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

export function createReminder(input: CreateReminderInput): Promise<Reminder> {
  return apiRequest<Reminder>("/api/reminders", jsonBody("POST", input))
}

export function updateReminder(
  id: number,
  input: UpdateReminderInput
): Promise<Reminder> {
  return apiRequest<Reminder>(`/api/reminders/${id}`, jsonBody("PUT", input))
}

// ids 순서가 곧 새 순서다. 리스트의 최상위 미완료 리마인더 id를 모두 담아야 한다.
export function reorderReminders(listId: number, ids: number[]): Promise<void> {
  return apiRequest<void>(
    "/api/reminders/order",
    jsonBody("PATCH", { listId, ids })
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
