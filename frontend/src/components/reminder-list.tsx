"use client"

import { useId, useState, type CSSProperties, type ReactNode } from "react"
import {
  DndContext,
  KeyboardSensor,
  PointerSensor,
  closestCenter,
  useSensor,
  useSensors,
  type DragEndEvent,
} from "@dnd-kit/core"
import {
  SortableContext,
  arrayMove,
  sortableKeyboardCoordinates,
  useSortable,
  verticalListSortingStrategy,
} from "@dnd-kit/sortable"
import { CSS } from "@dnd-kit/utilities"
import { ClipboardListIcon, FlagIcon, GripVerticalIcon } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Checkbox } from "@/components/ui/checkbox"
import { Skeleton } from "@/components/ui/skeleton"
import { ReminderEditDialog } from "@/components/reminder-edit-dialog"
import { cn } from "@/lib/utils"
import { formatDueAt, isOverdue } from "@/lib/due-date"
import { PRIORITY_LABELS, priorityMark } from "@/lib/priority"
import type { ReminderList as ReminderListType } from "@/lib/lists-api"
import type { Reminder } from "@/lib/reminders-api"
import { emptyMessage, type Selection } from "@/lib/selection"
import { useLists } from "@/hooks/use-lists"
import {
  useDeleteReminder,
  useReminders,
  useReorderReminders,
  useToggleReminderComplete,
  useToggleReminderFlag,
} from "@/hooks/use-reminders"

type ReminderActions = {
  onEdit: (reminder: Reminder) => void
  onToggleComplete: (reminder: Reminder) => void
  onToggleFlag: (reminder: Reminder) => void
  onDelete: (reminder: Reminder) => void
}

// 완료 항목은 미완료 항목 아래에 완료 시각 최신순으로 둔다.
function byRecentCompletion(a: Reminder, b: Reminder): number {
  const completedTime = (reminder: Reminder) =>
    reminder.completedAt ? Date.parse(reminder.completedAt) : 0
  return completedTime(b) - completedTime(a)
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
  const reorderReminders = useReorderReminders()
  const dndId = useId()
  const sensors = useSensors(
    // 살짝 끌어야 드래그가 시작되어 핸들 클릭과 구분된다.
    useSensor(PointerSensor, { activationConstraint: { distance: 4 } }),
    useSensor(KeyboardSensor, {
      coordinateGetter: sortableKeyboardCoordinates,
    })
  )
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
  // 사용자 리스트 화면에서만 미완료 항목을 드래그로 정렬한다. 미완료 항목은 서버가 준 순서(표시 순서)를 유지한다.
  // 스마트 뷰에서는 여러 리스트의 항목이 섞이므로 드래그를 끄고 소속 리스트를 함께 보여준다.
  const listId = selection.type === "list" ? selection.listId : null
  const incomplete = reminders.filter((reminder) => !reminder.completed)
  const completed = reminders
    .filter((reminder) => reminder.completed)
    .sort(byRecentCompletion)
  const listOf = (reminder: Reminder) =>
    listId === null
      ? lists?.find((list) => list.id === reminder.listId)
      : undefined

  function handleDragEnd({ active, over }: DragEndEvent) {
    if (listId === null || !over || active.id === over.id) return
    const ids = incomplete.map((reminder) => reminder.id)
    const from = ids.indexOf(Number(active.id))
    const to = ids.indexOf(Number(over.id))
    if (from < 0 || to < 0) return
    reorderReminders.mutate({ listId, ids: arrayMove(ids, from, to) })
  }

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
        {completed.length > 0 && (
          <ul className="flex w-full flex-col gap-1">
            {completed.map((reminder) => (
              <ReminderItem
                key={reminder.id}
                reminder={reminder}
                list={listOf(reminder)}
                actions={actions}
              />
            ))}
          </ul>
        )}
      </div>
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

function SortableReminderItem({
  reminder,
  actions,
}: {
  reminder: Reminder
  actions: ReminderActions
}) {
  const {
    attributes,
    listeners,
    setNodeRef,
    setActivatorNodeRef,
    transform,
    transition,
    isDragging,
  } = useSortable({ id: reminder.id })

  return (
    <ReminderItem
      reminder={reminder}
      actions={actions}
      itemRef={setNodeRef}
      style={{
        transform: CSS.Translate.toString(transform),
        transition,
      }}
      dragging={isDragging}
      dragHandle={
        <button
          type="button"
          ref={setActivatorNodeRef}
          {...attributes}
          {...listeners}
          aria-label={`${reminder.title} 순서 이동`}
          className="-ml-1.5 flex cursor-grab touch-none items-center rounded-sm text-muted-foreground outline-none focus-visible:ring-3 focus-visible:ring-ring/50 active:cursor-grabbing"
        >
          <GripVerticalIcon className="size-4" />
        </button>
      }
    />
  )
}

function ReminderItem({
  reminder,
  list,
  actions,
  itemRef,
  style,
  dragging,
  dragHandle,
}: {
  reminder: Reminder
  list?: ReminderListType
  actions: ReminderActions
  itemRef?: (node: HTMLElement | null) => void
  style?: CSSProperties
  dragging?: boolean
  dragHandle?: ReactNode
}) {
  const overdue =
    reminder.dueAt !== null && !reminder.completed && isOverdue(reminder.dueAt)
  const mark = priorityMark(reminder.priority)

  return (
    <li
      ref={itemRef}
      style={style}
      className={cn(
        "flex items-center gap-2.5 rounded-lg border border-border bg-background px-3 py-2",
        dragging && "relative z-10 shadow-md"
      )}
    >
      {dragHandle}
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
    </li>
  )
}
