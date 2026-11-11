"use client"

import { useSortable } from "@dnd-kit/sortable"
import { CSS } from "@dnd-kit/utilities"
import { GripVerticalIcon } from "lucide-react"
import type { Reminder } from "@/lib/reminders-api"
import { ReminderItem } from "@/components/reminder-list/reminder-item"
import type { ReminderActions, SubtaskView } from "@/components/reminder-list/types"

export function SortableReminderItem({
  reminder,
  actions,
  subtaskView,
}: {
  reminder: Reminder
  actions: ReminderActions
  subtaskView?: SubtaskView
}) {
  const {
    attributes,
    listeners,
    setNodeRef,
    setActivatorNodeRef,
    transform,
    transition,
    isDragging,
  } = useSortable({ id: reminder.id })

  return (
    <ReminderItem
      reminder={reminder}
      actions={actions}
      subtaskView={subtaskView}
      itemRef={setNodeRef}
      style={{
        transform: CSS.Translate.toString(transform),
        transition,
      }}
      dragging={isDragging}
      dragHandle={
        <button
          type="button"
          ref={setActivatorNodeRef}
          {...attributes}
          {...listeners}
          aria-label={`${reminder.title} 순서 이동`}
          className="-ml-1.5 flex cursor-grab touch-none items-center rounded-sm text-muted-foreground outline-none focus-visible:ring-3 focus-visible:ring-ring/50 active:cursor-grabbing"
        >
          <GripVerticalIcon className="size-4" />
        </button>
      }
    />
  )
}
