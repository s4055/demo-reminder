"use client"

import { useState } from "react"
import {
  CalendarCheckIcon,
  CalendarClockIcon,
  CheckCircle2Icon,
  FlagIcon,
  InboxIcon,
  PlusIcon,
  Trash2Icon,
  type LucideIcon,
} from "lucide-react"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { ListCreateDialog } from "@/components/list-create-dialog"
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
  const { data: lists, isLoading, isError } = useLists()
  const [createOpen, setCreateOpen] = useState(false)
  const [listToDelete, setListToDelete] = useState<ReminderList | null>(null)

  return (
    <aside className="flex w-64 shrink-0 flex-col gap-4 border-r border-border bg-muted/30 p-3">
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
          <p className="px-2 text-sm text-muted-foreground">불러오는 중...</p>
        )}
        {isError && (
          <p className="px-2 text-sm text-destructive">
            리스트를 불러오지 못했습니다.
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
                >
                  <span
                    className="size-3 shrink-0 rounded-full"
                    style={{ backgroundColor: list.color ?? "#8E8E93" }}
                  />
                  <span className="flex-1 truncate">{list.name}</span>
                  <Badge variant="secondary" className="group-hover/item:opacity-0">
                    {list.reminderCount}
                  </Badge>
                </SidebarItem>
                <Button
                  variant="ghost"
                  size="icon-xs"
                  className="absolute top-1/2 right-1.5 -translate-y-1/2 opacity-0 group-hover/item:opacity-100 focus-visible:opacity-100"
                  onClick={() => setListToDelete(list)}
                  aria-label={`${list.name} 리스트 삭제`}
                >
                  <Trash2Icon />
                </Button>
              </li>
            )
          })}
        </ul>
      </section>

      <Button
        variant="ghost"
        className="mt-auto justify-start"
        onClick={() => setCreateOpen(true)}
      >
        <PlusIcon />
        리스트 추가
      </Button>

      <ListCreateDialog
        open={createOpen}
        onOpenChange={setCreateOpen}
        onCreated={(list) => onSelect({ type: "list", listId: list.id })}
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
    </aside>
  )
}

function SidebarItem({
  active,
  onClick,
  children,
}: {
  active: boolean
  onClick: () => void
  children: React.ReactNode
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      aria-current={active ? "page" : undefined}
      className={cn(
        "flex w-full items-center gap-2 rounded-lg px-2 py-1.5 text-left text-sm outline-none transition-colors hover:bg-muted focus-visible:ring-3 focus-visible:ring-ring/50",
        active && "bg-muted font-medium"
      )}
    >
      {children}
    </button>
  )
}
