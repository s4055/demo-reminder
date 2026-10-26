import { LIST_ICONS, isListIconKey } from "@/lib/list-icons"
import type { ReminderList } from "@/lib/lists-api"
import { cn } from "@/lib/utils"

const DEFAULT_LIST_COLOR = "#8E8E93"

export function ListIconMark({
  list,
  className,
}: {
  list: Pick<ReminderList, "icon" | "color">
  className?: string
}) {
  const color = list.color ?? DEFAULT_LIST_COLOR
  if (!isListIconKey(list.icon)) {
    return (
      <span
        aria-hidden
        className={cn("size-3 shrink-0 rounded-full", className)}
        style={{ backgroundColor: color }}
      />
    )
  }
  const Icon = LIST_ICONS[list.icon]
  return (
    <Icon
      aria-hidden
      data-icon={list.icon}
      className={cn("size-4 shrink-0", className)}
      style={{ color }}
    />
  )
}
