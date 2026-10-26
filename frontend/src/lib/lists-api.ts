import { apiRequest, jsonBody } from "@/lib/api"

export type ListRole = "OWNER" | "EDITOR"

export type ReminderList = {
  id: number
  name: string
  color: string | null
  icon: string | null
  groupId: number | null
  sortOrder: number
  reminderCount: number
  role: ListRole
  memberCount: number
  version: number
  createdAt: string
  updatedAt: string
}

export type ListMember = {
  userId: number
  email: string
  name: string
  role: ListRole
  joinedAt: string
}

export function isOwner(list: ReminderList): boolean {
  return list.role === "OWNER"
}

export function isShared(list: ReminderList): boolean {
  return list.memberCount > 1
}

export type ReminderListInput = {
  name: string
  color?: string | null
  icon?: string | null
}

export type UpdateReminderListInput = ReminderListInput & { version: number }

export function getLists(): Promise<ReminderList[]> {
  return apiRequest<ReminderList[]>("/api/lists")
}

export function createList(input: ReminderListInput): Promise<ReminderList> {
  return apiRequest<ReminderList>("/api/lists", jsonBody("POST", input))
}

export function updateList(
  id: number,
  input: UpdateReminderListInput
): Promise<ReminderList> {
  return apiRequest<ReminderList>(`/api/lists/${id}`, jsonBody("PUT", input))
}

export function reorderLists(ids: number[]): Promise<void> {
  return apiRequest<void>("/api/lists/order", jsonBody("PATCH", { ids }))
}

export function changeListGroup(
  id: number,
  groupId: number | null
): Promise<ReminderList> {
  return apiRequest<ReminderList>(
    `/api/lists/${id}/group`,
    jsonBody("PATCH", { groupId })
  )
}

export function deleteList(id: number): Promise<void> {
  return apiRequest<void>(`/api/lists/${id}`, { method: "DELETE" })
}

export function getListMembers(listId: number): Promise<ListMember[]> {
  return apiRequest<ListMember[]>(`/api/lists/${listId}/members`)
}

export function inviteListMember(
  listId: number,
  email: string
): Promise<ListMember> {
  return apiRequest<ListMember>(
    `/api/lists/${listId}/members`,
    jsonBody("POST", { email })
  )
}

export function removeListMember(listId: number, userId: number): Promise<void> {
  return apiRequest<void>(`/api/lists/${listId}/members/${userId}`, {
    method: "DELETE",
  })
}
