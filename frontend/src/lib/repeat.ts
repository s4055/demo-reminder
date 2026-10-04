export type RepeatRule = "NONE" | "DAILY" | "WEEKLY" | "MONTHLY" | "YEARLY"

export const REPEAT_RULES: RepeatRule[] = [
  "NONE",
  "DAILY",
  "WEEKLY",
  "MONTHLY",
  "YEARLY",
]

export const REPEAT_RULE_LABELS: Record<RepeatRule, string> = {
  NONE: "반복 안 함",
  DAILY: "매일",
  WEEKLY: "매주",
  MONTHLY: "매월",
  YEARLY: "매년",
}

export function isRepeating(rule: RepeatRule): boolean {
  return rule !== "NONE"
}
