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
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import { Textarea } from "@/components/ui/textarea"
import { useUpdateReminder } from "@/hooks/use-reminders"
import { toDueAtParam } from "@/lib/due-date"
import { PRIORITIES, PRIORITY_LABELS, type Priority } from "@/lib/priority"
import type { Reminder } from "@/lib/reminders-api"

type ReminderFormValues = {
  title: string
  memo: string
  dueAt: Date | undefined
  flagged: boolean
  priority: Priority
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
  // 입력 연결 방식은 컴포넌트가 값을 주고받는 방식에 따라 나눈다.
  // - register: 내부가 진짜 HTML 입력 요소인 컴포넌트(Input, Textarea). ref와 onChange 이벤트(event.target.value)로 값을 읽는다.
  // - Controller(control): 진짜 입력 요소 없이 value / onChange 계열 prop으로 값을 주고받는 컴포넌트
  //   (DueDatePicker, Checkbox, Select). field.value와 field.onChange를 컴포넌트 prop에 이어 준다.
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
      priority: reminder.priority,
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
          priority: values.priority,
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
