"use client"

import { useState } from "react"
import { RepeatIcon } from "lucide-react"
import { Button } from "@/components/ui/button"
import {
  Dialog,
  DialogContent,
  DialogDescription,
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
  SelectSeparator,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import {
  DAYS_OF_WEEK,
  DAY_OF_WEEK_LABELS,
  REPEAT_INTERVAL_MAX,
  REPEAT_INTERVAL_MIN,
  REPEAT_RULES,
  REPEAT_RULE_LABELS,
  REPEAT_UNIT_LABELS,
  WEEKDAYS_REPEAT,
  isSameRecurrence,
  simpleRecurrence,
  sortDays,
  summarizeRecurrence,
  type DayOfWeek,
  type Recurrence,
  type RepeatRule,
} from "@/lib/repeat"

// 선택 목록의 항목. 프리셋 외의 반복은 CUSTOM으로 표시하고, EDIT_CUSTOM을 고르면 사용자 지정 다이얼로그를 연다.
type RepeatOption = RepeatRule | "WEEKDAYS" | "CUSTOM" | "EDIT_CUSTOM"

function optionOf(recurrence: Recurrence): RepeatOption {
  if (isSameRecurrence(recurrence, WEEKDAYS_REPEAT)) return "WEEKDAYS"
  if (isSameRecurrence(recurrence, simpleRecurrence(recurrence.rule))) {
    return recurrence.rule
  }
  return "CUSTOM"
}

// 반복은 마감일이 있을 때만 설정할 수 있으므로, 마감일이 없으면 disabled로 막는다.
export function RepeatSelect({
  id,
  value,
  onChange,
  disabled,
  size = "default",
  className,
}: {
  id?: string
  value: Recurrence
  onChange: (value: Recurrence) => void
  disabled?: boolean
  size?: "sm" | "default"
  className?: string
}) {
  const [customOpen, setCustomOpen] = useState(false)

  function handleOptionChange(option: RepeatOption | null) {
    if (option === null || option === "CUSTOM") return
    if (option === "EDIT_CUSTOM") {
      setCustomOpen(true)
    } else if (option === "WEEKDAYS") {
      onChange(WEEKDAYS_REPEAT)
    } else {
      onChange(simpleRecurrence(option))
    }
  }

  return (
    <>
      <Select<RepeatOption>
        value={optionOf(value)}
        onValueChange={handleOptionChange}
        disabled={disabled}
      >
        <SelectTrigger
          id={id}
          size={size}
          aria-label="반복"
          title={disabled ? "마감일을 먼저 지정해 주세요" : undefined}
          className={className}
        >
          <RepeatIcon />
          <SelectValue>{() => summarizeRecurrence(value)}</SelectValue>
        </SelectTrigger>
        <SelectContent>
          {REPEAT_RULES.map((rule) => (
            <SelectItem key={rule} value={rule}>
              {REPEAT_RULE_LABELS[rule]}
            </SelectItem>
          ))}
          <SelectItem value="WEEKDAYS">평일마다</SelectItem>
          <SelectSeparator />
          {optionOf(value) === "CUSTOM" && (
            <SelectItem value="CUSTOM">{summarizeRecurrence(value)}</SelectItem>
          )}
          <SelectItem value="EDIT_CUSTOM">사용자 지정...</SelectItem>
        </SelectContent>
      </Select>
      <CustomRepeatDialog
        open={customOpen}
        onOpenChange={setCustomOpen}
        value={value}
        onSave={(next) => {
          onChange(next)
          setCustomOpen(false)
        }}
      />
    </>
  )
}

type Unit = Exclude<RepeatRule, "NONE">

const UNITS: Unit[] = ["DAILY", "WEEKLY", "MONTHLY", "YEARLY"]

function CustomRepeatDialog({
  open,
  onOpenChange,
  value,
  onSave,
}: {
  open: boolean
  onOpenChange: (open: boolean) => void
  value: Recurrence
  onSave: (value: Recurrence) => void
}) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-sm">
        {/* 열 때마다 현재 반복 값으로 다시 시작한다. */}
        {open && <CustomRepeatForm initial={value} onSave={onSave} />}
      </DialogContent>
    </Dialog>
  )
}

