"use client"

import { useId } from "react"
import { format, set } from "date-fns"
import { CalendarIcon, XIcon } from "lucide-react"
import { ko } from "react-day-picker/locale"
import { Button } from "@/components/ui/button"
import { Calendar } from "@/components/ui/calendar"
import { Input } from "@/components/ui/input"
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover"
import { formatDueDate } from "@/lib/due-date"

const DEFAULT_HOUR = 9

export function DueDatePicker({
  value,
  onChange,
}: {
  value: Date | undefined
  onChange: (value: Date | undefined) => void
}) {
  const timeInputId = useId()

  function handleDaySelect(day: Date | undefined) {
    if (!day) {
      onChange(undefined)
      return
    }
    onChange(
      set(day, {
        hours: value?.getHours() ?? DEFAULT_HOUR,
        minutes: value?.getMinutes() ?? 0,
        seconds: 0,
        milliseconds: 0,
      })
    )
  }

  function handleTimeChange(time: string) {
    const [hours, minutes] = time.split(":").map(Number)
    if (!value || Number.isNaN(hours) || Number.isNaN(minutes)) return
    onChange(set(value, { hours, minutes }))
  }

  return (
    <div className="flex items-center gap-1">
      <Popover>
        <PopoverTrigger
          render={<Button type="button" variant="outline" size="sm" />}
        >
          <CalendarIcon />
          {value ? formatDueDate(value) : "마감일"}
        </PopoverTrigger>
        <PopoverContent align="start" className="w-auto gap-1 p-0">
          <Calendar
            mode="single"
            locale={ko}
            selected={value}
            onSelect={handleDaySelect}
            autoFocus
          />
          <div className="flex items-center gap-2 border-t border-border px-3 py-2">
            <label
              htmlFor={timeInputId}
              className="text-xs text-muted-foreground"
            >
              시간
            </label>
            <Input
              id={timeInputId}
              type="time"
              disabled={!value}
              value={value ? format(value, "HH:mm") : ""}
              onChange={(event) => handleTimeChange(event.target.value)}
              className="h-7 w-28"
            />
          </div>
        </PopoverContent>
      </Popover>
      {value && (
        <Button
          type="button"
          variant="ghost"
          size="icon-sm"
          onClick={() => onChange(undefined)}
          aria-label="마감일 지우기"
        >
          <XIcon />
        </Button>
      )}
    </div>
  )
}
