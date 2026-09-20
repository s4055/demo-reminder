"use client"

import { useState, type FormEvent } from "react"
import { Button } from "@/components/ui/button"
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { useCreateList } from "@/hooks/use-lists"
import { cn } from "@/lib/utils"
import type { ReminderList } from "@/lib/lists-api"

export const LIST_COLORS = [
  "#FF3B30",
  "#FF9500",
  "#FFCC00",
  "#34C759",
  "#007AFF",
  "#AF52DE",
  "#8E8E93",
] as const

const DEFAULT_COLOR = "#007AFF"

export function ListCreateDialog({
  open,
  onOpenChange,
  onCreated,
}: {
  open: boolean
  onOpenChange: (open: boolean) => void
  onCreated?: (list: ReminderList) => void
}) {
  const [name, setName] = useState("")
  const [color, setColor] = useState<string>(DEFAULT_COLOR)
  const createList = useCreateList()

  function handleOpenChange(nextOpen: boolean) {
    if (!nextOpen) {
      setName("")
      setColor(DEFAULT_COLOR)
      createList.reset()
    }
    onOpenChange(nextOpen)
  }

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    const trimmedName = name.trim()
    if (!trimmedName) return

    createList.mutate(
      { name: trimmedName, color },
      {
        onSuccess: (list) => {
          handleOpenChange(false)
          onCreated?.(list)
        },
      }
    )
  }

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogContent>
        <form onSubmit={handleSubmit} className="grid gap-4">
          <DialogHeader>
            <DialogTitle>새 리스트</DialogTitle>
          </DialogHeader>
          <div className="grid gap-1.5">
            <Label htmlFor="list-name">이름</Label>
            <Input
              id="list-name"
              value={name}
              onChange={(event) => setName(event.target.value)}
              placeholder="리스트 이름"
              autoFocus
            />
          </div>
          <div className="grid gap-1.5">
            <Label>색상</Label>
            <div className="flex gap-2">
              {LIST_COLORS.map((swatch) => (
                <button
                  key={swatch}
                  type="button"
                  onClick={() => setColor(swatch)}
                  aria-label={`색상 ${swatch}`}
                  aria-pressed={color === swatch}
                  className={cn(
                    "size-6 rounded-full ring-offset-2 ring-offset-popover outline-none focus-visible:ring-2 focus-visible:ring-ring",
                    color === swatch && "ring-2 ring-foreground"
                  )}
                  style={{ backgroundColor: swatch }}
                />
              ))}
            </div>
          </div>
          {createList.isError && (
            <p className="text-sm text-destructive">
              리스트를 만들지 못했습니다.
            </p>
          )}
          <DialogFooter>
            <Button
              type="submit"
              disabled={createList.isPending || !name.trim()}
            >
              추가
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
