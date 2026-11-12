import { parseISO } from "date-fns"
import { toDueAtParam } from "@/lib/due-date"
import type { Priority } from "@/lib/priority"
import type { Reminder, UpdateReminderInput } from "@/lib/reminders-api"
import { browserTimeZone } from "@/lib/timezone"
import {
  NO_REPEAT,
  recurrenceOf,
  toRepeatFields,
  type Recurrence,
} from "@/lib/repeat"

export type ReminderFormValues = {
  title: string
  memo: string
  dueAt: Date | undefined
  recurrence: Recurrence
  flagged: boolean
  priority: Priority
  tagNames: string[]
  listId: number | null
}

export function formValuesOf(reminder: Reminder): ReminderFormValues {
  return {
    title: reminder.title,
    memo: reminder.memo ?? "",
    dueAt: reminder.dueAt ? parseISO(reminder.dueAt) : undefined,
    recurrence: recurrenceOf(reminder),
    flagged: reminder.flagged,
    priority: reminder.priority,
    tagNames: reminder.tags,
    listId: reminder.listId,
  }
}

export function toUpdateInput(
  values: ReminderFormValues,
  version: number
): UpdateReminderInput {
  return {
    title: values.title.trim(),
    memo: values.memo.trim() || null,
    dueAt: values.dueAt ? toDueAtParam(values.dueAt) : null,
    timeZone: browserTimeZone(),
    flagged: values.flagged,
    priority: values.priority,
    tagNames: values.tagNames,
    ...toRepeatFields(values.dueAt ? values.recurrence : NO_REPEAT),
    version,
  }
}
