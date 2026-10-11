import { ListItemContent } from "@/components/sidebar/list-item-content"
import { useLists } from "@/hooks/use-lists"
import { isOwner, type ReminderList } from "@/lib/lists-api"
import { isSameSelection, type Selection } from "@/lib/selection"

// 공유받은 리스트. 순서 변경/그룹/편집/삭제 없이 멤버 보기만 할 수 있으며, 없으면 섹션을 숨긴다.
export function SharedListSection({
  selection,
  onSelect,
  onShareList,
}: {
  selection: Selection
  onSelect: (selection: Selection) => void
  onShareList: (list: ReminderList) => void
}) {
  const { data: lists } = useLists()
  const sharedLists = lists?.filter((list) => !isOwner(list))

  if (!sharedLists || sharedLists.length === 0) return null

  return (
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
              onShare={() => onShareList(list)}
            />
          </li>
        ))}
      </ul>
    </section>
  )
}
