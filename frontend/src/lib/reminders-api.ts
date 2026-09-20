import { apiRequest, jsonBody } from "@/lib/api"

export type Reminder = {
  id: number
  title: string
  memo: string | null
  completed: boolean
  listId: number | null
  createdAt: string
  updatedAt: string
}

export type CreateReminderInput = {
  title: string
  memo?: string | null
  listId?: number | null
}

export function getReminders(listId?: number): Promise<Reminder[]> {
  const query = listId === undefined ? "" : `?listId=${listId}`
  return apiRequest<Reminder[]>(`/api/reminders${query}`)
}

export function createReminder(input: CreateReminderInput): Promise<Reminder> {
  return apiRequest<Reminder>("/api/reminders", jsonBody("POST", input))
}

export function toggleReminderComplete(id: number): Promise<Reminder> {
  return apiRequest<Reminder>(`/api/reminders/${id}/complete`, {
    method: "PATCH",
  })
}

export function deleteReminder(id: number): Promise<void> {
  return apiRequest<void>(`/api/reminders/${id}`, { method: "DELETE" })
}
