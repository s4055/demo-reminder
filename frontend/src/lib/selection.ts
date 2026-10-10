export type SmartView = "today" | "scheduled" | "all" | "flagged" | "completed"

export type Selection =
  | { type: "smart"; view: SmartView }
  | { type: "list"; listId: number }
  | { type: "tag"; name: string }
  // 사이드바 검색창의 검색 결과. query는 앞뒤 공백을 뺀 검색어다.
  | { type: "search"; query: string }

export const SMART_VIEWS: { view: SmartView; label: string }[] = [
  { view: "today", label: "오늘" },
  { view: "scheduled", label: "예정됨" },
  { view: "all", label: "전체" },
  { view: "flagged", label: "플래그 지정됨" },
  { view: "completed", label: "완료됨" },
]

export const DEFAULT_SELECTION: Selection = { type: "smart", view: "all" }

export function smartViewLabel(view: SmartView): string {
  return SMART_VIEWS.find((item) => item.view === view)?.label ?? view
}

export function emptyMessage(selection: Selection): string {
  if (selection.type === "list") return "이 리스트에 리마인더가 없습니다."
  if (selection.type === "tag") {
    return `#${selection.name} 태그가 붙은 리마인더가 없습니다.`
  }
  if (selection.type === "search") {
    return `“${selection.query}”와(과) 일치하는 리마인더가 없습니다.`
  }
  switch (selection.view) {
    case "today":
      return "오늘 마감인 리마인더가 없습니다."
    case "scheduled":
      return "마감일이 설정된 리마인더가 없습니다."
    case "flagged":
      return "플래그 지정된 리마인더가 없습니다."
    case "completed":
      return "완료된 리마인더가 없습니다."
    default:
      return "리마인더가 없습니다."
  }
}

export function isSameSelection(a: Selection, b: Selection): boolean {
  if (a.type === "list" && b.type === "list") return a.listId === b.listId
  if (a.type === "smart" && b.type === "smart") return a.view === b.view
  if (a.type === "tag" && b.type === "tag") return a.name === b.name
  if (a.type === "search" && b.type === "search") return a.query === b.query
  return false
}

export function selectionKey(selection: Selection): string {
  switch (selection.type) {
    case "list":
      return `list-${selection.listId}`
    case "tag":
      return `tag-${selection.name}`
    case "search":
      return `search-${selection.query}`
    default:
      return `smart-${selection.view}`
  }
}
