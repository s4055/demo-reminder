"use client"

import { Controller, useForm } from "react-hook-form"
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
import { useCreateList, useUpdateList } from "@/hooks/use-lists"
import type { ReminderList } from "@/lib/lists-api"
import { cn } from "@/lib/utils"

export const LIST_COLORS = [
  "#FF3B30",
  "#FF9500",
  "#FFCC00",
  "#34C759",
  "#007AFF",
  "#AF52DE",
  "#8E8E93",
] as const

const DEFAULT_COLOR = "#007AFF"

type ListFormValues = {
  name: string
  color: string
}

/** list 가 있으면 수정, 없으면 새 리스트 생성 폼이 된다. */
export function ListFormDialog({
  open,
  onOpenChange,
  list,
  onSaved,
}: {
  open: boolean
  onOpenChange: (open: boolean) => void
  list?: ReminderList
  onSaved?: (list: ReminderList) => void
}) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <ListForm
          key={list?.id ?? "new"}
          list={list}
          onSaved={(saved) => {
            onOpenChange(false)
            onSaved?.(saved)
          }}
        />
      </DialogContent>
    </Dialog>
  )
}

function ListForm({
  list,
  onSaved,
}: {
  list?: ReminderList
  onSaved: (list: ReminderList) => void
}) {
  const isEdit = list !== undefined
  const createList = useCreateList()
  const updateList = useUpdateList()
  const mutation = isEdit ? updateList : createList

  const {
    register,
    handleSubmit,
    control,
    formState: { errors },
  } = useForm<ListFormValues>({
    defaultValues: {
      name: list?.name ?? "",
      color: list?.color ?? DEFAULT_COLOR,
    },
  })

  function onSubmit(values: ListFormValues) {
    const input = { name: values.name.trim(), color: values.color }
    const options = { onSuccess: onSaved }
    if (isEdit) {
      updateList.mutate({ id: list.id, input }, options)
    } else {
      createList.mutate(input, options)
    }
  }

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="grid gap-4">
      <DialogHeader>
        <DialogTitle>{isEdit ? "리스트 편집" : "새 리스트"}</DialogTitle>
      </DialogHeader>
      <div className="grid gap-1.5">
        <Label htmlFor="list-name">이름</Label>
        <Input
          id="list-name"
          placeholder="리스트 이름"
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
      <div className="grid gap-1.5">
        <Label>색상</Label>
        <Controller
          control={control}
          name="color"
          render={({ field }) => (
            <div className="flex flex-wrap items-center gap-2">
              {LIST_COLORS.map((swatch) => (
                <button
                  key={swatch}
                  type="button"
                  onClick={() => field.onChange(swatch)}
                  aria-label={`색상 ${swatch}`}
                  aria-pressed={field.value.toLowerCase() === swatch.toLowerCase()}
                  className={cn(
                    "size-6 rounded-full ring-offset-2 ring-offset-popover outline-none focus-visible:ring-2 focus-visible:ring-ring",
                    field.value.toLowerCase() === swatch.toLowerCase() &&
                      "ring-2 ring-foreground"
                  )}
                  style={{ backgroundColor: swatch }}
                />
              ))}
              <input
                type="color"
                value={field.value}
                onChange={(event) => field.onChange(event.target.value)}
                aria-label="사용자 지정 색상"
                className="size-6 cursor-pointer rounded-full border border-border bg-transparent p-0"
              />
            </div>
          )}
        />
      </div>
      {mutation.isError && (
        <p className="text-sm text-destructive">
          리스트를 저장하지 못했습니다.
        </p>
      )}
      <DialogFooter>
        <Button type="submit" disabled={mutation.isPending}>
          {isEdit ? "저장" : "추가"}
        </Button>
      </DialogFooter>
    </form>
  )
}
