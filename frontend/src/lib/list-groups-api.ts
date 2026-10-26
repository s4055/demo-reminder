import { apiRequest, jsonBody } from "@/lib/api"

export type ListGroup = {
  id: number
  name: string
  sortOrder: number
  createdAt: string
  updatedAt: string
}

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

export function reorderListGroups(ids: number[]): Promise<void> {
  return apiRequest<void>("/api/list-groups/order", jsonBody("PATCH", { ids }))
}

export function deleteListGroup(id: number): Promise<void> {
  return apiRequest<void>(`/api/list-groups/${id}`, { method: "DELETE" })
}
