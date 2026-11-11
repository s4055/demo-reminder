import type { Reminder } from "@/lib/reminders-api"

export function findReminder(
  reminders: Reminder[] | undefined,
  id: number
): Reminder | undefined {
  for (const reminder of reminders ?? []) {
    if (reminder.id === id) return reminder
    const subtask = reminder.subtasks.find((item) => item.id === id)
    if (subtask) return subtask
  }
  return undefined
}

export function byRecentCompletion(a: Reminder, b: Reminder): number {
  const completedTime = (reminder: Reminder) =>
    reminder.completedAt ? Date.parse(reminder.completedAt) : 0
  return completedTime(b) - completedTime(a)
}

export function partitionByCompletion(reminders: Reminder[]) {
  const incomplete = reminders.filter((reminder) => !reminder.completed)
  const completed = reminders
    .filter((reminder) => reminder.completed)
    .sort(byRecentCompletion)
  const completedCount =
    completed.length +
    reminders.reduce(
      (count, reminder) =>
        count + reminder.subtasks.filter((subtask) => subtask.completed).length,
      0
    )
  return { incomplete, completed, completedCount }
}
