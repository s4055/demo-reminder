"use client"

import { Button } from "@/components/ui/button"
import { Checkbox } from "@/components/ui/checkbox"
import { cn } from "@/lib/utils"
import type { Reminder } from "@/lib/reminders-api"
import {
  useDeleteReminder,
  useReminders,
  useToggleReminderComplete,
} from "@/hooks/use-reminders"

export function ReminderList() {
  const { data: reminders, isLoading, isError } = useReminders()
  const toggleComplete = useToggleReminderComplete()
  const deleteReminder = useDeleteReminder()

  if (isLoading) {
    return <p className="text-sm text-muted-foreground">불러오는 중...</p>
  }

  if (isError) {
    return (
      <p className="text-sm text-destructive">
        리마인더 목록을 불러오지 못했습니다.
      </p>
    )
  }

  if (!reminders || reminders.length === 0) {
    return <p className="text-sm text-muted-foreground">리마인더가 없습니다.</p>
  }

  return (
    <ul className="flex w-full flex-col gap-1">
      {reminders.map((reminder) => (
        <ReminderItem
          key={reminder.id}
          reminder={reminder}
          onToggleComplete={() => toggleComplete.mutate(reminder.id)}
          onDelete={() => deleteReminder.mutate(reminder.id)}
        />
      ))}
    </ul>
  )
}

function ReminderItem({
  reminder,
  onToggleComplete,
  onDelete,
}: {
  reminder: Reminder
  onToggleComplete: () => void
  onDelete: () => void
}) {
  return (
    <li className="flex items-center gap-2.5 rounded-lg border border-border px-3 py-2">
      <Checkbox
        checked={reminder.completed}
        onCheckedChange={onToggleComplete}
        aria-label={`${reminder.title} 완료 처리`}
      />
      <span
        className={cn(
          "flex-1 text-sm",
          reminder.completed && "text-muted-foreground line-through"
        )}
      >
        {reminder.title}
      </span>
      <Button
        variant="ghost"
        size="sm"
        onClick={onDelete}
        aria-label={`${reminder.title} 삭제`}
      >
        삭제
      </Button>
    </li>
  )
}
