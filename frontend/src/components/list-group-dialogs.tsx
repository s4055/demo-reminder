"use client"

import { useState } from "react"
import { useForm } from "react-hook-form"
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
import { Button } from "@/components/ui/button"
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import {
  useCreateListGroup,
  useDeleteListGroup,
  useRenameListGroup,
} from "@/hooks/use-list-groups"
import type { ListGroup } from "@/lib/list-groups-api"

export function ListGroupFormDialog({
  open,
  onOpenChange,
  group,
}: {
  open: boolean
  onOpenChange: (open: boolean) => void
  group?: ListGroup
}) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-sm">
        <ListGroupForm
          key={group?.id ?? "new"}
          group={group}
          onSaved={() => onOpenChange(false)}
        />
      </DialogContent>
    </Dialog>
  )
}

function ListGroupForm({
  group,
  onSaved,
}: {
  group?: ListGroup
  onSaved: () => void
}) {
  const isEdit = group !== undefined
  const createGroup = useCreateListGroup()
  const renameGroup = useRenameListGroup()
  const mutation = isEdit ? renameGroup : createGroup
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<{ name: string }>({
    defaultValues: { name: group?.name ?? "" },
  })

  function onSubmit({ name }: { name: string }) {
    const trimmed = name.trim()
    if (isEdit) {
      renameGroup.mutate({ id: group.id, name: trimmed }, { onSuccess: onSaved })
    } else {
      createGroup.mutate(trimmed, { onSuccess: onSaved })
    }
  }

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="grid gap-4">
      <DialogHeader>
        <DialogTitle>{isEdit ? "그룹 이름 변경" : "새 그룹"}</DialogTitle>
      </DialogHeader>
      <div className="grid gap-1.5">
        <Label htmlFor="list-group-name">이름</Label>
        <Input
          id="list-group-name"
          placeholder="그룹 이름"
          autoFocus
          aria-invalid={errors.name ? true : undefined}
          {...register("name", {
            validate: (value) =>
              value.trim().length > 0 || "이름을 입력해 주세요.",
          })}
        />
        {errors.name && (
          <p className="text-xs text-destructive">{errors.name.message}</p>
        )}
      </div>
      <DialogFooter>
        <Button type="submit" disabled={mutation.isPending}>
          {isEdit ? "저장" : "추가"}
        </Button>
      </DialogFooter>
    </form>
  )
}

export function ListGroupDeleteDialog({
  group,
  onOpenChange,
}: {
  group: ListGroup | null
  onOpenChange: (open: boolean) => void
}) {
  const deleteGroup = useDeleteListGroup()
  // 닫힘 애니메이션 동안 제목이 비지 않도록 마지막으로 열었던 그룹을 유지한다.
  const [shownGroup, setShownGroup] = useState(group)
  if (group && group !== shownGroup) setShownGroup(group)

  function handleDelete() {
    if (!group) return
    deleteGroup.mutate(group.id, { onSuccess: () => onOpenChange(false) })
  }

  return (
    <AlertDialog open={group !== null} onOpenChange={onOpenChange}>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>
            &ldquo;{shownGroup?.name}&rdquo; 그룹을 삭제할까요?
          </AlertDialogTitle>
          <AlertDialogDescription>
            그룹만 삭제됩니다. 그룹 안의 리스트와 리마인더는 그대로 남습니다.
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel>취소</AlertDialogCancel>
          <AlertDialogAction
            variant="destructive"
            onClick={handleDelete}
            disabled={deleteGroup.isPending}
          >
            삭제
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  )
}
