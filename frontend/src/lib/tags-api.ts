import { apiRequest } from "@/lib/api"

export type Tag = {
  id: number
  name: string
  reminderCount: number
  createdAt: string
}

export function getTags(): Promise<Tag[]> {
  return apiRequest<Tag[]>("/api/tags")
}

export function deleteTag(id: number): Promise<void> {
  return apiRequest<void>(`/api/tags/${id}`, { method: "DELETE" })
}

// 사용자가 "#집"처럼 입력해도 태그 이름은 "집"으로 저장한다.
export function normalizeTagName(input: string): string {
  return input.trim().replace(/^#+/, "").trim()
}
