import { apiRequest, jsonBody } from "@/lib/api"

// OWNER는 리스트 수정/삭제와 멤버 관리까지, EDITOR는 리마인더만 다룰 수 있다.
export type ListRole = "OWNER" | "EDITOR"

export type ReminderList = {
  id: number
  name: string
  color: string | null
  sortOrder: number
  reminderCount: number
  // 로그인한 사용자의 역할
  role: ListRole
  // 소유자를 포함한 멤버 수. 2 이상이면 공유된 리스트다.
  memberCount: number
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

// ids 순서가 곧 새 사이드바 순서다. 소유한 모든 리스트 id를 담아야 한다 (공유받은 리스트는 제외).
export function reorderLists(ids: number[]): Promise<void> {
  return apiRequest<void>("/api/lists/order", jsonBody("PATCH", { ids }))
}

export function deleteList(id: number): Promise<void> {
  return apiRequest<void>(`/api/lists/${id}`, { method: "DELETE" })
}

// 소유자가 먼저, 나머지는 초대한 순서다.
export function getListMembers(listId: number): Promise<ListMember[]> {
  return apiRequest<ListMember[]>(`/api/lists/${listId}/members`)
}

// 가입한 사용자만 초대할 수 있다. 없는 사용자는 404, 이미 멤버면 400(ApiError)으로 실패한다.
export function inviteListMember(
  listId: number,
  email: string
): Promise<ListMember> {
  return apiRequest<ListMember>(
    `/api/lists/${listId}/members`,
    jsonBody("POST", { email })
  )
}

// userId가 본인이면 리스트에서 나간다.
export function removeListMember(listId: number, userId: number): Promise<void> {
  return apiRequest<void>(`/api/lists/${listId}/members/${userId}`, {
    method: "DELETE",
  })
}
