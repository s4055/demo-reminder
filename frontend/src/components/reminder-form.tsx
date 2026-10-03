"use client"

import { useState, type FormEvent } from "react"
import { Button } from "@/components/ui/button"
import { DueDatePicker } from "@/components/due-date-picker"
import { Input } from "@/components/ui/input"
import { useCreateReminder } from "@/hooks/use-reminders"
import { toDueAtParam } from "@/lib/due-date"

export function ReminderForm({
  listId,
  defaultDueAt,
  tagNames,
}: {
  listId?: number
  defaultDueAt?: Date
  // 태그 화면에서 추가한 리마인더가 바로 그 화면에 나타나도록 붙일 태그
  tagNames?: string[]
}) {
  const [title, setTitle] = useState("")
  const [dueAt, setDueAt] = useState<Date | undefined>(defaultDueAt)
  const createReminder = useCreateReminder()

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    const trimmedTitle = title.trim()
    if (!trimmedTitle) return

    createReminder.mutate(
      {
        title: trimmedTitle,
        listId,
        dueAt: dueAt ? toDueAtParam(dueAt) : null,
        tagNames,
      },
      {
        onSuccess: () => {
          setTitle("")
          setDueAt(defaultDueAt)
        },
      }
    )
  }

  return (
    <form onSubmit={handleSubmit} className="flex w-full flex-col gap-2">
      <div className="flex gap-2">
        <Input
          value={title}
          onChange={(event) => setTitle(event.target.value)}
          placeholder="새 리마인더"
          aria-label="리마인더 제목"
        />
        <Button
          type="submit"
          disabled={createReminder.isPending || !title.trim()}
        >
          추가
        </Button>
      </div>
      <DueDatePicker value={dueAt} onChange={setDueAt} />
    </form>
  )
}
