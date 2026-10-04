"use client"

import { useId, useState } from "react"
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
  CalendarCheckIcon,
  CalendarClockIcon,
  CheckCircle2Icon,
  FlagIcon,
  GripVerticalIcon,
  HashIcon,
  InboxIcon,
  PencilIcon,
  PlusIcon,
  Trash2Icon,
  XIcon,
  type LucideIcon,
} from "lucide-react"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Skeleton } from "@/components/ui/skeleton"
import { ListFormDialog } from "@/components/list-form-dialog"
import { ListDeleteDialog } from "@/components/list-delete-dialog"
import { NotificationToggle } from "@/components/notification-toggle"
import { useLists, useReorderLists } from "@/hooks/use-lists"
import { useDeleteTag, useTags } from "@/hooks/use-tags"
import type { ReminderList } from "@/lib/lists-api"
import {
  DEFAULT_SELECTION,
  SMART_VIEWS,
  isSameSelection,
  type Selection,
  type SmartView,
} from "@/lib/selection"
import { cn } from "@/lib/utils"

const SMART_VIEW_ICONS: Record<SmartView, LucideIcon> = {
  today: CalendarCheckIcon,
  scheduled: CalendarClockIcon,
  all: InboxIcon,
  flagged: FlagIcon,
  completed: CheckCircle2Icon,
}

export function Sidebar({
  selection,
  onSelect,
}: {
  selection: Selection
  onSelect: (selection: Selection) => void
}) {
  const { data: lists, isLoading, isError, refetch } = useLists()
  const reorderLists = useReorderLists()
  const dndId = useId()
  const sensors = useSensors(
    // 살짝 끌어야 드래그가 시작되어 핸들 클릭과 구분된다.
    useSensor(PointerSensor, { activationConstraint: { distance: 4 } }),
    useSensor(KeyboardSensor, {
      coordinateGetter: sortableKeyboardCoordinates,
    })
  )
  // 닫힘 애니메이션 동안 폼 내용이 유지되도록 open 과 대상 리스트를 함께 보관한다.
  const [listForm, setListForm] = useState<{
    open: boolean
    list?: ReminderList
  }>({ open: false })
  const [listToDelete, setListToDelete] = useState<ReminderList | null>(null)

  function handleDragEnd({ active, over }: DragEndEvent) {
    if (!lists || !over || active.id === over.id) return
    const ids = lists.map((list) => list.id)
    const from = ids.indexOf(Number(active.id))
    const to = ids.indexOf(Number(over.id))
    if (from < 0 || to < 0) return
    reorderLists.mutate(arrayMove(ids, from, to))
  }

  return (
    <div className="flex h-full flex-col gap-4 overflow-y-auto p-3">
      <nav className="flex flex-col gap-0.5">
        {SMART_VIEWS.map(({ view, label }) => {
          const Icon = SMART_VIEW_ICONS[view]
          const smartSelection: Selection = { type: "smart", view }
          return (
            <SidebarItem
              key={view}
              active={isSameSelection(selection, smartSelection)}
              onClick={() => onSelect(smartSelection)}
            >
              <Icon className="size-4 text-muted-foreground" />
              <span className="flex-1 truncate">{label}</span>
            </SidebarItem>
          )
        })}
      </nav>

      <section className="flex flex-col gap-1">
        <h2 className="px-2 text-xs font-semibold text-muted-foreground">
          나의 리스트
        </h2>
        {isLoading && (
          <div className="flex flex-col gap-1.5 px-2" aria-label="리스트 불러오는 중">
            <Skeleton className="h-6 w-full" />
            <Skeleton className="h-6 w-full" />
            <Skeleton className="h-6 w-2/3" />
          </div>
        )}
        {isError && (
          <div className="flex flex-col items-start gap-1 px-2">
            <p className="text-sm text-destructive">
              리스트를 불러오지 못했습니다.
            </p>
            <Button variant="outline" size="xs" onClick={() => refetch()}>
              다시 시도
            </Button>
          </div>
        )}
        {lists?.length === 0 && (
          <p className="px-2 text-sm text-muted-foreground">
            리스트가 없습니다. 아래에서 새 리스트를 추가해 보세요.
          </p>
        )}
        <DndContext
          id={dndId}
          sensors={sensors}
          collisionDetection={closestCenter}
          onDragEnd={handleDragEnd}
        >
          <SortableContext
            items={lists?.map((list) => list.id) ?? []}
            strategy={verticalListSortingStrategy}
          >
            <ul className="flex flex-col gap-0.5">
              {lists?.map((list) => (
                <SortableListItem
                  key={list.id}
                  list={list}
                  active={isSameSelection(selection, {
                    type: "list",
                    listId: list.id,
                  })}
                  onSelect={() => onSelect({ type: "list", listId: list.id })}
                  onEdit={() => setListForm({ open: true, list })}
                  onDelete={() => setListToDelete(list)}
                />
              ))}
            </ul>
          </SortableContext>
        </DndContext>
      </section>

      <TagSection selection={selection} onSelect={onSelect} />

      <div className="mt-auto flex flex-col gap-1">
        <NotificationToggle />
        <Button
          variant="ghost"
          className="justify-start"
          onClick={() => setListForm({ open: true })}
        >
          <PlusIcon />
          리스트 추가
        </Button>
      </div>

      <ListFormDialog
        open={listForm.open}
        onOpenChange={(open) => setListForm((prev) => ({ ...prev, open }))}
        list={listForm.list}
        onSaved={(list) => {
          if (!listForm.list) onSelect({ type: "list", listId: list.id })
        }}
      />
      <ListDeleteDialog
        list={listToDelete}
        onOpenChange={(open) => {
          if (!open) setListToDelete(null)
        }}
        onDeleted={(list) => {
          if (selection.type === "list" && selection.listId === list.id) {
            onSelect(DEFAULT_SELECTION)
          }
        }}
      />
    </div>
  )
}

