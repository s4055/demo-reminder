"use client"

import { useState } from "react"
import { ReminderForm } from "@/components/reminder-form"
import { ReminderList } from "@/components/reminder-list"
import { Sidebar } from "@/components/sidebar"
import { useLists } from "@/hooks/use-lists"
import { ALL_SELECTION, type Selection } from "@/lib/selection"

export function RemindersApp() {
  const [selection, setSelection] = useState<Selection>(ALL_SELECTION)
  const { data: lists } = useLists()

  const selectedList =
    selection.type === "list"
      ? lists?.find((list) => list.id === selection.listId)
      : undefined
  const title = selection.type === "list" ? (selectedList?.name ?? "") : "전체"

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
            listId={selection.type === "list" ? selection.listId : undefined}
          />
          <ReminderList selection={selection} />
        </div>
      </main>
    </div>
  )
}
