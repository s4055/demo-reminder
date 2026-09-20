"use client"

import { useState } from "react"
import {
  CalendarCheckIcon,
  CalendarClockIcon,
  CheckCircle2Icon,
  FlagIcon,
  InboxIcon,
  PencilIcon,
  PlusIcon,
  Trash2Icon,
  type LucideIcon,
} from "lucide-react"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Skeleton } from "@/components/ui/skeleton"
import { ListFormDialog } from "@/components/list-form-dialog"
import { ListDeleteDialog } from "@/components/list-delete-dialog"
import { useLists } from "@/hooks/use-lists"
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
  // 닫힘 애니메이션 동안 폼 내용이 유지되도록 open 과 대상 리스트를 함께 보관한다.
  const [listForm, setListForm] = useState<{
    open: boolean
    list?: ReminderList
  }>({ open: false })
  const [listToDelete, setListToDelete] = useState<ReminderList | null>(null)

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
        <ul className="flex flex-col gap-0.5">
          {lists?.map((list) => {
            const listSelection: Selection = { type: "list", listId: list.id }
            return (
              <li key={list.id} className="group/item relative">
                <SidebarItem
                  active={isSameSelection(selection, listSelection)}
                  onClick={() => onSelect(listSelection)}
                  className="pr-14 md:pr-2"
                >
                  <span
                    className="size-3 shrink-0 rounded-full"
                    style={{ backgroundColor: list.color ?? "#8E8E93" }}
                  />
                  <span className="flex-1 truncate">{list.name}</span>
                  <Badge
                    variant="secondary"
                    className="md:group-hover/item:opacity-0"
                  >
                    {list.reminderCount}
                  </Badge>
                </SidebarItem>
                <div className="absolute top-1/2 right-1.5 flex -translate-y-1/2 focus-within:opacity-100 md:opacity-0 md:group-hover/item:opacity-100">
                  <Button
                    variant="ghost"
                    size="icon-xs"
                    onClick={() => setListForm({ open: true, list })}
                    aria-label={`${list.name} 리스트 편집`}
                  >
                    <PencilIcon />
                  </Button>
                  <Button
                    variant="ghost"
                    size="icon-xs"
                    onClick={() => setListToDelete(list)}
                    aria-label={`${list.name} 리스트 삭제`}
                  >
                    <Trash2Icon />
                  </Button>
                </div>
              </li>
            )
          })}
        </ul>
      </section>

      <Button
        variant="ghost"
        className="mt-auto justify-start"
        onClick={() => setListForm({ open: true })}
      >
        <PlusIcon />
        리스트 추가
      </Button>

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
