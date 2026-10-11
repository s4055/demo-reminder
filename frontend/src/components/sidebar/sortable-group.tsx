import { useSortable } from "@dnd-kit/sortable"
import { CSS } from "@dnd-kit/utilities"
import {
  ChevronRightIcon,
  FolderIcon,
  GripVerticalIcon,
  PencilIcon,
  Trash2Icon,
} from "lucide-react"
import { Button } from "@/components/ui/button"
import { groupDragId } from "@/components/sidebar/use-sidebar-dnd"
import type { ListGroup } from "@/lib/list-groups-api"
import { cn } from "@/lib/utils"

// 그룹 머리글(펼치기/접기, 이름, 리스트 수)과 그 아래 리스트. 그룹 전체가 함께 드래그된다.
export function SortableGroup({
  group,
  listCount,
  collapsed,
  onToggle,
  onRename,
  onDelete,
  children,
}: {
  group: ListGroup
  listCount: number
  collapsed: boolean
  onToggle: () => void
  onRename: () => void
  onDelete: () => void
  children: React.ReactNode
}) {
  const {
    attributes,
    listeners,
    setNodeRef,
    setActivatorNodeRef,
    transform,
    transition,
    isDragging,
  } = useSortable({ id: groupDragId(group.id) })

  return (
    <li
      ref={setNodeRef}
      style={{ transform: CSS.Translate.toString(transform), transition }}
      className={cn(
        "flex flex-col gap-0.5 rounded-lg",
        isDragging && "z-10 bg-background shadow-md"
      )}
    >
      <div className="group/item relative flex items-center rounded-lg">
        <button
          type="button"
          ref={setActivatorNodeRef}
          {...attributes}
          {...listeners}
          aria-label={`${group.name} 그룹 순서 이동`}
          className="flex h-7 w-4 shrink-0 cursor-grab touch-none items-center justify-center rounded-sm text-muted-foreground outline-none focus-visible:opacity-100 focus-visible:ring-3 focus-visible:ring-ring/50 active:cursor-grabbing md:opacity-0 md:group-hover/item:opacity-100"
        >
          <GripVerticalIcon className="size-3.5" />
        </button>
        <button
          type="button"
          onClick={onToggle}
          aria-expanded={!collapsed}
          className="flex min-w-0 flex-1 items-center gap-2 rounded-lg px-2 py-1.5 pr-16 text-left text-sm outline-none transition-colors hover:bg-muted focus-visible:ring-3 focus-visible:ring-ring/50 md:pr-2"
        >
          <ChevronRightIcon
            aria-hidden
            className={cn(
              "size-3.5 shrink-0 text-muted-foreground transition-transform",
              !collapsed && "rotate-90"
            )}
          />
          <FolderIcon aria-hidden className="size-4 shrink-0 text-muted-foreground" />
          <span className="flex-1 truncate font-medium">{group.name}</span>
          <span
            className="text-xs text-muted-foreground md:group-hover/item:opacity-0"
            aria-label={`리스트 ${listCount}개`}
          >
            {listCount}
          </span>
        </button>
        <div className="absolute top-1/2 right-1.5 flex -translate-y-1/2 focus-within:opacity-100 md:opacity-0 md:group-hover/item:opacity-100">
          <Button
            variant="ghost"
            size="icon-xs"
            onClick={onRename}
            aria-label={`${group.name} 그룹 이름 변경`}
          >
            <PencilIcon />
          </Button>
          <Button
            variant="ghost"
            size="icon-xs"
            onClick={onDelete}
            aria-label={`${group.name} 그룹 삭제`}
            title="그룹 삭제 (리스트는 유지)"
          >
            <Trash2Icon />
          </Button>
        </div>
      </div>
      {children}
    </li>
  )
}
