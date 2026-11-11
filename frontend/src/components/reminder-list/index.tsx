"use client"

import { useState } from "react"
import { DndContext, closestCenter } from "@dnd-kit/core"
import {
  SortableContext,
  verticalListSortingStrategy,
} from "@dnd-kit/sortable"
import { ClipboardListIcon } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Skeleton } from "@/components/ui/skeleton"
import { ClearCompletedDialog } from "@/components/clear-completed-dialog"
import { ReminderEditDialog } from "@/components/reminder-edit-dialog"
import {
  useDeleteReminder,
  useToggleReminderComplete,
  useToggleReminderFlag,
} from "@/hooks/use-reminders"
import type { Reminder } from "@/lib/reminders-api"
import { emptyMessage, type Selection } from "@/lib/selection"
import { ReminderItem } from "@/components/reminder-list/reminder-item"
import { findReminder } from "@/components/reminder-list/reminder-items"
import { SortableReminderItem } from "@/components/reminder-list/sortable-reminder-item"
import type { ReminderActions } from "@/components/reminder-list/types"
import { useReminderDnd } from "@/components/reminder-list/use-reminder-dnd"
import { useReminderListItems } from "@/components/reminder-list/use-reminder-list-items"

export function ReminderList({ selection }: { selection: Selection }) {
  const {
    query: { data: reminders, isLoading, isError, refetch },
    listId,
    incomplete,
    completed,
    completedCount,
    showCompleted,
    setShowCompleted,
    listOf,
    subtaskViewOf,
  } = useReminderListItems(selection)
  const { dndId, sensors, handleDragEnd } = useReminderDnd(listId, incomplete)
  const toggleComplete = useToggleReminderComplete()
  const toggleFlag = useToggleReminderFlag()
  const deleteReminder = useDeleteReminder()
  const [clearDialogOpen, setClearDialogOpen] = useState(false)
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

  const actions: ReminderActions = {
    onEdit: (reminder) => setEditing({ reminder, open: true }),
    onToggleComplete: (reminder) => toggleComplete.mutate(reminder.id),
    onToggleFlag: (reminder) => toggleFlag.mutate(reminder.id),
    onDelete: (reminder) => deleteReminder.mutate(reminder.id),
  }
  // 편집 중에 하위 작업이 추가/변경되면 대화상자에도 반영되도록 최신 조회 결과에서 다시 찾는다.
  // 삭제되어 찾을 수 없으면 닫힘 애니메이션 동안 마지막 내용을 유지한다.
  const editingReminder = editing
    ? (findReminder(reminders, editing.reminder.id) ?? editing.reminder)
    : null

  return (
    <>
      <div className="flex w-full flex-col gap-1">
        {listId !== null ? (
          <DndContext
            id={dndId}
            sensors={sensors}
            collisionDetection={closestCenter}
            onDragEnd={handleDragEnd}
          >
            <SortableContext
              items={incomplete.map((reminder) => reminder.id)}
              strategy={verticalListSortingStrategy}
            >
              <ul className="flex w-full flex-col gap-1">
                {incomplete.map((reminder) => (
                  <SortableReminderItem
                    key={reminder.id}
                    reminder={reminder}
                    actions={actions}
                    subtaskView={subtaskViewOf(reminder)}
                  />
                ))}
              </ul>
            </SortableContext>
          </DndContext>
        ) : (
          <ul className="flex w-full flex-col gap-1">
            {incomplete.map((reminder) => (
              <ReminderItem
                key={reminder.id}
                reminder={reminder}
                list={listOf(reminder)}
                actions={actions}
              />
            ))}
          </ul>
        )}
        {listId !== null && completedCount > 0 && (
          <div className="flex items-center justify-between gap-2 px-1 pt-2 text-xs text-muted-foreground">
            <span>완료된 항목 {completedCount}개</span>
            <div className="flex items-center gap-1">
              <Button
                variant="ghost"
                size="xs"
                onClick={() => setClearDialogOpen(true)}
              >
                지우기
              </Button>
              <Button
                variant="ghost"
                size="xs"
                onClick={() => setShowCompleted(!showCompleted)}
                aria-pressed={showCompleted}
              >
                {showCompleted ? "숨기기" : "보기"}
              </Button>
            </div>
          </div>
        )}
        {showCompleted && completed.length > 0 && (
          <ul className="flex w-full flex-col gap-1">
            {completed.map((reminder) => (
              <ReminderItem
                key={reminder.id}
                reminder={reminder}
                list={listOf(reminder)}
                actions={actions}
                subtaskView={subtaskViewOf(reminder)}
              />
            ))}
          </ul>
        )}
      </div>
      {listId !== null && (
        <ClearCompletedDialog
          listId={listId}
          completedCount={completedCount}
          open={clearDialogOpen}
          onOpenChange={setClearDialogOpen}
        />
      )}
      {editing && editingReminder && (
        <ReminderEditDialog
          open={editing.open}
          onOpenChange={(open) =>
            setEditing((prev) => prev && { ...prev, open })
          }
          reminder={editingReminder}
        />
      )}
    </>
  )
}
