"use client"

import { useState } from "react"
import { Controller, useForm, useWatch } from "react-hook-form"
import { toast } from "sonner"
import { DueDatePicker } from "@/components/due-date-picker"
import { RepeatSelect } from "@/components/repeat-select"
import { TagInput } from "@/components/tag-input"
import { Button } from "@/components/ui/button"
import { Checkbox } from "@/components/ui/checkbox"
import { DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import { Textarea } from "@/components/ui/textarea"
import {
  formValuesOf,
  toUpdateInput,
  type ReminderFormValues,
} from "@/components/reminder-edit-dialog/form-values"
import { ListSelect } from "@/components/reminder-edit-dialog/list-select"
import { SubtaskSection } from "@/components/reminder-edit-dialog/subtask-section"
import { useLists } from "@/hooks/use-lists"
import {
  useFetchLatestReminder,
  useMoveReminder,
  useUpdateReminder,
} from "@/hooks/use-reminders"
import { isVersionConflict } from "@/lib/api"
import { PRIORITIES, PRIORITY_LABELS } from "@/lib/priority"
import type { Reminder } from "@/lib/reminders-api"
import { NO_REPEAT } from "@/lib/repeat"

export function ReminderEditForm({
  reminder,
  onSaved,
}: {
  reminder: Reminder
  onSaved: () => void
}) {
  const updateReminder = useUpdateReminder()
  const moveReminder = useMoveReminder()
  const fetchLatestReminder = useFetchLatestReminder()
  const { data: lists } = useLists()
  // 폼을 채운 시점의 리마인더. 목록이 주기적으로 다시 조회되어 reminder가 바뀌어도, 저장할 때는 편집을 시작한 시점의
  // version을 보내야 그 사이 다른 사용자가 저장한 변경을 덮어쓰지 않는다 (다르면 409).
  const [base, setBase] = useState(reminder)
  const {
    register,
    handleSubmit,
    control,
    setValue,
    reset,
    formState: { errors },
  } = useForm<ReminderFormValues>({ defaultValues: formValuesOf(reminder) })

  const dueAt = useWatch({ control, name: "dueAt" })

  async function reloadLatest() {
    try {
      const latest = await fetchLatestReminder(reminder.id)
      if (!latest) {
        onSaved()
        return
      }
      setBase(latest)
      reset(formValuesOf(latest))
    } catch {
    }
  }

  function moveIfChanged(listId: number | null) {
    if (listId === null || listId === base.listId) {
      onSaved()
      return
    }
    moveReminder.mutate(
      { id: reminder.id, listId },
      {
        onSuccess: () => {
          const target = lists?.find((list) => list.id === listId)
          toast.success(`“${target?.name ?? "리스트"}”(으)로 이동했습니다.`)
          onSaved()
        },
      }
    )
  }

  function onSubmit(values: ReminderFormValues) {
    updateReminder.mutate(
      { id: reminder.id, input: toUpdateInput(values, base.version) },
      {
        onSuccess: () => moveIfChanged(values.listId),
        onError: (error) => {
          if (isVersionConflict(error)) reloadLatest()
        },
      }
    )
  }

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="grid gap-4">
      <DialogHeader>
        <DialogTitle>
          {reminder.parentId === null ? "리마인더 편집" : "하위 작업 편집"}
        </DialogTitle>
      </DialogHeader>
      <div className="grid gap-1.5">
        <Label htmlFor="reminder-title">제목</Label>
        <Input
          id="reminder-title"
          autoFocus
          aria-invalid={errors.title ? true : undefined}
          {...register("title", {
            validate: (value) =>
              value.trim().length > 0 || "제목을 입력해 주세요.",
          })}
        />
        {errors.title && (
          <p className="text-xs text-destructive">{errors.title.message}</p>
        )}
      </div>
      <div className="grid gap-1.5">
        <Label htmlFor="reminder-memo">메모</Label>
        <Textarea id="reminder-memo" rows={3} {...register("memo")} />
      </div>
      {reminder.parentId === null && (
        <div className="grid gap-1.5">
          <Label htmlFor="reminder-list">리스트</Label>
          <Controller
            control={control}
            name="listId"
            render={({ field }) => (
              <ListSelect
                id="reminder-list"
                lists={lists ?? []}
                value={field.value}
                onChange={field.onChange}
              />
            )}
          />
        </div>
      )}
      <div className="grid gap-1.5">
        <Label>마감일 / 반복</Label>
        <div className="flex flex-wrap items-center gap-2">
          <Controller
            control={control}
            name="dueAt"
            render={({ field }) => (
              <DueDatePicker
                value={field.value}
                onChange={(value) => {
                  field.onChange(value)
                  if (!value) setValue("recurrence", NO_REPEAT)
                }}
              />
            )}
          />
          <Controller
            control={control}
            name="recurrence"
            render={({ field }) => (
              <RepeatSelect
                id="reminder-repeat"
                size="sm"
                value={field.value}
                onChange={field.onChange}
                disabled={!dueAt}
              />
            )}
          />
        </div>
      </div>
      <div className="grid gap-1.5">
        <Label htmlFor="reminder-priority">우선순위</Label>
        <Controller
          control={control}
          name="priority"
          render={({ field }) => (
            <Select
              items={PRIORITY_LABELS}
              value={field.value}
              onValueChange={(value) => field.onChange(value ?? "NONE")}
            >
              <SelectTrigger id="reminder-priority" className="w-full">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {PRIORITIES.map((priority) => (
                  <SelectItem key={priority} value={priority}>
                    {PRIORITY_LABELS[priority]}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          )}
        />
      </div>
      <div className="grid gap-1.5">
        <Label htmlFor="reminder-tags">태그</Label>
        <Controller
          control={control}
          name="tagNames"
          render={({ field }) => (
            <TagInput
              id="reminder-tags"
              value={field.value}
              onChange={field.onChange}
            />
          )}
        />
      </div>
      <div className="flex items-center gap-2">
        <Controller
          control={control}
          name="flagged"
          render={({ field }) => (
            <Checkbox
              id="reminder-flagged"
              checked={field.value}
              onCheckedChange={field.onChange}
            />
          )}
        />
        <Label htmlFor="reminder-flagged">플래그 지정</Label>
      </div>
      {reminder.parentId === null && <SubtaskSection parent={reminder} />}
      <DialogFooter>
        <Button
          type="submit"
          disabled={updateReminder.isPending || moveReminder.isPending}
        >
          저장
        </Button>
      </DialogFooter>
    </form>
  )
}
