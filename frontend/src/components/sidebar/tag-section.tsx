import { HashIcon, XIcon } from "lucide-react"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { SidebarItem } from "@/components/sidebar/sidebar-item"
import { useDeleteTag, useTags } from "@/hooks/use-tags"
import {
  DEFAULT_SELECTION,
  isSameSelection,
  type Selection,
} from "@/lib/selection"

export function TagSection({
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
