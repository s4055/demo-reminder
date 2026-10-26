"use client"

import { useId } from "react"
import { DndContext } from "@dnd-kit/core"
import {
  SortableContext,
  verticalListSortingStrategy,
} from "@dnd-kit/sortable"
import { Button } from "@/components/ui/button"
import { Skeleton } from "@/components/ui/skeleton"
import { SortableGroup } from "@/components/sidebar/sortable-group"
import { SortableListItem } from "@/components/sidebar/sortable-list-item"
import {
  groupDragId,
  listDragId,
  useSidebarDnd,
} from "@/components/sidebar/use-sidebar-dnd"
import { useCollapsedGroups } from "@/hooks/use-collapsed-groups"
import { useListGroups } from "@/hooks/use-list-groups"
import { useChangeListGroup, useLists } from "@/hooks/use-lists"
import type { ListGroup } from "@/lib/list-groups-api"
import { isOwner, type ReminderList } from "@/lib/lists-api"
import { isSameSelection, type Selection } from "@/lib/selection"

export function ListSection({
  selection,
  onSelect,
  onShareList,
  onEditList,
  onDeleteList,
  onCreateGroup,
  onRenameGroup,
  onDeleteGroup,
}: {
  selection: Selection
  onSelect: (selection: Selection) => void
  onShareList: (list: ReminderList) => void
  onEditList: (list: ReminderList) => void
  onDeleteList: (list: ReminderList) => void
  onCreateGroup: () => void
  onRenameGroup: (group: ListGroup) => void
  onDeleteGroup: (group: ListGroup) => void
}) {
  const { data: lists, isLoading, isError, refetch } = useLists()
  const { data: groups } = useListGroups()
  const changeListGroup = useChangeListGroup()
  const collapsedGroups = useCollapsedGroups()
  const dndId = useId()
  const ownedLists = lists?.filter(isOwner)

  const groupIds = new Set(groups?.map((group) => group.id))
  const groupOf = (list: ReminderList) =>
    list.groupId !== null && groupIds.has(list.groupId) ? list.groupId : null
  const listsIn = (groupId: number | null) =>
    ownedLists?.filter((list) => groupOf(list) === groupId) ?? []
  const ungroupedLists = listsIn(null)

  const { sensors, collisionDetection, onDragEnd } = useSidebarDnd({
    groups,
    ownedLists,
    groupOf,
    listsIn,
  })

  function renderListItem(list: ReminderList) {
    return (
      <SortableListItem
        key={list.id}
        list={list}
        active={isSameSelection(selection, { type: "list", listId: list.id })}
        onSelect={() => onSelect({ type: "list", listId: list.id })}
        onShare={() => onShareList(list)}
        onEdit={() => onEditList(list)}
        onDelete={() => onDeleteList(list)}
        groups={groups ?? []}
        currentGroupId={groupOf(list)}
        onMoveToGroup={(groupId) =>
          changeListGroup.mutate({ id: list.id, groupId })
        }
        onCreateGroup={onCreateGroup}
      />
    )
  }

  return (
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
        onDragEnd={onDragEnd}
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
                  onRename={() => onRenameGroup(group)}
                  onDelete={() => onDeleteGroup(group)}
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
  )
}
