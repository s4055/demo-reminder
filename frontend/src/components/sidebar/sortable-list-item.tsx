import { useSortable } from "@dnd-kit/sortable"
import { CSS } from "@dnd-kit/utilities"
import { GripVerticalIcon } from "lucide-react"
import { ListItemContent } from "@/components/sidebar/list-item-content"
import { MoveToGroupMenu } from "@/components/sidebar/move-to-group-menu"
import { listDragId } from "@/components/sidebar/use-sidebar-dnd"
import type { ListGroup } from "@/lib/list-groups-api"
import type { ReminderList } from "@/lib/lists-api"
import { cn } from "@/lib/utils"

// 소유한 리스트 항목. 드래그 핸들과 소유자 전용 메뉴(편집/삭제/그룹으로 이동)를 붙인다.
export function SortableListItem({
  list,
  active,
  onSelect,
  onShare,
  onEdit,
  onDelete,
  groups,
  currentGroupId,
  onMoveToGroup,
  onCreateGroup,
}: {
  list: ReminderList
  active: boolean
  onSelect: () => void
  onShare: () => void
  onEdit: () => void
  onDelete: () => void
  groups: ListGroup[]
  currentGroupId: number | null
  onMoveToGroup: (groupId: number | null) => void
  onCreateGroup: () => void
}) {
  const {
    attributes,
    listeners,
    setNodeRef,
    setActivatorNodeRef,
    transform,
    transition,
    isDragging,
  } = useSortable({ id: listDragId(list.id) })

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
      <ListItemContent
        list={list}
        active={active}
        onSelect={onSelect}
        onShare={onShare}
        onEdit={onEdit}
        onDelete={onDelete}
        moveToGroup={
          <MoveToGroupMenu
            list={list}
            groups={groups}
            currentGroupId={currentGroupId}
            onMove={onMoveToGroup}
            onCreateGroup={onCreateGroup}
          />
        }
      />
    </li>
  )
}
