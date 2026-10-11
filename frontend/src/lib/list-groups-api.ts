import { apiRequest, jsonBody } from "@/lib/api"

// 사이드바에서 소유한 리스트를 묶는 그룹(폴더). 사용자마다 따로 가진다.
export type ListGroup = {
  id: number
  name: string
  sortOrder: number
  createdAt: string
  updatedAt: string
}

// 표시 순서대로 온다.
export function getListGroups(): Promise<ListGroup[]> {
  return apiRequest<ListGroup[]>("/api/list-groups")
}

export function createListGroup(name: string): Promise<ListGroup> {
  return apiRequest<ListGroup>("/api/list-groups", jsonBody("POST", { name }))
}

export function renameListGroup(id: number, name: string): Promise<ListGroup> {
  return apiRequest<ListGroup>(
    `/api/list-groups/${id}`,
    jsonBody("PUT", { name })
  )
}

// ids 순서가 곧 새 순서다. 모든 그룹 id를 빠짐없이 담아야 한다.
export function reorderListGroups(ids: number[]): Promise<void> {
  return apiRequest<void>("/api/list-groups/order", jsonBody("PATCH", { ids }))
}

// 소속 리스트는 그룹에서 빠질 뿐 삭제되지 않는다.
export function deleteListGroup(id: number): Promise<void> {
  return apiRequest<void>(`/api/list-groups/${id}`, { method: "DELETE" })
}
