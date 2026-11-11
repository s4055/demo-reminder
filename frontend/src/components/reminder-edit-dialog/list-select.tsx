"use client"

import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import type { ReminderList } from "@/lib/lists-api"

export function ListSelect({
  id,
  lists,
  value,
  onChange,
}: {
  id: string
  lists: ReminderList[]
  value: number | null
  onChange: (listId: number | null) => void
}) {
  const listById = new Map(lists.map((list) => [list.id, list]))
  return (
    <Select<number | null>
      value={value}
      onValueChange={(next) => onChange(next ?? value)}
    >
      <SelectTrigger id={id} className="w-full">
        <SelectValue placeholder="리스트 없음">
          {(selected: number | null) => {
            const list = selected === null ? undefined : listById.get(selected)
            return list ? <ListLabel list={list} /> : "리스트 없음"
          }}
        </SelectValue>
      </SelectTrigger>
      <SelectContent>
        {lists.map((list) => (
          <SelectItem key={list.id} value={list.id}>
            <ListLabel list={list} />
          </SelectItem>
        ))}
      </SelectContent>
    </Select>
  )
}

function ListLabel({ list }: { list: ReminderList }) {
  return (
    <span className="flex min-w-0 items-center gap-2">
      <span
        aria-hidden
        className="size-2.5 shrink-0 rounded-full"
        style={{ backgroundColor: list.color ?? "#8E8E93" }}
      />
      <span className="truncate">{list.name}</span>
    </span>
  )
}
