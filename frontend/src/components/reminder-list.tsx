"use client"

import { useState } from "react"
import { ClipboardListIcon, FlagIcon } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Checkbox } from "@/components/ui/checkbox"
import { Skeleton } from "@/components/ui/skeleton"
import { ReminderEditDialog } from "@/components/reminder-edit-dialog"
import { cn } from "@/lib/utils"
import { formatDueAt, isOverdue } from "@/lib/due-date"
import type { ReminderList as ReminderListType } from "@/lib/lists-api"
import type { Reminder } from "@/lib/reminders-api"
import { emptyMessage, type Selection } from "@/lib/selection"
import { useLists } from "@/hooks/use-lists"
import {
  useDeleteReminder,
  useReminders,
  useToggleReminderComplete,
  useToggleReminderFlag,
} from "@/hooks/use-reminders"

// 미완료 항목은 서버가 준 순서(생성순)를 유지하고, 완료 항목은 아래에 완료 시각 최신순으로 둔다.
function sortForDisplay(reminders: Reminder[]): Reminder[] {
  const completedTime = (reminder: Reminder) =>
    reminder.completedAt ? Date.parse(reminder.completedAt) : 0
  const byRecentCompletion = (a: Reminder, b: Reminder) =>
    completedTime(b) - completedTime(a)
  return [
    ...reminders.filter((reminder) => !reminder.completed),
    ...reminders.filter((reminder) => reminder.completed).sort(byRecentCompletion),
  ]
}

export function ReminderList({ selection }: { selection: Selection }) {
  const {
    data: reminders,
    isLoading,
    isError,
    refetch,
  } = useReminders(selection)
  const { data: lists } = useLists()
  const toggleComplete = useToggleReminderComplete()
  const toggleFlag = useToggleReminderFlag()
  const deleteReminder = useDeleteReminder()
  // 닫힘 애니메이션 동안 폼 내용이 유지되도록 open 과 대상 리마인더를 함께 보관한다.
  const [editing, setEditing] = useState<{
    reminder: Reminder
    open: boolean
  } | null>(null)

  if (isLoading) {
    return (
      <div
        className="flex w-full flex-col gap-1"
        aria-label="리마인더 불러오는 중"
      >
        <Skeleton className="h-12 w-full" />
        <Skeleton className="h-12 w-full" />
        <Skeleton className="h-12 w-full" />
      </div>
    )
  }

  if (isError) {
    return (
      <div className="flex flex-col items-start gap-2">
        <p className="text-sm text-destructive">
          리마인더 목록을 불러오지 못했습니다.
        </p>
        <Button variant="outline" size="sm" onClick={() => refetch()}>
          다시 시도
        </Button>
      </div>
    )
  }

  if (!reminders || reminders.length === 0) {
    return (
      <div className="flex flex-col items-center gap-2 rounded-lg border border-dashed border-border py-12 text-center">
        <ClipboardListIcon className="size-8 text-muted-foreground" />
        <p className="text-sm text-muted-foreground">
          {emptyMessage(selection)}
        </p>
      </div>
    )
  }

  // 스마트 뷰에서는 여러 리스트의 항목이 섞이므로 소속 리스트를 함께 보여준다.
  const showList = selection.type === "smart"

  return (
    <>
      <ul className="flex w-full flex-col gap-1">
        {sortForDisplay(reminders).map((reminder) => (
          <ReminderItem
            key={reminder.id}
            reminder={reminder}
            list={
              showList
                ? lists?.find((list) => list.id === reminder.listId)
                : undefined
            }
            onEdit={() => setEditing({ reminder, open: true })}
            onToggleComplete={() => toggleComplete.mutate(reminder.id)}
            onToggleFlag={() => toggleFlag.mutate(reminder.id)}
            onDelete={() => deleteReminder.mutate(reminder.id)}
          />
        ))}
      </ul>
      {editing && (
        <ReminderEditDialog
          open={editing.open}
          onOpenChange={(open) =>
            setEditing((prev) => prev && { ...prev, open })
          }
          reminder={editing.reminder}
        />
      )}
    </>
  )
}

function ReminderItem({
  reminder,
  list,
  onEdit,
  onToggleComplete,
  onToggleFlag,
  onDelete,
}: {
  reminder: Reminder
  list?: ReminderListType
  onEdit: () => void
  onToggleComplete: () => void
  onToggleFlag: () => void
  onDelete: () => void
}) {
  const overdue =
    reminder.dueAt !== null && !reminder.completed && isOverdue(reminder.dueAt)

  return (
    <li className="flex items-center gap-2.5 rounded-lg border border-border px-3 py-2">
      <Checkbox
        checked={reminder.completed}
        onCheckedChange={onToggleComplete}
        aria-label={`${reminder.title} 완료 처리`}
      />
      <button
        type="button"
        onClick={onEdit}
        aria-label={`${reminder.title} 편집`}
        className="flex min-w-0 flex-1 cursor-pointer flex-col gap-0.5 rounded-sm text-left outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
      >
        <span
          className={cn(
            "text-sm",
            reminder.completed && "text-muted-foreground line-through"
          )}
        >
          {reminder.title}
        </span>
        {(reminder.dueAt || list) && (
          <span className="flex items-center gap-2 text-xs text-muted-foreground">
            {reminder.dueAt && (
              <span className={cn(overdue && "text-destructive")}>
                {formatDueAt(reminder.dueAt)}
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
          </span>
        )}
        {reminder.memo && (
          <span className="truncate text-xs text-muted-foreground">
            {reminder.memo}
          </span>
        )}
      </button>
      <Button
        variant="ghost"
        size="icon-sm"
        onClick={onToggleFlag}
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
        onClick={onDelete}
        aria-label={`${reminder.title} 삭제`}
      >
        삭제
      </Button>
    </li>
  )
}
