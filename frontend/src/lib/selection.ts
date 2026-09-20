export type SmartView = "today" | "scheduled" | "all" | "flagged" | "completed"

export type Selection =
  | { type: "smart"; view: SmartView }
  | { type: "list"; listId: number }

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

export function isSameSelection(a: Selection, b: Selection): boolean {
  if (a.type === "list" && b.type === "list") return a.listId === b.listId
  if (a.type === "smart" && b.type === "smart") return a.view === b.view
  return false
}

export function selectionKey(selection: Selection): string {
  return selection.type === "list"
    ? `list-${selection.listId}`
    : `smart-${selection.view}`
}
