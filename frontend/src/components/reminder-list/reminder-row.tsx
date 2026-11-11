"use client"

import type { ReactNode } from "react"
import { FlagIcon, RepeatIcon } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Checkbox } from "@/components/ui/checkbox"
import { formatDueAt, isOverdue } from "@/lib/due-date"
import type { ReminderList } from "@/lib/lists-api"
import { PRIORITY_LABELS, priorityMark } from "@/lib/priority"
import type { Reminder } from "@/lib/reminders-api"
import { isRepeating, recurrenceOf, summarizeRecurrence } from "@/lib/repeat"
import { cn } from "@/lib/utils"
import type { ReminderActions } from "@/components/reminder-list/types"

export function ReminderRow({
  reminder,
  list,
  actions,
  className,
  leading,
  trailing,
}: {
  reminder: Reminder
  list?: ReminderList
  actions: ReminderActions
  className?: string
  leading?: ReactNode
  trailing?: ReactNode
}) {
  const overdue =
    reminder.dueAt !== null && !reminder.completed && isOverdue(reminder.dueAt)
  const mark = priorityMark(reminder.priority)

  return (
    <div
      className={cn(
        "flex items-center gap-2.5 rounded-lg border border-border bg-background px-3 py-2",
        className
      )}
    >
      {leading}
      <Checkbox
        checked={reminder.completed}
        onCheckedChange={() => actions.onToggleComplete(reminder)}
        aria-label={`${reminder.title} 완료 처리`}
      />
      <button
        type="button"
        onClick={() => actions.onEdit(reminder)}
        aria-label={`${reminder.title} 편집`}
        className="flex min-w-0 flex-1 cursor-pointer flex-col gap-0.5 rounded-sm text-left outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
      >
        <span
          className={cn(
            "text-sm",
            reminder.completed && "text-muted-foreground line-through"
          )}
        >
          {mark && (
            <>
              <span
                aria-hidden
                className={cn(
                  "mr-1 font-semibold",
                  !reminder.completed && "text-primary"
                )}
              >
                {mark}
              </span>
              <span className="sr-only">
                우선순위 {PRIORITY_LABELS[reminder.priority]},
              </span>
            </>
          )}
          {reminder.title}
        </span>
        {(reminder.dueAt || list || reminder.tags.length > 0) && (
          <span className="flex flex-wrap items-center gap-x-2 gap-y-0.5 text-xs text-muted-foreground">
            {reminder.dueAt && (
              <span className={cn(overdue && "text-destructive")}>
                {formatDueAt(reminder.dueAt)}
              </span>
            )}
            {isRepeating(reminder.repeatRule) && (
              <span className="flex items-center gap-1">
                <RepeatIcon aria-hidden className="size-3" />
                <span className="sr-only">반복:</span>
                {summarizeRecurrence(recurrenceOf(reminder))}
              </span>
            )}
            {list && (
              <span className="flex items-center gap-1">
                <span
                  className="size-2 rounded-full"
                  style={{ backgroundColor: list.color ?? "#8E8E93" }}
                />
                {list.name}
              </span>
            )}
            {reminder.tags.map((tag) => (
              <span key={tag} className="text-primary/80">
                #{tag}
              </span>
            ))}
          </span>
        )}
        {reminder.memo && (
          <span className="truncate text-xs text-muted-foreground">
            {reminder.memo}
          </span>
        )}
      </button>
      {trailing}
      <Button
        variant="ghost"
        size="icon-sm"
        onClick={() => actions.onToggleFlag(reminder)}
        aria-label={`${reminder.title} 플래그 ${reminder.flagged ? "해제" : "지정"}`}
        aria-pressed={reminder.flagged}
      >
        <FlagIcon
          className={cn(reminder.flagged && "fill-orange-500 text-orange-500")}
        />
      </Button>
      <Button
        variant="ghost"
        size="sm"
        onClick={() => actions.onDelete(reminder)}
        aria-label={`${reminder.title} 삭제`}
      >
        삭제
      </Button>
    </div>
  )
}
