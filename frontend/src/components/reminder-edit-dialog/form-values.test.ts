import { describe, expect, it } from "vitest"
import {
  formValuesOf,
  toUpdateInput,
  type ReminderFormValues,
} from "@/components/reminder-edit-dialog/form-values"
import type { Reminder } from "@/lib/reminders-api"
import { NO_REPEAT } from "@/lib/repeat"

function reminder(overrides: Partial<Reminder> = {}): Reminder {
  return {
    id: 1,
    title: "우유 사기",
    memo: null,
    completed: false,
    flagged: false,
    priority: "NONE",
    dueAt: null,
    repeatRule: "NONE",
    repeatInterval: 1,
    repeatDaysOfWeek: [],
    completedAt: null,
    listId: 3,
    sortOrder: 0,
    tags: [],
    parentId: null,
    subtasks: [],
    version: 2,
    createdAt: "2026-11-01T09:00:00",
    updatedAt: "2026-11-01T09:00:00",
    ...overrides,
  }
}

function values(overrides: Partial<ReminderFormValues> = {}): ReminderFormValues {
  return {
    title: "우유 사기",
    memo: "",
    dueAt: undefined,
    recurrence: NO_REPEAT,
    flagged: false,
    priority: "NONE",
    tagNames: [],
    listId: 3,
    ...overrides,
  }
}

describe("formValuesOf", () => {
  it("mapsReminderToFormValues", () => {
    const result = formValuesOf(
      reminder({
        memo: "저지방",
        dueAt: "2026-11-12T08:30:00",
        repeatRule: "WEEKLY",
        repeatInterval: 2,
        repeatDaysOfWeek: ["MONDAY"],
        flagged: true,
        priority: "HIGH",
        tags: ["장보기"],
      })
    )

    expect(result).toEqual({
      title: "우유 사기",
      memo: "저지방",
      dueAt: new Date(2026, 10, 12, 8, 30),
      recurrence: { rule: "WEEKLY", interval: 2, daysOfWeek: ["MONDAY"] },
      flagged: true,
      priority: "HIGH",
      tagNames: ["장보기"],
      listId: 3,
    })
  })

  it("usesEmptyMemoAndNoDueDateWhenMissing", () => {
    const result = formValuesOf(reminder())

    expect(result.memo).toBe("")
    expect(result.dueAt).toBeUndefined()
  })
})

describe("toUpdateInput", () => {
  it("buildsUpdateRequestWithVersion", () => {
    const input = toUpdateInput(
      values({
        title: "  우유 2개  ",
        memo: "  저지방  ",
        dueAt: new Date(2026, 10, 12, 8, 30),
        recurrence: { rule: "WEEKLY", interval: 1, daysOfWeek: ["FRIDAY", "MONDAY"] },
        flagged: true,
        priority: "LOW",
        tagNames: ["장보기"],
      }),
      7
    )

    expect(input).toEqual({
      title: "우유 2개",
      memo: "저지방",
      dueAt: "2026-11-12T08:30:00",
      flagged: true,
      priority: "LOW",
      tagNames: ["장보기"],
      repeatRule: "WEEKLY",
      repeatInterval: 1,
      repeatDaysOfWeek: ["MONDAY", "FRIDAY"],
      version: 7,
    })
  })

  it("sendsNullMemoWhenBlank", () => {
    expect(toUpdateInput(values({ memo: "   " }), 1).memo).toBeNull()
  })

  it("dropsRecurrenceWithoutDueDate", () => {
    const input = toUpdateInput(
      values({ recurrence: { rule: "DAILY", interval: 1, daysOfWeek: [] } }),
      1
    )

    expect(input).toMatchObject({
      dueAt: null,
      repeatRule: "NONE",
      repeatInterval: 1,
      repeatDaysOfWeek: [],
    })
  })
})
