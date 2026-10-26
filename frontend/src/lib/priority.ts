export type Priority = "NONE" | "LOW" | "MEDIUM" | "HIGH"

export const PRIORITIES: Priority[] = ["NONE", "LOW", "MEDIUM", "HIGH"]

export const PRIORITY_LABELS: Record<Priority, string> = {
  NONE: "없음",
  LOW: "낮음",
  MEDIUM: "보통",
  HIGH: "높음",
}

const PRIORITY_MARKS: Record<Priority, string> = {
  NONE: "",
  LOW: "!",
  MEDIUM: "!!",
  HIGH: "!!!",
}

export function priorityMark(priority: Priority): string {
  return PRIORITY_MARKS[priority]
}
