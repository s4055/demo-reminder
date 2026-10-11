"use client"

import { useId, useState } from "react"
import {
  DndContext,
  KeyboardSensor,
  PointerSensor,
  closestCenter,
  useSensor,
  useSensors,
  type CollisionDetection,
  type DragEndEvent,
  type KeyboardCoordinateGetter,
  type UniqueIdentifier,
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
  CheckIcon,
  ChevronRightIcon,
  FlagIcon,
  FolderIcon,
  FolderInputIcon,
  FolderPlusIcon,
  GripVerticalIcon,
  HashIcon,
  InboxIcon,
  PencilIcon,
  PlusIcon,
  SearchIcon,
  Trash2Icon,
  UserPlusIcon,
  UsersIcon,
  XIcon,
  type LucideIcon,
} from "lucide-react"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover"
import { Skeleton } from "@/components/ui/skeleton"
import { ListFormDialog } from "@/components/list-form-dialog"
import { ListDeleteDialog } from "@/components/list-delete-dialog"
import {
  ListGroupDeleteDialog,
  ListGroupFormDialog,
} from "@/components/list-group-dialogs"
import { ListIconMark } from "@/components/list-icon-mark"
import { ListShareDialog } from "@/components/list-share-dialog"
import { NotificationToggle } from "@/components/notification-toggle"
import { UserMenu } from "@/components/user-menu"
import { useCollapsedGroups } from "@/hooks/use-collapsed-groups"
import {
  useListGroups,
  useReorderListGroups,
} from "@/hooks/use-list-groups"
import {
  useChangeListGroup,
  useLists,
  useReorderLists,
} from "@/hooks/use-lists"
import { useDeleteTag, useTags } from "@/hooks/use-tags"
import type { ListGroup } from "@/lib/list-groups-api"
import { isOwner, isShared, type ReminderList } from "@/lib/lists-api"
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

// 그룹과 리스트를 한 DndContext에서 정렬하므로 드래그 id에 종류를 붙여 구분한다.
const groupDragId = (id: number) => `group-${id}`
const listDragId = (id: number) => `list-${id}`

function parseDragId(
  id: UniqueIdentifier
): { kind: "group" | "list"; id: number } | null {
  const match = /^(group|list)-(\d+)$/.exec(String(id))
  return match
    ? { kind: match[1] as "group" | "list", id: Number(match[2]) }
    : null
}

