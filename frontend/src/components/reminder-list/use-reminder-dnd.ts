import { useId } from "react"
import {
  KeyboardSensor,
  PointerSensor,
  useSensor,
  useSensors,
  type DragEndEvent,
} from "@dnd-kit/core"
import { arrayMove, sortableKeyboardCoordinates } from "@dnd-kit/sortable"
import { useReorderReminders } from "@/hooks/use-reminders"
import type { Reminder } from "@/lib/reminders-api"

export function useReminderDnd(listId: number | null, incomplete: Reminder[]) {
  const reorderReminders = useReorderReminders()
  const dndId = useId()
  const sensors = useSensors(
    // 살짝 끌어야 드래그가 시작되어 핸들 클릭과 구분된다.
    useSensor(PointerSensor, { activationConstraint: { distance: 4 } }),
    useSensor(KeyboardSensor, {
      coordinateGetter: sortableKeyboardCoordinates,
    })
  )

  function handleDragEnd({ active, over }: DragEndEvent) {
    if (listId === null || !over || active.id === over.id) return
    const ids = incomplete.map((reminder) => reminder.id)
    const from = ids.indexOf(Number(active.id))
    const to = ids.indexOf(Number(over.id))
    if (from < 0 || to < 0) return
    reorderReminders.mutate({ listId, ids: arrayMove(ids, from, to) })
  }

  return { dndId, sensors, handleDragEnd }
}
