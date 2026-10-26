import {
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
import { arrayMove, sortableKeyboardCoordinates } from "@dnd-kit/sortable"
import { useReorderListGroups } from "@/hooks/use-list-groups"
import { useReorderLists } from "@/hooks/use-lists"
import type { ListGroup } from "@/lib/list-groups-api"
import type { ReminderList } from "@/lib/lists-api"

export const groupDragId = (id: number) => `group-${id}`
export const listDragId = (id: number) => `list-${id}`

function parseDragId(
  id: UniqueIdentifier
): { kind: "group" | "list"; id: number } | null {
  const match = /^(group|list)-(\d+)$/.exec(String(id))
  return match
    ? { kind: match[1] as "group" | "list", id: Number(match[2]) }
    : null
}

export function useSidebarDnd({
  groups,
  ownedLists,
  groupOf,
  listsIn,
}: {
  groups: ListGroup[] | undefined
  ownedLists: ReminderList[] | undefined
  groupOf: (list: ReminderList) => number | null
  listsIn: (groupId: number | null) => ReminderList[]
}) {
  const reorderLists = useReorderLists()
  const reorderGroups = useReorderListGroups()

  function dragScope(id: UniqueIdentifier): string | null {
    const parsed = parseDragId(id)
    if (!parsed) return null
    if (parsed.kind === "group") return "groups"
    const list = ownedLists?.find((l) => l.id === parsed.id)
    return list ? `lists:${groupOf(list) ?? "none"}` : null
  }

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

  const collisionDetection: CollisionDetection = (args) => {
    const scope = dragScope(args.active.id)
    return closestCenter({
      ...args,
      droppableContainers: args.droppableContainers.filter(
        (container) => dragScope(container.id) === scope
      ),
    })
  }

  function onDragEnd({ active, over }: DragEndEvent) {
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

  return { sensors, collisionDetection, onDragEnd }
}
