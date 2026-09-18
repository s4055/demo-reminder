"use client"

import { useState, type FormEvent } from "react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { useCreateReminder } from "@/hooks/use-reminders"

export function ReminderForm() {
  const [title, setTitle] = useState("")
  const createReminder = useCreateReminder()

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    const trimmedTitle = title.trim()
    if (!trimmedTitle) return

    createReminder.mutate(
      { title: trimmedTitle },
      { onSuccess: () => setTitle("") }
    )
  }

  return (
    <form onSubmit={handleSubmit} className="flex w-full gap-2">
      <Input
        value={title}
        onChange={(event) => setTitle(event.target.value)}
        placeholder="새 리마인더"
        aria-label="리마인더 제목"
      />
      <Button type="submit" disabled={createReminder.isPending || !title.trim()}>
        추가
      </Button>
    </form>
  )
}
