"use client"

import { useState } from "react"
import { toast } from "sonner"
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog"
import { useDeleteCompletedReminders } from "@/hooks/use-reminders"

export function ClearCompletedDialog({
  listId,
  completedCount,
  open,
  onOpenChange,
}: {
  listId: number
  completedCount: number
  open: boolean
  onOpenChange: (open: boolean) => void
}) {
  const deleteCompleted = useDeleteCompletedReminders()
  // 삭제 후 닫힘 애니메이션 동안 개수가 0으로 바뀌지 않도록 열 때의 개수를 유지한다.
  const [shownCount, setShownCount] = useState(completedCount)
  if (open && completedCount > 0 && completedCount !== shownCount) {
    setShownCount(completedCount)
  }

  function handleClear() {
    deleteCompleted.mutate(listId, {
      onSuccess: ({ deletedCount }) => {
        onOpenChange(false)
        toast.success(`완료된 항목 ${deletedCount}개를 지웠습니다.`)
      },
    })
  }

  return (
    <AlertDialog open={open} onOpenChange={onOpenChange}>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>완료된 항목 {shownCount}개를 지울까요?</AlertDialogTitle>
          <AlertDialogDescription>
            완료된 리마인더에 딸린 하위 작업도 함께 삭제되며 되돌릴 수 없습니다.
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel>취소</AlertDialogCancel>
          <AlertDialogAction
            variant="destructive"
            onClick={handleClear}
            disabled={deleteCompleted.isPending}
          >
            지우기
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  )
}
