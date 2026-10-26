"use client"

import { useState } from "react"
import { set, startOfDay } from "date-fns"
import { MenuIcon } from "lucide-react"
import { ReminderEditDialog } from "@/components/reminder-edit-dialog"
import { ReminderForm } from "@/components/reminder-form"
import { ReminderList } from "@/components/reminder-list"
import { Sidebar } from "@/components/sidebar"
import { Button } from "@/components/ui/button"
import {
  Sheet,
  SheetContent,
  SheetDescription,
  SheetHeader,
  SheetTitle,
} from "@/components/ui/sheet"
import { useDebouncedValue } from "@/hooks/use-debounced-value"
import { useLists } from "@/hooks/use-lists"
import { useNotificationPermission } from "@/hooks/use-notification-permission"
import { useReminderNotifications } from "@/hooks/use-reminder-notifications"
import type { Reminder } from "@/lib/reminders-api"
import {
  DEFAULT_SELECTION,
  selectionKey,
  smartViewLabel,
  type Selection,
} from "@/lib/selection"

const SEARCH_DEBOUNCE_MS = 300

export function RemindersApp() {
  const [baseSelection, setBaseSelection] =
    useState<Selection>(DEFAULT_SELECTION)
  const [searchText, setSearchText] = useState("")
  const searchQuery = useDebouncedValue(searchText.trim(), SEARCH_DEBOUNCE_MS)
  const selection: Selection =
    searchText.trim() && searchQuery
      ? { type: "search", query: searchQuery }
      : baseSelection
  const setSelection = (next: Selection) => {
    setSearchText("")
    setBaseSelection(next)
  }
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const { data: lists } = useLists()
  const { permission } = useNotificationPermission()
  // 닫힘 애니메이션 동안 폼 내용이 유지되도록 open 과 대상 리마인더를 함께 보관한다.
  const [notifiedEdit, setNotifiedEdit] = useState<{
    open: boolean
    reminder?: Reminder
  }>({ open: false })
  useReminderNotifications({
    enabled: permission === "granted",
    onOpen: (reminder) => setNotifiedEdit({ open: true, reminder }),
  })

  const selectedList =
    selection.type === "list"
      ? lists?.find((list) => list.id === selection.listId)
      : undefined
  if (
    baseSelection.type === "list" &&
    lists &&
    !lists.some((list) => list.id === baseSelection.listId)
  ) {
    setBaseSelection(DEFAULT_SELECTION)
  }
  const title =
    selection.type === "list"
      ? (selectedList?.name ?? "")
      : selection.type === "tag"
        ? `#${selection.name}`
        : selection.type === "search"
          ? `“${selection.query}” 검색 결과`
          : smartViewLabel(selection.view)

  const defaultDueAt =
    selection.type === "smart" && selection.view === "today"
      ? set(startOfDay(new Date()), { hours: 9 })
      : undefined

  return (
    <div className="flex min-h-screen flex-1 flex-col md:flex-row">
      <aside className="hidden w-64 shrink-0 border-r border-border bg-muted/30 md:sticky md:top-0 md:block md:h-screen">
        <Sidebar
          selection={selection}
          onSelect={setSelection}
          searchText={searchText}
          onSearchChange={setSearchText}
        />
      </aside>

      <header className="flex items-center gap-2 border-b border-border px-3 py-2 md:hidden">
        <Button
          variant="ghost"
          size="icon"
          onClick={() => setMobileMenuOpen(true)}
          aria-label="메뉴 열기"
        >
          <MenuIcon />
        </Button>
        <span className="text-sm font-medium">리마인더</span>
      </header>
      <Sheet open={mobileMenuOpen} onOpenChange={setMobileMenuOpen}>
        <SheetContent side="left" className="w-64 gap-0 p-0" showCloseButton={false}>
          <SheetHeader className="sr-only">
            <SheetTitle>메뉴</SheetTitle>
            <SheetDescription>리스트와 스마트 리스트 목록</SheetDescription>
          </SheetHeader>
          <Sidebar
            selection={selection}
            onSelect={(next) => {
              setSelection(next)
              setMobileMenuOpen(false)
            }}
            searchText={searchText}
            onSearchChange={setSearchText}
          />
        </SheetContent>
      </Sheet>

      <main className="flex flex-1 justify-center px-4 py-6 md:px-6 md:py-10">
        <div className="flex w-full max-w-xl flex-col gap-4">
          <h1
            className="text-2xl font-semibold"
            style={{ color: selectedList?.color ?? undefined }}
          >
            {title}
          </h1>
          {selection.type !== "search" && (
            <ReminderForm
              key={selectionKey(selection)}
              listId={selection.type === "list" ? selection.listId : undefined}
              tagNames={selection.type === "tag" ? [selection.name] : undefined}
              defaultDueAt={defaultDueAt}
            />
          )}
          <ReminderList selection={selection} />
        </div>
      </main>
      {notifiedEdit.reminder && (
        <ReminderEditDialog
          open={notifiedEdit.open}
          onOpenChange={(open) => setNotifiedEdit((prev) => ({ ...prev, open }))}
          reminder={notifiedEdit.reminder}
        />
      )}
    </div>
  )
}
