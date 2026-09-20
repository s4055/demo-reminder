export type Selection =
  | { type: "all" }
  | { type: "list"; listId: number }

export const ALL_SELECTION: Selection = { type: "all" }

export function isSameSelection(a: Selection, b: Selection): boolean {
  if (a.type === "list" && b.type === "list") return a.listId === b.listId
  return a.type === b.type
}
