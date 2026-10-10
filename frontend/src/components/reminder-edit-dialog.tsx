"use client"

import { useState, type KeyboardEvent } from "react"
import { parseISO } from "date-fns"
import { Controller, useForm, useWatch } from "react-hook-form"
import { toast } from "sonner"
import { DueDatePicker } from "@/components/due-date-picker"
import { RepeatSelect } from "@/components/repeat-select"
import { TagInput } from "@/components/tag-input"
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
import { useLists } from "@/hooks/use-lists"
import {
  useCreateReminder,
  useMoveReminder,
  useUpdateReminder,
} from "@/hooks/use-reminders"
import { toDueAtParam } from "@/lib/due-date"
import type { ReminderList } from "@/lib/lists-api"
import { PRIORITIES, PRIORITY_LABELS, type Priority } from "@/lib/priority"
import type { Reminder } from "@/lib/reminders-api"
import {
  NO_REPEAT,
  recurrenceOf,
  toRepeatFields,
  type Recurrence,
} from "@/lib/repeat"
import { cn } from "@/lib/utils"

type ReminderFormValues = {
  title: string
  memo: string
  dueAt: Date | undefined
  recurrence: Recurrence
  flagged: boolean
  priority: Priority
  tagNames: string[]
  // 소속 리스트. 리스트 없이 만든 리마인더는 null이며, 리스트로 옮길 수는 있지만 '리스트 없음'으로 되돌릴 수는 없다.
  listId: number | null
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
  const moveReminder = useMoveReminder()
  const { data: lists } = useLists()
  // 입력 연결 방식은 컴포넌트가 값을 주고받는 방식에 따라 나눈다.
  // - register: 내부가 진짜 HTML 입력 요소인 컴포넌트(Input, Textarea). ref와 onChange 이벤트(event.target.value)로 값을 읽는다.
  // - Controller(control): 진짜 입력 요소 없이 value / onChange 계열 prop으로 값을 주고받는 컴포넌트
  //   (DueDatePicker, Checkbox, Select, TagInput). field.value와 field.onChange를 컴포넌트 prop에 이어 준다.
  const {
    register,
    handleSubmit,
    control,
    setValue,
    formState: { errors },
  } = useForm<ReminderFormValues>({
    defaultValues: {
      title: reminder.title,
      memo: reminder.memo ?? "",
      dueAt: reminder.dueAt ? parseISO(reminder.dueAt) : undefined,
      recurrence: recurrenceOf(reminder),
      flagged: reminder.flagged,
      priority: reminder.priority,
      tagNames: reminder.tags,
      listId: reminder.listId,
    },
  })

  const dueAt = useWatch({ control, name: "dueAt" })

  // 내용을 저장한 뒤 리스트가 바뀌었으면 옮긴다. 하위 작업은 부모를 따라서만 이동하므로 리스트를 바꿀 수 없다.
  function moveIfChanged(listId: number | null) {
    if (listId === null || listId === reminder.listId) {
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
      {
        id: reminder.id,
        input: {
          title: values.title.trim(),
          memo: values.memo.trim() || null,
          dueAt: values.dueAt ? toDueAtParam(values.dueAt) : null,
          flagged: values.flagged,
          priority: values.priority,
          tagNames: values.tagNames,
          ...toRepeatFields(values.dueAt ? values.recurrence : NO_REPEAT),
        },
      },
      { onSuccess: () => moveIfChanged(values.listId) }
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
                  // 반복은 마감일이 있어야 하므로 마감일을 지우면 반복도 해제한다.
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
      {/* 하위 작업은 1단계까지만 만들 수 있으므로 최상위 리마인더에서만 보여준다. */}
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

// 리스트 이름 앞에 사이드바와 같은 색상 점을 보여준다. 리스트가 없으면(null) "리스트 없음"으로 표시한다.
function ListSelect({
  id,
  lists,
  value,
  onChange,
}: {
  id: string
  lists: ReminderList[]
  value: number | null
  onChange: (listId: number | null) => void
}) {
  const listById = new Map(lists.map((list) => [list.id, list]))
  return (
    <Select<number | null>
      value={value}
      onValueChange={(next) => onChange(next ?? value)}
    >
      <SelectTrigger id={id} className="w-full">
        <SelectValue placeholder="리스트 없음">
          {(selected: number | null) => {
            const list = selected === null ? undefined : listById.get(selected)
            return list ? <ListLabel list={list} /> : "리스트 없음"
          }}
        </SelectValue>
      </SelectTrigger>
      <SelectContent>
        {lists.map((list) => (
          <SelectItem key={list.id} value={list.id}>
            <ListLabel list={list} />
          </SelectItem>
        ))}
      </SelectContent>
    </Select>
  )
}

function ListLabel({ list }: { list: ReminderList }) {
  return (
    <span className="flex min-w-0 items-center gap-2">
      <span
        aria-hidden
        className="size-2.5 shrink-0 rounded-full"
        style={{ backgroundColor: list.color ?? "#8E8E93" }}
      />
      <span className="truncate">{list.name}</span>
    </span>
  )
}

// 하위 작업은 편집 폼의 저장과 별개로 추가 즉시 생성한다.
// 이 영역은 편집 폼 안에 있으므로 Enter가 폼을 제출하지 않도록 막는다.
function SubtaskSection({ parent }: { parent: Reminder }) {
  const createReminder = useCreateReminder()
  const [title, setTitle] = useState("")

  function addSubtask() {
    const trimmed = title.trim()
    if (!trimmed || createReminder.isPending) return
    createReminder.mutate(
      { title: trimmed, parentId: parent.id },
      { onSuccess: () => setTitle("") }
    )
  }

  function handleKeyDown(event: KeyboardEvent<HTMLInputElement>) {
    // 한글 조합 중 Enter는 조합 확정이므로 추가하지 않는다.
    if (event.nativeEvent.isComposing || event.key !== "Enter") return
    event.preventDefault()
    addSubtask()
  }

  return (
    <div className="grid gap-1.5">
      <Label htmlFor="reminder-subtask">하위 작업</Label>
      {parent.subtasks.length > 0 && (
        <ul className="grid gap-1 text-sm">
          {parent.subtasks.map((subtask) => (
            <li
              key={subtask.id}
              className={cn(
                "truncate rounded-md bg-secondary px-2 py-1",
                subtask.completed && "text-muted-foreground line-through"
              )}
            >
              {subtask.title}
            </li>
          ))}
        </ul>
      )}
      <div className="flex gap-2">
        <Input
          id="reminder-subtask"
          value={title}
          onChange={(event) => setTitle(event.target.value)}
          onKeyDown={handleKeyDown}
          placeholder="하위 작업 입력 후 Enter"
        />
        <Button
          type="button"
          variant="outline"
          onClick={addSubtask}
          disabled={!title.trim() || createReminder.isPending}
        >
          추가
        </Button>
      </div>
      {createReminder.isError && (
        <p className="text-xs text-destructive">
          하위 작업을 추가하지 못했습니다.
        </p>
      )}
    </div>
  )
}
