import { PencilIcon, Trash2Icon, UserPlusIcon, UsersIcon } from "lucide-react"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { ListIconMark } from "@/components/list-icon-mark"
import { SidebarItem } from "@/components/sidebar/sidebar-item"
import { isOwner, isShared, type ReminderList } from "@/lib/lists-api"
import { cn } from "@/lib/utils"

// 리스트 아이콘/이름/개수와 마우스를 올리면 나타나는 버튼. 편집/삭제/그룹 이동은 소유자에게만 전달되어 보인다.
export function ListItemContent({
  list,
  active,
  onSelect,
  onShare,
  onEdit,
  onDelete,
  moveToGroup,
}: {
  list: ReminderList
  active: boolean
  onSelect: () => void
  onShare: () => void
  onEdit?: () => void
  onDelete?: () => void
  moveToGroup?: React.ReactNode
}) {
  const shared = isShared(list)
  return (
    <>
      <SidebarItem
        active={active}
        onClick={onSelect}
        className={cn("min-w-0 flex-1 md:pr-2", onEdit ? "pr-26" : "pr-8")}
      >
        <span className="flex size-4 shrink-0 items-center justify-center">
          <ListIconMark list={list} />
        </span>
        <span className="flex-1 truncate">{list.name}</span>
        {shared && (
          <UsersIcon
            className="size-3.5 shrink-0 text-muted-foreground"
            aria-label={`공유된 리스트 (멤버 ${list.memberCount}명)`}
          >
            <title>{`공유된 리스트 (멤버 ${list.memberCount}명)`}</title>
          </UsersIcon>
        )}
        <Badge variant="secondary" className="md:group-hover/item:opacity-0">
          {list.reminderCount}
        </Badge>
      </SidebarItem>
      <div className="absolute top-1/2 right-1.5 flex -translate-y-1/2 focus-within:opacity-100 has-[[aria-expanded=true]]:opacity-100 md:opacity-0 md:group-hover/item:opacity-100">
        <Button
          variant="ghost"
          size="icon-xs"
          onClick={onShare}
          aria-label={`${list.name} 리스트 공유`}
          title={isOwner(list) ? "공유" : "멤버 보기"}
        >
          <UserPlusIcon />
        </Button>
        {moveToGroup}
        {onEdit && (
          <Button
            variant="ghost"
            size="icon-xs"
            onClick={onEdit}
            aria-label={`${list.name} 리스트 편집`}
          >
            <PencilIcon />
          </Button>
        )}
        {onDelete && (
          <Button
            variant="ghost"
            size="icon-xs"
            onClick={onDelete}
            aria-label={`${list.name} 리스트 삭제`}
          >
            <Trash2Icon />
          </Button>
        )}
      </div>
    </>
  )
}
