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

export function RemindersApp() {
  const [selection, setSelection] = useState<Selection>(DEFAULT_SELECTION)
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
  if (selection.type === "list" && lists && !selectedList) {
    setSelection(DEFAULT_SELECTION)
  }
  const title =
    selection.type === "list"
      ? (selectedList?.name ?? "")
      : selection.type === "tag"
        ? `#${selection.name}`
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
        <Sidebar selection={selection} onSelect={setSelection} />
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
          <ReminderForm
            key={selectionKey(selection)}
            listId={selection.type === "list" ? selection.listId : undefined}
            tagNames={selection.type === "tag" ? [selection.name] : undefined}
            defaultDueAt={defaultDueAt}
          />
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
