const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080"

export type Reminder = {
  id: number
  title: string
  memo: string | null
  completed: boolean
  createdAt: string
  updatedAt: string
}

export type CreateReminderInput = {
  title: string
  memo?: string | null
}

async function handleResponse<T>(response: Response): Promise<T> {
  if (!response.ok) {
    throw new Error(`리마인더 요청에 실패했습니다. (status: ${response.status})`)
  }
  if (response.status === 204) {
    return undefined as T
  }
  return response.json() as Promise<T>
}

export function getReminders(): Promise<Reminder[]> {
  return fetch(`${API_BASE_URL}/api/reminders`).then((res) =>
    handleResponse<Reminder[]>(res)
  )
}

export function createReminder(input: CreateReminderInput): Promise<Reminder> {
  return fetch(`${API_BASE_URL}/api/reminders`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(input),
  }).then((res) => handleResponse<Reminder>(res))
}

export function toggleReminderComplete(id: number): Promise<Reminder> {
  return fetch(`${API_BASE_URL}/api/reminders/${id}/complete`, {
    method: "PATCH",
  }).then((res) => handleResponse<Reminder>(res))
}

export function deleteReminder(id: number): Promise<void> {
  return fetch(`${API_BASE_URL}/api/reminders/${id}`, {
    method: "DELETE",
  }).then((res) => handleResponse<void>(res))
}
