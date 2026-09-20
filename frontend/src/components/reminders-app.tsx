"use client"

import { useState } from "react"
import { set, startOfDay } from "date-fns"
import { ReminderForm } from "@/components/reminder-form"
import { ReminderList } from "@/components/reminder-list"
import { Sidebar } from "@/components/sidebar"
import { useLists } from "@/hooks/use-lists"
import {
  DEFAULT_SELECTION,
  selectionKey,
  smartViewLabel,
  type Selection,
} from "@/lib/selection"

export function RemindersApp() {
  const [selection, setSelection] = useState<Selection>(DEFAULT_SELECTION)
  const { data: lists } = useLists()

  const selectedList =
    selection.type === "list"
      ? lists?.find((list) => list.id === selection.listId)
      : undefined
  const title =
    selection.type === "list"
      ? (selectedList?.name ?? "")
      : smartViewLabel(selection.view)

  // "오늘" 뷰에서 추가한 리마인더가 바로 그 뷰에 나타나도록 마감일 기본값을 오늘로 둔다.
  const defaultDueAt =
    selection.type === "smart" && selection.view === "today"
      ? set(startOfDay(new Date()), { hours: 9 })
      : undefined

  return (
    <div className="flex min-h-screen flex-1">
      <Sidebar selection={selection} onSelect={setSelection} />
      <main className="flex flex-1 justify-center px-6 py-10">
        <div className="flex w-full max-w-xl flex-col gap-4">
          <h1
            className="text-2xl font-semibold"
            style={{ color: selectedList?.color ?? undefined }}
          >
            {title}
          </h1>
          <ReminderForm
            key={selectionKey(selection)}
            listId={selection.type === "list" ? selection.listId : undefined}
            defaultDueAt={defaultDueAt}
          />
          <ReminderList selection={selection} />
        </div>
      </main>
    </div>
  )
}