export function Sidebar({
  selection,
  onSelect,
  searchText,
  onSearchChange,
}: {
  selection: Selection
  onSelect: (selection: Selection) => void
  // 검색창 입력값. 검색 결과 조회(디바운스)와 이전 화면 복귀는 상위에서 처리한다.
  searchText: string
  onSearchChange: (text: string) => void
}) {
  const { data: lists, isLoading, isError, refetch } = useLists()
  const { data: groups } = useListGroups()
  const reorderLists = useReorderLists()
  const reorderGroups = useReorderListGroups()
  const changeListGroup = useChangeListGroup()
  const collapsedGroups = useCollapsedGroups()
  const dndId = useId()
  // 키보드로 옮길 때도 같은 범위의 항목 사이에서만 움직이도록 다른 범위의 드롭 대상을 빼고 계산한다.
  // (그대로 두면 그룹을 위로 옮길 때 위 그룹 안의 리스트 위치로 이동해 버린다.)
  const keyboardCoordinates: KeyboardCoordinateGetter = (event, args) => {
    const scope = dragScope(args.active)
    const containers = args.context.droppableContainers
    const enabled = containers
      .getEnabled()
      .filter((container) => dragScope(container.id) === scope)
    return sortableKeyboardCoordinates(event, {
      ...args,
      context: {
        ...args.context,
        // sortableKeyboardCoordinates는 getEnabled()로 후보를 고르고 get()으로 고른 대상을 찾는다.
        droppableContainers: {
          getEnabled: () => enabled,
          get: (id: UniqueIdentifier) => containers.get(id),
        } as unknown as typeof containers,
      },
    })
  }
  const sensors = useSensors(
    // 살짝 끌어야 드래그가 시작되어 핸들 클릭과 구분된다.
    useSensor(PointerSensor, { activationConstraint: { distance: 4 } }),
    useSensor(KeyboardSensor, { coordinateGetter: keyboardCoordinates })
  )
  // 닫힘 애니메이션 동안 폼 내용이 유지되도록 open 과 대상을 함께 보관한다.
  const [listForm, setListForm] = useState<{
    open: boolean
    list?: ReminderList
  }>({ open: false })
  const [listToDelete, setListToDelete] = useState<ReminderList | null>(null)
  const [listToShare, setListToShare] = useState<ReminderList | null>(null)
  const [groupForm, setGroupForm] = useState<{
    open: boolean
    group?: ListGroup
  }>({ open: false })
  const [groupToDelete, setGroupToDelete] = useState<ListGroup | null>(null)
  // 순서 변경과 그룹은 소유한 리스트만 쓸 수 있다. 공유받은 리스트는 따로 모아 아래에 보여준다.
  const ownedLists = lists?.filter(isOwner)
  const sharedLists = lists?.filter((list) => !isOwner(list))

  // 리스트가 속한 그룹. 그룹 목록에 없는 그룹(아직 불러오지 않았거나 방금 삭제됨)이면 그룹 밖으로 본다.
  const groupIds = new Set(groups?.map((group) => group.id))
  const groupOf = (list: ReminderList) =>
    list.groupId !== null && groupIds.has(list.groupId) ? list.groupId : null
  const listsIn = (groupId: number | null) =>
    ownedLists?.filter((list) => groupOf(list) === groupId) ?? []
  const ungroupedLists = listsIn(null)

  // 드래그는 같은 범위(그룹 순서, 또는 같은 그룹 안의 리스트) 안에서만 한다. 그룹 간 이동은 메뉴로 한다.
  function dragScope(id: UniqueIdentifier): string | null {
    const parsed = parseDragId(id)
    if (!parsed) return null
    if (parsed.kind === "group") return "groups"
    const list = ownedLists?.find((l) => l.id === parsed.id)
    return list ? `lists:${groupOf(list) ?? "none"}` : null
  }

  const collisionDetection: CollisionDetection = (args) => {
    const scope = dragScope(args.active.id)
    return closestCenter({
      ...args,
      droppableContainers: args.droppableContainers.filter(
        (container) => dragScope(container.id) === scope
      ),
    })
  }

  function handleDragEnd({ active, over }: DragEndEvent) {
    if (!over || active.id === over.id) return
    const from = parseDragId(active.id)
    const to = parseDragId(over.id)
    if (!from || !to || from.kind !== to.kind) return
    if (from.kind === "group") {
      if (!groups) return
      const ids = groups.map((group) => group.id)
      const fromIndex = ids.indexOf(from.id)
      const toIndex = ids.indexOf(to.id)
      if (fromIndex < 0 || toIndex < 0) return
      reorderGroups.mutate(arrayMove(ids, fromIndex, toIndex))
      return
    }
    if (!ownedLists) return
    const source = ownedLists.find((list) => list.id === from.id)
    const target = ownedLists.find((list) => list.id === to.id)
    if (!source || !target || groupOf(source) !== groupOf(target)) return
    // 리스트 순서는 그룹과 관계없이 소유한 리스트 전체 기준이므로,
    // 같은 그룹 안에서 바뀐 순서를 그 그룹 리스트들이 차지하던 자리에 다시 채워 전체 순서를 만든다.
    const scopeIds = listsIn(groupOf(source)).map((list) => list.id)
    const moved = arrayMove(
      scopeIds,
      scopeIds.indexOf(from.id),
      scopeIds.indexOf(to.id)
    )
    let next = 0
    reorderLists.mutate(
      ownedLists.map((list) =>
        groupOf(list) === groupOf(source) ? moved[next++] : list.id
      )
    )
  }

  function renderListItem(list: ReminderList) {
    return (
      <SortableListItem
        key={list.id}
        list={list}
        active={isSameSelection(selection, { type: "list", listId: list.id })}
        onSelect={() => onSelect({ type: "list", listId: list.id })}
        onShare={() => setListToShare(list)}
        onEdit={() => setListForm({ open: true, list })}
        onDelete={() => setListToDelete(list)}
        groups={groups ?? []}
        currentGroupId={groupOf(list)}
        onMoveToGroup={(groupId) =>
          changeListGroup.mutate({ id: list.id, groupId })
        }
        onCreateGroup={() => setGroupForm({ open: true })}
      />
    )
  }

  return (
    <div className="flex h-full flex-col gap-4 overflow-y-auto p-3">
      <div role="search" className="relative">
        <SearchIcon
          aria-hidden
          className="pointer-events-none absolute top-1/2 left-2.5 size-4 -translate-y-1/2 text-muted-foreground"
        />
        <Input
          type="search"
          value={searchText}
          onChange={(event) => onSearchChange(event.target.value)}
          onKeyDown={(event) => {
            if (event.key === "Escape") onSearchChange("")
          }}
          placeholder="검색"
          aria-label="리마인더 검색"
          maxLength={100}
          className="pr-8 pl-8 [&::-webkit-search-cancel-button]:hidden"
        />
        {searchText && (
          <button
            type="button"
            onClick={() => onSearchChange("")}
            aria-label="검색어 지우기"
            className="absolute top-1/2 right-2 flex -translate-y-1/2 items-center rounded-sm text-muted-foreground outline-none hover:text-foreground focus-visible:ring-3 focus-visible:ring-ring/50"
          >
            <XIcon className="size-4" />
          </button>
        )}
      </div>

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
        {ownedLists?.length === 0 && !groups?.length && (
          <p className="px-2 text-sm text-muted-foreground">
            리스트가 없습니다. 아래에서 새 리스트를 추가해 보세요.
          </p>
        )}
        <DndContext
          id={dndId}
          sensors={sensors}
          collisionDetection={collisionDetection}
          onDragEnd={handleDragEnd}
        >
          <SortableContext
            items={groups?.map((group) => groupDragId(group.id)) ?? []}
            strategy={verticalListSortingStrategy}
          >
            <ul className="flex flex-col gap-0.5">
              {groups?.map((group) => {
                const groupLists = listsIn(group.id)
                const collapsed = collapsedGroups.isCollapsed(group.id)
                return (
                  <SortableGroup
                    key={group.id}
                    group={group}
                    listCount={groupLists.length}
                    collapsed={collapsed}
                    onToggle={() => collapsedGroups.toggle(group.id)}
                    onRename={() => setGroupForm({ open: true, group })}
                    onDelete={() => setGroupToDelete(group)}
                  >
                    {!collapsed && (
                      <SortableContext
                        items={groupLists.map((list) => listDragId(list.id))}
                        strategy={verticalListSortingStrategy}
                      >
                        <ul
                          aria-label={`${group.name} 그룹의 리스트`}
                          className="ml-4 flex flex-col gap-0.5"
                        >
                          {groupLists.map(renderListItem)}
                          {groupLists.length === 0 && (
                            <li className="py-1 pl-6 text-xs text-muted-foreground">
                              리스트 메뉴의 &ldquo;그룹으로 이동&rdquo;으로
                              리스트를 넣어 보세요.
                            </li>
                          )}
                        </ul>
                      </SortableContext>
                    )}
                  </SortableGroup>
                )
              })}
            </ul>
          </SortableContext>
          <SortableContext
            items={ungroupedLists.map((list) => listDragId(list.id))}
            strategy={verticalListSortingStrategy}
          >
            <ul className="flex flex-col gap-0.5">
              {ungroupedLists.map(renderListItem)}
            </ul>
          </SortableContext>
        </DndContext>
      </section>

      {sharedLists && sharedLists.length > 0 && (
        <section className="flex flex-col gap-1">
          <h2 className="px-2 text-xs font-semibold text-muted-foreground">
            공유받은 리스트
          </h2>
          <ul className="flex flex-col gap-0.5">
            {sharedLists.map((list) => (
              <li key={list.id} className="group/item relative flex items-center rounded-lg">
                <span className="w-4 shrink-0" aria-hidden />
                <ListItemContent
                  list={list}
                  active={isSameSelection(selection, {
                    type: "list",
                    listId: list.id,
                  })}
                  onSelect={() => onSelect({ type: "list", listId: list.id })}
                  onShare={() => setListToShare(list)}
                />
              </li>
            ))}
          </ul>
        </section>
      )}

      <TagSection selection={selection} onSelect={onSelect} />

      <div className="mt-auto flex flex-col gap-1">
        <NotificationToggle />
        <div className="flex gap-1">
          <Button
            variant="ghost"
            className="flex-1 justify-start"
            onClick={() => setListForm({ open: true })}
          >
            <PlusIcon />
            리스트 추가
          </Button>
          <Button
            variant="ghost"
            onClick={() => setGroupForm({ open: true })}
            aria-label="그룹 추가"
            title="그룹 추가"
          >
            <FolderPlusIcon />
          </Button>
        </div>
        <UserMenu />
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
      <ListShareDialog
        list={listToShare}
        onOpenChange={(open) => {
          if (!open) setListToShare(null)
        }}
        onLeft={(list) => {
          if (selection.type === "list" && selection.listId === list.id) {
            onSelect(DEFAULT_SELECTION)
          }
        }}
      />
      <ListGroupFormDialog
        open={groupForm.open}
        onOpenChange={(open) => setGroupForm((prev) => ({ ...prev, open }))}
        group={groupForm.group}
      />
      <ListGroupDeleteDialog
        group={groupToDelete}
        onOpenChange={(open) => {
          if (!open) setGroupToDelete(null)
        }}
      />
    </div>
  )
}