function SortableListItem({
  list,
  active,
  onSelect,
  onEdit,
  onDelete,
}: {
  list: ReminderList
  active: boolean
  onSelect: () => void
  onEdit: () => void
  onDelete: () => void
}) {
  const {
    attributes,
    listeners,
    setNodeRef,
    setActivatorNodeRef,
    transform,
    transition,
    isDragging,
  } = useSortable({ id: list.id })

  return (
    <li
      ref={setNodeRef}
      style={{ transform: CSS.Translate.toString(transform), transition }}
      className={cn(
        "group/item relative flex items-center rounded-lg",
        isDragging && "z-10 bg-background shadow-md"
      )}
    >
      <button
        type="button"
        ref={setActivatorNodeRef}
        {...attributes}
        {...listeners}
        aria-label={`${list.name} 리스트 순서 이동`}
        className="flex h-7 w-4 shrink-0 cursor-grab touch-none items-center justify-center rounded-sm text-muted-foreground outline-none focus-visible:opacity-100 focus-visible:ring-3 focus-visible:ring-ring/50 active:cursor-grabbing md:opacity-0 md:group-hover/item:opacity-100"
      >
        <GripVerticalIcon className="size-3.5" />
      </button>
      <SidebarItem
        active={active}
        onClick={onSelect}
        className="min-w-0 flex-1 pr-14 md:pr-2"
      >
        <span
          className="size-3 shrink-0 rounded-full"
          style={{ backgroundColor: list.color ?? "#8E8E93" }}
        />
        <span className="flex-1 truncate">{list.name}</span>
        <Badge variant="secondary" className="md:group-hover/item:opacity-0">
          {list.reminderCount}
        </Badge>
      </SidebarItem>
      <div className="absolute top-1/2 right-1.5 flex -translate-y-1/2 focus-within:opacity-100 md:opacity-0 md:group-hover/item:opacity-100">
        <Button
          variant="ghost"
          size="icon-xs"
          onClick={onEdit}
          aria-label={`${list.name} 리스트 편집`}
        >
          <PencilIcon />
        </Button>
        <Button
          variant="ghost"
          size="icon-xs"
          onClick={onDelete}
          aria-label={`${list.name} 리스트 삭제`}
        >
          <Trash2Icon />
        </Button>
      </div>
    </li>
  )
}

// 리마인더에 붙어 있는 태그만 보여주며, 태그가 하나도 없으면 섹션을 숨긴다.
function TagSection({
  selection,
  onSelect,
}: {
  selection: Selection
  onSelect: (selection: Selection) => void
}) {
  const { data: tags } = useTags()
  const deleteTag = useDeleteTag()

  if (!tags || tags.length === 0) return null

  return (
    <section className="flex flex-col gap-1">
      <h2 className="px-2 text-xs font-semibold text-muted-foreground">태그</h2>
      <ul className="flex flex-col gap-0.5">
        {tags.map((tag) => {
          const tagSelection: Selection = { type: "tag", name: tag.name }
          return (
            <li key={tag.id} className="group/item relative">
              <SidebarItem
                active={isSameSelection(selection, tagSelection)}
                onClick={() => onSelect(tagSelection)}
                className="pr-8 md:pr-2"
              >
                <HashIcon className="size-4 text-muted-foreground" />
                <span className="flex-1 truncate">{tag.name}</span>
                <Badge
                  variant="secondary"
                  className="md:group-hover/item:opacity-0"
                >
                  {tag.reminderCount}
                </Badge>
              </SidebarItem>
              <div className="absolute top-1/2 right-1.5 flex -translate-y-1/2 focus-within:opacity-100 md:opacity-0 md:group-hover/item:opacity-100">
                <Button
                  variant="ghost"
                  size="icon-xs"
                  disabled={deleteTag.isPending}
                  onClick={() =>
                    deleteTag.mutate(tag.id, {
                      onSuccess: () => {
                        if (isSameSelection(selection, tagSelection)) {
                          onSelect(DEFAULT_SELECTION)
                        }
                      },
                    })
                  }
                  aria-label={`${tag.name} 태그 삭제`}
                  title="태그 삭제 (리마인더는 유지)"
                >
                  <XIcon />
                </Button>
              </div>
            </li>
          )
        })}
      </ul>
    </section>
  )
}

function SidebarItem({
  active,
  onClick,
  className,
  children,
}: {
  active: boolean
  onClick: () => void
  className?: string
  children: React.ReactNode
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      aria-current={active ? "page" : undefined}
      className={cn(
        "flex w-full items-center gap-2 rounded-lg px-2 py-1.5 text-left text-sm outline-none transition-colors hover:bg-muted focus-visible:ring-3 focus-visible:ring-ring/50",
        active && "bg-muted font-medium",
        className
      )}
    >
      {children}
    </button>
  )
}
