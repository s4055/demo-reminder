"use client"

import type { CSSProperties, ReactNode } from "react"
import { ChevronDownIcon, ChevronRightIcon } from "lucide-react"
import type { ReminderList } from "@/lib/lists-api"
import type { Reminder } from "@/lib/reminders-api"
import { cn } from "@/lib/utils"
import { ReminderRow } from "@/components/reminder-list/reminder-row"
import type { ReminderActions, SubtaskView } from "@/components/reminder-list/types"

export function ReminderItem({
  reminder,
  list,
  actions,
  subtaskView,
  itemRef,
  style,
  dragging,
  dragHandle,
}: {
  reminder: Reminder
  list?: ReminderList
  actions: ReminderActions
  subtaskView?: SubtaskView
  itemRef?: (node: HTMLElement | null) => void
  style?: CSSProperties
  dragging?: boolean
  dragHandle?: ReactNode
}) {
  const allSubtasks = subtaskView ? reminder.subtasks : []
  const subtasks = subtaskView?.hideCompleted
    ? allSubtasks.filter((subtask) => !subtask.completed)
    : allSubtasks
  const expanded = subtasks.length > 0 && !subtaskView?.collapsed
  const subtasksId = `reminder-${reminder.id}-subtasks`

  return (
    <li
      ref={itemRef}
      style={style}
      className={cn("flex flex-col gap-1", dragging && "relative z-10")}
    >
      <ReminderRow
        reminder={reminder}
        list={list}
        actions={actions}
        className={cn(dragging && "shadow-md")}
        leading={
          <>
            {dragHandle}
            {subtasks.length > 0 && subtaskView && (
              <button
                type="button"
                onClick={subtaskView.onToggleCollapsed}
                aria-expanded={expanded}
                aria-controls={subtasksId}
                aria-label={`${reminder.title} 하위 작업 ${expanded ? "접기" : "펼치기"}`}
                className="-mx-1 flex items-center rounded-sm text-muted-foreground outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
              >
                {expanded ? (
                  <ChevronDownIcon className="size-4" />
                ) : (
                  <ChevronRightIcon className="size-4" />
                )}
              </button>
            )}
          </>
        }
        trailing={
          allSubtasks.length > 0 && (
            <span className="text-xs text-muted-foreground">
              {allSubtasks.filter((subtask) => subtask.completed).length}/
              {allSubtasks.length}
            </span>
          )
        }
      />
      {expanded && (
        <ul
          id={subtasksId}
          aria-label={`${reminder.title} 하위 작업`}
          className="ml-8 flex flex-col gap-1"
        >
          {subtasks.map((subtask) => (
            <li key={subtask.id}>
              <ReminderRow reminder={subtask} actions={actions} />
            </li>
          ))}
        </ul>
      )}
    </li>
  )
}
