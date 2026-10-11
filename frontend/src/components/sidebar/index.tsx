"use client"

import { useState } from "react"
import { FolderPlusIcon, PlusIcon } from "lucide-react"
import { Button } from "@/components/ui/button"
import { ListFormDialog } from "@/components/list-form-dialog"
import { ListDeleteDialog } from "@/components/list-delete-dialog"
import {
  ListGroupDeleteDialog,
  ListGroupFormDialog,
} from "@/components/list-group-dialogs"
import { ListShareDialog } from "@/components/list-share-dialog"
import { NotificationToggle } from "@/components/notification-toggle"
import { UserMenu } from "@/components/user-menu"
import { ListSection } from "@/components/sidebar/list-section"
import { SharedListSection } from "@/components/sidebar/shared-list-section"
import { SidebarSearch } from "@/components/sidebar/sidebar-search"
import { SmartViews } from "@/components/sidebar/smart-views"
import { TagSection } from "@/components/sidebar/tag-section"
import type { ListGroup } from "@/lib/list-groups-api"
import type { ReminderList } from "@/lib/lists-api"
import { DEFAULT_SELECTION, type Selection } from "@/lib/selection"

// 사이드바 조립. 여러 섹션에서 여는 리스트/그룹 다이얼로그의 상태는 여기서 관리한다.
export function Sidebar({
  selection,
  onSelect,
  searchText,
  onSearchChange,
}: {
  selection: Selection
  onSelect: (selection: Selection) => void
  // 검색창 입력값. 검색 결과 조회(디바운스)와 이전 화면 복귀는 상위에서 처리한다.
  searchText: string
  onSearchChange: (text: string) => void
}) {
  // 닫힘 애니메이션 동안 폼 내용이 유지되도록 open 과 대상을 함께 보관한다.
  const [listForm, setListForm] = useState<{
    open: boolean
    list?: ReminderList
  }>({ open: false })
  const [listToDelete, setListToDelete] = useState<ReminderList | null>(null)
  const [listToShare, setListToShare] = useState<ReminderList | null>(null)
  const [groupForm, setGroupForm] = useState<{
    open: boolean
    group?: ListGroup
  }>({ open: false })
  const [groupToDelete, setGroupToDelete] = useState<ListGroup | null>(null)

  // 보고 있던 리스트가 삭제되거나 나간 리스트면 기본 화면으로 돌아간다.
  function leaveIfSelected(list: ReminderList) {
    if (selection.type === "list" && selection.listId === list.id) {
      onSelect(DEFAULT_SELECTION)
    }
  }

  return (
    <div className="flex h-full flex-col gap-4 overflow-y-auto p-3">
      <SidebarSearch value={searchText} onChange={onSearchChange} />

      <SmartViews selection={selection} onSelect={onSelect} />

      <ListSection
        selection={selection}
        onSelect={onSelect}
        onShareList={setListToShare}
        onEditList={(list) => setListForm({ open: true, list })}
        onDeleteList={setListToDelete}
        onCreateGroup={() => setGroupForm({ open: true })}
        onRenameGroup={(group) => setGroupForm({ open: true, group })}
        onDeleteGroup={setGroupToDelete}
      />

      <SharedListSection
        selection={selection}
        onSelect={onSelect}
        onShareList={setListToShare}
      />

      <TagSection selection={selection} onSelect={onSelect} />

      <div className="mt-auto flex flex-col gap-1">
        <NotificationToggle />
        <div className="flex gap-1">
          <Button
            variant="ghost"
            className="flex-1 justify-start"
            onClick={() => setListForm({ open: true })}
          >
            <PlusIcon />
            리스트 추가
          </Button>
          <Button
            variant="ghost"
            onClick={() => setGroupForm({ open: true })}
            aria-label="그룹 추가"
            title="그룹 추가"
          >
            <FolderPlusIcon />
          </Button>
        </div>
        <UserMenu />
      </div>

      <ListFormDialog
        open={listForm.open}
        onOpenChange={(open) => setListForm((prev) => ({ ...prev, open }))}
        list={listForm.list}
        onSaved={(list) => {
          if (!listForm.list) onSelect({ type: "list", listId: list.id })
        }}
      />
      <ListDeleteDialog
        list={listToDelete}
        onOpenChange={(open) => {
          if (!open) setListToDelete(null)
        }}
        onDeleted={leaveIfSelected}
      />
      <ListShareDialog
        list={listToShare}
        onOpenChange={(open) => {
          if (!open) setListToShare(null)
        }}
        onLeft={leaveIfSelected}
      />
      <ListGroupFormDialog
        open={groupForm.open}
        onOpenChange={(open) => setGroupForm((prev) => ({ ...prev, open }))}
        group={groupForm.group}
      />
      <ListGroupDeleteDialog
        group={groupToDelete}
        onOpenChange={(open) => {
          if (!open) setGroupToDelete(null)
        }}
      />
    </div>
  )
}
