import { useState } from "react"
import { useLists } from "@/hooks/use-lists"
import { useReminders } from "@/hooks/use-reminders"
import { useShowCompleted } from "@/hooks/use-show-completed"
import type { Reminder } from "@/lib/reminders-api"
import type { Selection } from "@/lib/selection"
import { partitionByCompletion } from "@/components/reminder-list/reminder-items"
import type { SubtaskView } from "@/components/reminder-list/types"

export function useReminderListItems(selection: Selection) {
  const query = useReminders(selection)
  const { data: lists } = useLists()
  const listId = selection.type === "list" ? selection.listId : null
  const [showCompleted, setShowCompleted] = useShowCompleted(listId)
  const [collapsedIds, setCollapsedIds] = useState<ReadonlySet<number>>(
    () => new Set()
  )

  const { incomplete, completed, completedCount } = partitionByCompletion(
    query.data ?? []
  )

  const listOf = (reminder: Reminder) =>
    listId === null
      ? lists?.find((list) => list.id === reminder.listId)
      : undefined

  const subtaskViewOf = (reminder: Reminder): SubtaskView | undefined =>
    listId === null
      ? undefined
      : {
          collapsed: collapsedIds.has(reminder.id),
          hideCompleted: !showCompleted,
          onToggleCollapsed: () =>
            setCollapsedIds((prev) => {
              const next = new Set(prev)
              if (!next.delete(reminder.id)) next.add(reminder.id)
              return next
            }),
        }

  return {
    query,
    listId,
    incomplete,
    completed,
    completedCount,
    showCompleted,
    setShowCompleted,
    listOf,
    subtaskViewOf,
  }
}
