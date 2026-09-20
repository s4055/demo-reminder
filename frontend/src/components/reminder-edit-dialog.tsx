"use client"

import { parseISO } from "date-fns"
import { Controller, useForm } from "react-hook-form"
import { DueDatePicker } from "@/components/due-date-picker"
import { Button } from "@/components/ui/button"
import { Checkbox } from "@/components/ui/checkbox"
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Textarea } from "@/components/ui/textarea"
import { useUpdateReminder } from "@/hooks/use-reminders"
import { toDueAtParam } from "@/lib/due-date"
import type { Reminder } from "@/lib/reminders-api"

type ReminderFormValues = {
  title: string
  memo: string
  dueAt: Date | undefined
  flagged: boolean
}

export function ReminderEditDialog({
  open,
  onOpenChange,
  reminder,
}: {
  open: boolean
  onOpenChange: (open: boolean) => void
  reminder: Reminder
}) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <ReminderEditForm
          key={reminder.id}
          reminder={reminder}
          onSaved={() => onOpenChange(false)}
        />
      </DialogContent>
    </Dialog>
  )
}

function ReminderEditForm({
  reminder,
  onSaved,
}: {
  reminder: Reminder
  onSaved: () => void
}) {
  const updateReminder = useUpdateReminder()
  const {
    register,
    handleSubmit,
    control,
    formState: { errors },
  } = useForm<ReminderFormValues>({
    defaultValues: {
      title: reminder.title,
      memo: reminder.memo ?? "",
      dueAt: reminder.dueAt ? parseISO(reminder.dueAt) : undefined,
      flagged: reminder.flagged,
    },
  })

  function onSubmit(values: ReminderFormValues) {
    updateReminder.mutate(
      {
        id: reminder.id,
        input: {
          title: values.title.trim(),
          memo: values.memo.trim() || null,
          dueAt: values.dueAt ? toDueAtParam(values.dueAt) : null,
          flagged: values.flagged,
        },
      },
      { onSuccess: onSaved }
    )
  }

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="grid gap-4">
      <DialogHeader>
        <DialogTitle>리마인더 편집</DialogTitle>
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
      <div className="grid gap-1.5">
        <Label>마감일</Label>
        <Controller
          control={control}
          name="dueAt"
          render={({ field }) => (
            <DueDatePicker value={field.value} onChange={field.onChange} />
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
      <DialogFooter>
        <Button type="submit" disabled={updateReminder.isPending}>
          저장
        </Button>
      </DialogFooter>
    </form>
  )
}