// 그룹 머리글(펼치기/접기, 이름, 리스트 수)과 그 아래 리스트. 그룹 전체가 함께 드래그된다.
function SortableGroup({
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

function SortableListItem({
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

// 리스트 메뉴의 "그룹으로 이동". 그룹 간 이동은 드래그 대신 이 메뉴로 한다.
function MoveToGroupMenu({
  list,
  groups,
  currentGroupId,
  onMove,
  onCreateGroup,
}: {
  list: ReminderList
  groups: ListGroup[]
  currentGroupId: number | null
  onMove: (groupId: number | null) => void
  onCreateGroup: () => void
}) {
  const [open, setOpen] = useState(false)
  const options: { id: number | null; label: string }[] = [
    { id: null, label: "그룹 없음" },
    ...groups.map((group) => ({ id: group.id, label: group.name })),
  ]

  return (
    <Popover open={open} onOpenChange={setOpen}>
      <PopoverTrigger
        render={
          <Button
            variant="ghost"
            size="icon-xs"
            aria-label={`${list.name} 리스트 그룹으로 이동`}
            title="그룹으로 이동"
          />
        }
      >
        <FolderInputIcon />
      </PopoverTrigger>
      <PopoverContent align="start" className="w-48 gap-0.5 p-1">
        <p className="px-2 py-1 text-xs font-semibold text-muted-foreground">
          그룹으로 이동
        </p>
        <ul aria-label="이동할 그룹" className="flex flex-col gap-0.5">
          {options.map((option) => {
            const selected = option.id === currentGroupId
            return (
              <li key={option.id ?? "none"}>
                <button
                  type="button"
                  aria-pressed={selected}
                  onClick={() => {
                    setOpen(false)
                    if (!selected) onMove(option.id)
                  }}
                  className="flex w-full items-center gap-2 rounded-md px-2 py-1.5 text-left text-sm outline-none hover:bg-muted focus-visible:ring-2 focus-visible:ring-ring"
                >
                  <span className="flex-1 truncate">{option.label}</span>
                  {selected && <CheckIcon aria-hidden className="size-3.5" />}
                </button>
              </li>
            )
          })}
        </ul>
        <button
          type="button"
          onClick={() => {
            setOpen(false)
            onCreateGroup()
          }}
          className="mt-0.5 flex w-full items-center gap-2 rounded-md border-t border-border px-2 py-1.5 text-left text-sm text-muted-foreground outline-none hover:bg-muted focus-visible:ring-2 focus-visible:ring-ring"
        >
          <FolderPlusIcon aria-hidden className="size-3.5" />새 그룹...
        </button>
      </PopoverContent>
    </Popover>
  )
}

// 리스트 아이콘/이름/개수와 마우스를 올리면 나타나는 버튼. 편집/삭제/그룹 이동은 소유자에게만 전달되어 보인다.
function ListItemContent({
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
