import { apiRequest, jsonBody } from "@/lib/api"

export type ReminderList = {
  id: number
  name: string
  color: string | null
  sortOrder: number
  reminderCount: number
  createdAt: string
  updatedAt: string
}

export type ReminderListInput = {
  name: string
  color?: string | null
}

export function getLists(): Promise<ReminderList[]> {
  return apiRequest<ReminderList[]>("/api/lists")
}

export function createList(input: ReminderListInput): Promise<ReminderList> {
  return apiRequest<ReminderList>("/api/lists", jsonBody("POST", input))
}

export function updateList(
  id: number,
  input: ReminderListInput
): Promise<ReminderList> {
  return apiRequest<ReminderList>(`/api/lists/${id}`, jsonBody("PUT", input))
}

// ids 순서가 곧 새 사이드바 순서다. 모든 리스트 id를 담아야 한다.
export function reorderLists(ids: number[]): Promise<void> {
  return apiRequest<void>("/api/lists/order", jsonBody("PATCH", { ids }))
}

export function deleteList(id: number): Promise<void> {
  return apiRequest<void>(`/api/lists/${id}`, { method: "DELETE" })
}
