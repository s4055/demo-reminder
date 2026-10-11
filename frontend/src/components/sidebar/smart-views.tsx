import {
  CalendarCheckIcon,
  CalendarClockIcon,
  CheckCircle2Icon,
  FlagIcon,
  InboxIcon,
  type LucideIcon,
} from "lucide-react"
import { SidebarItem } from "@/components/sidebar/sidebar-item"
import {
  SMART_VIEWS,
  isSameSelection,
  type Selection,
  type SmartView,
} from "@/lib/selection"

const SMART_VIEW_ICONS: Record<SmartView, LucideIcon> = {
  today: CalendarCheckIcon,
  scheduled: CalendarClockIcon,
  all: InboxIcon,
  flagged: FlagIcon,
  completed: CheckCircle2Icon,
}

export function SmartViews({
  selection,
  onSelect,
}: {
  selection: Selection
  onSelect: (selection: Selection) => void
}) {
  return (
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
  )
}
