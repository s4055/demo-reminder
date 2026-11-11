import type { Reminder } from "@/lib/reminders-api"

export type ReminderActions = {
  onEdit: (reminder: Reminder) => void
  onToggleComplete: (reminder: Reminder) => void
  onToggleFlag: (reminder: Reminder) => void
  onDelete: (reminder: Reminder) => void
}

export type SubtaskView = {
  collapsed: boolean
  hideCompleted: boolean
  onToggleCollapsed: () => void
}
