"use client"

import { useState } from "react"
import { CheckIcon, FolderInputIcon, FolderPlusIcon } from "lucide-react"
import { Button } from "@/components/ui/button"
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover"
import type { ListGroup } from "@/lib/list-groups-api"
import type { ReminderList } from "@/lib/lists-api"

export function MoveToGroupMenu({
  list,
  groups,
  currentGroupId,
  onMove,
  onCreateGroup,
}: {
  list: ReminderList
  groups: ListGroup[]
  currentGroupId: number | null
  onMove: (groupId: number | null) => void
  onCreateGroup: () => void
}) {
  const [open, setOpen] = useState(false)
  const options: { id: number | null; label: string }[] = [
    { id: null, label: "그룹 없음" },
    ...groups.map((group) => ({ id: group.id, label: group.name })),
  ]

  return (
    <Popover open={open} onOpenChange={setOpen}>
      <PopoverTrigger
        render={
          <Button
            variant="ghost"
            size="icon-xs"
            aria-label={`${list.name} 리스트 그룹으로 이동`}
            title="그룹으로 이동"
          />
        }
      >
        <FolderInputIcon />
      </PopoverTrigger>
      <PopoverContent align="start" className="w-48 gap-0.5 p-1">
        <p className="px-2 py-1 text-xs font-semibold text-muted-foreground">
          그룹으로 이동
        </p>
        <ul aria-label="이동할 그룹" className="flex flex-col gap-0.5">
          {options.map((option) => {
            const selected = option.id === currentGroupId
            return (
              <li key={option.id ?? "none"}>
                <button
                  type="button"
                  aria-pressed={selected}
                  onClick={() => {
                    setOpen(false)
                    if (!selected) onMove(option.id)
                  }}
                  className="flex w-full items-center gap-2 rounded-md px-2 py-1.5 text-left text-sm outline-none hover:bg-muted focus-visible:ring-2 focus-visible:ring-ring"
                >
                  <span className="flex-1 truncate">{option.label}</span>
                  {selected && <CheckIcon aria-hidden className="size-3.5" />}
                </button>
              </li>
            )
          })}
        </ul>
        <button
          type="button"
          onClick={() => {
            setOpen(false)
            onCreateGroup()
          }}
          className="mt-0.5 flex w-full items-center gap-2 rounded-md border-t border-border px-2 py-1.5 text-left text-sm text-muted-foreground outline-none hover:bg-muted focus-visible:ring-2 focus-visible:ring-ring"
        >
          <FolderPlusIcon aria-hidden className="size-3.5" />새 그룹...
        </button>
      </PopoverContent>
    </Popover>
  )
}
