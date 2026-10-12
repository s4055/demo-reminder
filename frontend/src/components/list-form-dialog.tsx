"use client"

import { useState } from "react"
import { Controller, useForm, useWatch } from "react-hook-form"
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
  useCreateList,
  useFetchLatestList,
  useUpdateList,
} from "@/hooks/use-lists"
import { isVersionConflict } from "@/lib/api"
import {
  LIST_ICONS,
  LIST_ICON_KEYS,
  isListIconKey,
  type ListIconKey,
} from "@/lib/list-icons"
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
  // null이면 아이콘 없이 색상 점으로 표시한다.
  icon: ListIconKey | null
}

function formValuesOf(list: ReminderList | undefined): ListFormValues {
  return {
    name: list?.name ?? "",
    color: list?.color ?? DEFAULT_COLOR,
    icon: list && isListIconKey(list.icon) ? list.icon : null,
  }
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
          onClose={() => onOpenChange(false)}
        />
      </DialogContent>
    </Dialog>
  )
}

function ListForm({
  list,
  onSaved,
  onClose,
}: {
  list?: ReminderList
  onSaved: (list: ReminderList) => void
  onClose: () => void
}) {
  const isEdit = list !== undefined
  const createList = useCreateList()
  const updateList = useUpdateList()
  const mutation = isEdit ? updateList : createList
  const fetchLatestList = useFetchLatestList()
  // 폼을 채운 시점의 version. 저장할 때 보내 그 사이 다른 변경이 먼저 저장되었으면 409로 거부되게 한다.
  const [version, setVersion] = useState(list?.version)

  const {
    register,
    handleSubmit,
    control,
    reset,
    formState: { errors },
  } = useForm<ListFormValues>({ defaultValues: formValuesOf(list) })
  const color = useWatch({ control, name: "color" })

  // 다른 변경이 먼저 저장되어 거부되면(409) 최신 내용과 version으로 폼을 다시 채운다 (입력 중이던 내용은 버린다).
  // 그 사이 삭제되었으면 창을 닫는다. 다시 불러오지 못하면 그대로 두며, 다시 저장하면 또 409가 되어 다시 시도한다.
  async function reloadLatest(id: number) {
    try {
      const latest = await fetchLatestList(id)
      if (!latest) {
        onClose()
        return
      }
      setVersion(latest.version)
      reset(formValuesOf(latest))
    } catch {
      // 전역 토스트로 충돌은 이미 알렸다.
    }
  }

  function onSubmit(values: ListFormValues) {
    const input = {
      name: values.name.trim(),
      color: values.color,
      icon: values.icon,
    }
    const options = { onSuccess: onSaved }
    if (isEdit) {
      updateList.mutate(
        { id: list.id, input: { ...input, version: version ?? list.version } },
        {
          ...options,
          onError: (error) => {
            if (isVersionConflict(error)) reloadLatest(list.id)
          },
        }
      )
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
      <div className="grid gap-1.5">
        <Label id="list-icon-label">아이콘</Label>
        <Controller
          control={control}
          name="icon"
          render={({ field }) => (
            <div
              role="group"
              aria-labelledby="list-icon-label"
              className="grid grid-cols-8 gap-1"
            >
              <IconOption
                label="아이콘 없음"
                selected={field.value === null}
                onSelect={() => field.onChange(null)}
              >
                <span
                  className="size-3 rounded-full"
                  style={{ backgroundColor: color }}
                />
              </IconOption>
              {LIST_ICON_KEYS.map((key) => {
                const Icon = LIST_ICONS[key]
                return (
                  <IconOption
                    key={key}
                    label={`아이콘 ${key}`}
                    selected={field.value === key}
                    onSelect={() => field.onChange(key)}
                  >
                    <Icon className="size-4" style={{ color }} />
                  </IconOption>
                )
              })}
            </div>
          )}
        />
      </div>
      <DialogFooter>
        <Button type="submit" disabled={mutation.isPending}>
          {isEdit ? "저장" : "추가"}
        </Button>
      </DialogFooter>
    </form>
  )
}

function IconOption({
  label,
  selected,
  onSelect,
  children,
}: {
  label: string
  selected: boolean
  onSelect: () => void
  children: React.ReactNode
}) {
  return (
    <button
      type="button"
      onClick={onSelect}
      aria-label={label}
      aria-pressed={selected}
      className={cn(
        "flex aspect-square items-center justify-center rounded-md outline-none transition-colors hover:bg-muted focus-visible:ring-2 focus-visible:ring-ring",
        selected && "bg-muted ring-2 ring-foreground"
      )}
    >
      {children}
    </button>
  )
}