// 편집 모달의 form 안에서도 열리므로 form 요소를 쓰지 않고 버튼으로만 저장한다.
function CustomRepeatForm({
  initial,
  onSave,
}: {
  initial: Recurrence
  onSave: (value: Recurrence) => void
}) {
  const [unit, setUnit] = useState<Unit>(
    initial.rule === "NONE" ? "WEEKLY" : initial.rule
  )
  const [intervalText, setIntervalText] = useState(String(initial.interval))
  const [days, setDays] = useState<DayOfWeek[]>(initial.daysOfWeek)

  const interval = Number(intervalText)
  const intervalValid =
    Number.isInteger(interval) &&
    interval >= REPEAT_INTERVAL_MIN &&
    interval <= REPEAT_INTERVAL_MAX
  const next: Recurrence = {
    rule: unit,
    interval: intervalValid ? interval : 1,
    daysOfWeek: unit === "WEEKLY" ? sortDays(days) : [],
  }

  function toggleDay(day: DayOfWeek) {
    setDays((prev) =>
      prev.includes(day) ? prev.filter((d) => d !== day) : [...prev, day]
    )
  }

  return (
    <div className="grid gap-4">
      <DialogHeader>
        <DialogTitle>사용자 지정 반복</DialogTitle>
        <DialogDescription>
          {intervalValid ? summarizeRecurrence(next) : "간격을 확인해 주세요."}
        </DialogDescription>
      </DialogHeader>
      <div className="grid gap-1.5">
        <Label htmlFor="repeat-interval">반복 간격</Label>
        <div className="flex items-center gap-2">
          <Input
            id="repeat-interval"
            type="number"
            inputMode="numeric"
            min={REPEAT_INTERVAL_MIN}
            max={REPEAT_INTERVAL_MAX}
            value={intervalText}
            onChange={(event) => setIntervalText(event.target.value)}
            aria-invalid={intervalValid ? undefined : true}
            className="w-20"
          />
          <Select<Unit>
            items={REPEAT_UNIT_LABELS}
            value={unit}
            onValueChange={(nextUnit) => nextUnit && setUnit(nextUnit)}
          >
            <SelectTrigger aria-label="반복 단위" className="w-24">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              {UNITS.map((u) => (
                <SelectItem key={u} value={u}>
                  {REPEAT_UNIT_LABELS[u]}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
          <span className="text-sm text-muted-foreground">마다</span>
        </div>
        {!intervalValid && (
          <p className="text-xs text-destructive">
            {REPEAT_INTERVAL_MIN}~{REPEAT_INTERVAL_MAX} 사이의 숫자를 입력해
            주세요.
          </p>
        )}
      </div>
      {unit === "WEEKLY" && (
        <div className="grid gap-1.5">
          <Label id="repeat-days-label">요일</Label>
          <div
            role="group"
            aria-labelledby="repeat-days-label"
            className="flex flex-wrap gap-1"
          >
            {DAYS_OF_WEEK.map((day) => {
              const selected = days.includes(day)
              return (
                <Button
                  key={day}
                  type="button"
                  size="sm"
                  variant={selected ? "default" : "outline"}
                  aria-pressed={selected}
                  onClick={() => toggleDay(day)}
                  className="size-8 p-0"
                >
                  {DAY_OF_WEEK_LABELS[day]}
                </Button>
              )
            })}
          </div>
          <p className="text-xs text-muted-foreground">
            요일을 고르지 않으면 마감일의 요일에 반복합니다.
          </p>
        </div>
      )}
      <DialogFooter>
        <Button
          type="button"
          disabled={!intervalValid}
          onClick={() => onSave(next)}
        >
          완료
        </Button>
      </DialogFooter>
    </div>
  )
}
