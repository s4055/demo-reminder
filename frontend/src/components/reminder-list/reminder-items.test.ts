import { describe, expect, it } from "vitest"
import {
  byRecentCompletion,
  findReminder,
  partitionByCompletion,
} from "@/components/reminder-list/reminder-items"
import type { Reminder } from "@/lib/reminders-api"

function reminder(id: number, overrides: Partial<Reminder> = {}): Reminder {
  return {
    id,
    title: `리마인더 ${id}`,
    memo: null,
    completed: false,
    flagged: false,
    priority: "NONE",
    dueAt: null,
    timeZone: null,
    repeatRule: "NONE",
    repeatInterval: 1,
    repeatDaysOfWeek: [],
    completedAt: null,
    listId: 1,
    sortOrder: id,
    tags: [],
    parentId: null,
    subtasks: [],
    version: 0,
    createdAt: "2026-11-01T00:00:00Z",
    updatedAt: "2026-11-01T00:00:00Z",
    ...overrides,
  }
}

function done(id: number, completedAt: string, overrides: Partial<Reminder> = {}) {
  return reminder(id, { completed: true, completedAt, ...overrides })
}

describe("findReminder", () => {
  const subtask = reminder(11, { parentId: 1 })
  const reminders = [reminder(1, { subtasks: [subtask] }), reminder(2)]

  it("findsTopLevelReminder", () => {
    expect(findReminder(reminders, 2)?.id).toBe(2)
  })

  it("findsSubtask", () => {
    expect(findReminder(reminders, 11)).toBe(subtask)
  })

  it("returnsUndefinedWhenMissing", () => {
    expect(findReminder(reminders, 99)).toBeUndefined()
    expect(findReminder(undefined, 1)).toBeUndefined()
  })
})

describe("byRecentCompletion", () => {
  it("putsRecentlyCompletedFirstAndMissingTimeLast", () => {
    const sorted = [
      done(1, "2026-11-01T00:00:00Z"),
      reminder(2),
      done(3, "2026-11-03T09:00:00Z"),
    ].sort(byRecentCompletion)

    expect(sorted.map((item) => item.id)).toEqual([3, 1, 2])
  })
})

describe("partitionByCompletion", () => {
  it("splitsIncompleteAndCompletedInRecentOrder", () => {
    const result = partitionByCompletion([
      done(1, "2026-11-01T00:00:00Z"),
      reminder(2),
      done(3, "2026-11-02T09:00:00Z"),
      reminder(4),
    ])

    expect(result.incomplete.map((item) => item.id)).toEqual([2, 4])
    expect(result.completed.map((item) => item.id)).toEqual([3, 1])
  })

  it("countsCompletedSubtasksOfEveryReminder", () => {
    const result = partitionByCompletion([
      reminder(1, {
        subtasks: [
          done(11, "2026-11-01T00:00:00Z", { parentId: 1 }),
          reminder(12, { parentId: 1 }),
        ],
      }),
      done(2, "2026-11-01T00:00:00Z", {
        subtasks: [done(21, "2026-11-01T00:00:00Z", { parentId: 2 })],
      }),
    ])

    expect(result.completedCount).toBe(3)
  })

  it("returnsEmptyResultForNoReminders", () => {
    expect(partitionByCompletion([])).toEqual({
      incomplete: [],
      completed: [],
      completedCount: 0,
    })
  })
})
