"use client"

import { useState, type KeyboardEvent } from "react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { useCreateReminder } from "@/hooks/use-reminders"
import type { Reminder } from "@/lib/reminders-api"
import { cn } from "@/lib/utils"

// 이 영역은 편집 폼 안에 있으므로 Enter가 폼을 제출하지 않도록 막는다.
export function SubtaskSection({ parent }: { parent: Reminder }) {
  const createReminder = useCreateReminder()
  const [title, setTitle] = useState("")

  function addSubtask() {
    const trimmed = title.trim()
    if (!trimmed || createReminder.isPending) return
    createReminder.mutate(
      { title: trimmed, parentId: parent.id },
      { onSuccess: () => setTitle("") }
    )
  }

  function handleKeyDown(event: KeyboardEvent<HTMLInputElement>) {
    // 한글 조합 중 Enter는 조합 확정이므로 추가하지 않는다.
    if (event.nativeEvent.isComposing || event.key !== "Enter") return
    event.preventDefault()
    addSubtask()
  }

  return (
    <div className="grid gap-1.5">
      <Label htmlFor="reminder-subtask">하위 작업</Label>
      {parent.subtasks.length > 0 && (
        <ul className="grid gap-1 text-sm">
          {parent.subtasks.map((subtask) => (
            <li
              key={subtask.id}
              className={cn(
                "truncate rounded-md bg-secondary px-2 py-1",
                subtask.completed && "text-muted-foreground line-through"
              )}
            >
              {subtask.title}
            </li>
          ))}
        </ul>
      )}
      <div className="flex gap-2">
        <Input
          id="reminder-subtask"
          value={title}
          onChange={(event) => setTitle(event.target.value)}
          onKeyDown={handleKeyDown}
          placeholder="하위 작업 입력 후 Enter"
        />
        <Button
          type="button"
          variant="outline"
          onClick={addSubtask}
          disabled={!title.trim() || createReminder.isPending}
        >
          추가
        </Button>
      </div>
      {createReminder.isError && (
        <p className="text-xs text-destructive">
          하위 작업을 추가하지 못했습니다.
        </p>
      )}
    </div>
  )
}
