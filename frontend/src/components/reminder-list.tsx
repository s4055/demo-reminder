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
import {
  ChevronDownIcon,
  ChevronRightIcon,
  ClipboardListIcon,
  FlagIcon,
  GripVerticalIcon,
  RepeatIcon,
} from "lucide-react"
import { Button } from "@/components/ui/button"
import { Checkbox } from "@/components/ui/checkbox"
import { Skeleton } from "@/components/ui/skeleton"
import { ClearCompletedDialog } from "@/components/clear-completed-dialog"
import { ReminderEditDialog } from "@/components/reminder-edit-dialog"
import { cn } from "@/lib/utils"
import { formatDueAt, isOverdue } from "@/lib/due-date"
import { PRIORITY_LABELS, priorityMark } from "@/lib/priority"
import { isRepeating, recurrenceOf, summarizeRecurrence } from "@/lib/repeat"
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
import { useShowCompleted } from "@/hooks/use-show-completed"

type ReminderActions = {
  onEdit: (reminder: Reminder) => void
  onToggleComplete: (reminder: Reminder) => void
  onToggleFlag: (reminder: Reminder) => void
  onDelete: (reminder: Reminder) => void
}

// 리스트 화면에서 부모 아래에 하위 작업을 펼쳐 보여줄 때 쓰는 상태. 스마트 뷰/태그 화면에는 넘기지 않는다.
type SubtaskView = {
  collapsed: boolean
  // 완료된 항목 숨기기를 켠 리스트에서는 완료된 하위 작업도 숨긴다.
  hideCompleted: boolean
  onToggleCollapsed: () => void
}

// 하위 작업까지 포함해 id로 리마인더를 찾는다.
function findReminder(
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
  // 사용자 리스트 화면에서만 미완료 항목을 드래그로 정렬하고, 완료 항목 보기/숨기기와 일괄 삭제를 제공한다.
  // 스마트 뷰와 태그 화면에서는 여러 리스트의 항목이 섞이므로 드래그를 끄고 소속 리스트를 함께 보여준다.
  const listId = selection.type === "list" ? selection.listId : null
  const [showCompleted, setShowCompleted] = useShowCompleted(listId)
  const [clearDialogOpen, setClearDialogOpen] = useState(false)
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
  // 접어 둔 부모 리마인더 id. 기본은 모두 펼친 상태다.
  const [collapsedIds, setCollapsedIds] = useState<ReadonlySet<number>>(
    () => new Set()
  )

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
  // 미완료 항목은 서버가 준 순서(표시 순서)를 유지한다.
  const incomplete = reminders.filter((reminder) => !reminder.completed)
  const completed = reminders
    .filter((reminder) => reminder.completed)
    .sort(byRecentCompletion)
  // 리스트 화면의 완료 항목 수. 부모 아래에 묶여 오는 완료된 하위 작업도 센다.
  const completedCount =
    completed.length +
    reminders.reduce(
      (count, reminder) =>
        count + reminder.subtasks.filter((subtask) => subtask.completed).length,
      0
    )
  const listOf = (reminder: Reminder) =>
    listId === null
      ? lists?.find((list) => list.id === reminder.listId)
      : undefined
  // 리스트 화면만 서버가 최상위 리마인더와 그 하위 작업을 묶어서 주므로 부모 아래에 펼쳐 보여준다.
  // 스마트 뷰/태그 화면에서는 하위 작업도 개별 항목으로 오므로 그대로 나열한다.
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
  // 편집 중에 하위 작업이 추가/변경되면 대화상자에도 반영되도록 최신 조회 결과에서 다시 찾는다.
  // 삭제되어 찾을 수 없으면 닫힘 애니메이션 동안 마지막 내용을 유지한다.
  const editingReminder = editing
    ? (findReminder(reminders, editing.reminder.id) ?? editing.reminder)
    : null

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

function SortableReminderItem({
  reminder,
  actions,
  subtaskView,
}: {
  reminder: Reminder
  actions: ReminderActions
  subtaskView?: SubtaskView
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
      subtaskView={subtaskView}
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

// 리마인더 한 항목. subtaskView가 있으면 하위 작업을 들여쓰기해 함께 보여주고, 드래그 시에도 같이 움직인다.
function ReminderItem({
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
  list?: ReminderListType
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

function ReminderRow({
  reminder,
  list,
  actions,
  className,
  leading,
  trailing,
}: {
  reminder: Reminder
  list?: ReminderListType
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
