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

// 입력을 멈추고 이 시간이 지나면 검색한다.
const SEARCH_DEBOUNCE_MS = 300

export function RemindersApp() {
  // 사이드바에서 고른 화면. 검색 중에도 유지되어 검색어를 지우면 이 화면으로 돌아간다.
  const [baseSelection, setBaseSelection] =
    useState<Selection>(DEFAULT_SELECTION)
  const [searchText, setSearchText] = useState("")
  const searchQuery = useDebouncedValue(searchText.trim(), SEARCH_DEBOUNCE_MS)
  // 검색창을 비우면 디바운스를 기다리지 않고 바로 이전 화면으로 돌아간다.
  const selection: Selection =
    searchText.trim() && searchQuery
      ? { type: "search", query: searchQuery }
      : baseSelection
  // 사이드바에서 다른 화면을 고르면 검색을 끝낸다.
  const setSelection = (next: Selection) => {
    setSearchText("")
    setBaseSelection(next)
  }
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const { data: lists } = useLists()
  const { permission } = useNotificationPermission()
  // 브라우저 알림을 클릭하면 어느 화면에 있든 해당 리마인더의 상세 편집을 연다.
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
  // 보고 있던 리스트가 사라지면(소유자가 삭제하거나 공유에서 제외) 기본 화면으로 돌아간다.
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

  // "오늘" 뷰에서 추가한 리마인더가 바로 그 뷰에 나타나도록 마감일 기본값을 오늘로 둔다.
  const defaultDueAt =
    selection.type === "smart" && selection.view === "today"
      ? set(startOfDay(new Date()), { hours: 9 })
      : undefined

  return (
    <div className="flex min-h-screen flex-1 flex-col md:flex-row">
      {/* 데스크톱: 고정 사이드바 */}
      <aside className="hidden w-64 shrink-0 border-r border-border bg-muted/30 md:sticky md:top-0 md:block md:h-screen">
        <Sidebar
          selection={selection}
          onSelect={setSelection}
          searchText={searchText}
          onSearchChange={setSearchText}
        />
      </aside>

      {/* 모바일: 상단 바 + 슬라이드 사이드바 */}
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
          {/* 검색 결과 화면에서는 새 리마인더를 추가하지 않는다. */}
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
