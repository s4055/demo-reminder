import { apiRequest, jsonBody } from "@/lib/api"

export type ReminderList = {
  id: number
  name: string
  color: string | null
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

export function deleteList(id: number): Promise<void> {
  return apiRequest<void>(`/api/lists/${id}`, { method: "DELETE" })
}
