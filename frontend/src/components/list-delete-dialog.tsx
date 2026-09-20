"use client"

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
import { useDeleteList } from "@/hooks/use-lists"
import type { ReminderList } from "@/lib/lists-api"

export function ListDeleteDialog({
  list,
  onOpenChange,
  onDeleted,
}: {
  list: ReminderList | null
  onOpenChange: (open: boolean) => void
  onDeleted?: (list: ReminderList) => void
}) {
  const deleteList = useDeleteList()

  function handleDelete() {
    if (!list) return
    deleteList.mutate(list.id, {
      onSuccess: () => {
        onOpenChange(false)
        onDeleted?.(list)
      },
    })
  }

  return (
    <AlertDialog open={list !== null} onOpenChange={onOpenChange}>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>&ldquo;{list?.name}&rdquo; 리스트를 삭제할까요?</AlertDialogTitle>
          <AlertDialogDescription>
            리스트에 속한 모든 리마인더도 함께 삭제되며 되돌릴 수 없습니다.
          </AlertDialogDescription>
        </AlertDialogHeader>
        {deleteList.isError && (
          <p className="text-sm text-destructive">리스트를 삭제하지 못했습니다.</p>
        )}
        <AlertDialogFooter>
          <AlertDialogCancel>취소</AlertDialogCancel>
          <AlertDialogAction
            variant="destructive"
            onClick={handleDelete}
            disabled={deleteList.isPending}
          >
            삭제
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  )
}
