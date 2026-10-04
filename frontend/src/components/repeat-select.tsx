"use client"

import { RepeatIcon } from "lucide-react"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import { REPEAT_RULES, REPEAT_RULE_LABELS, type RepeatRule } from "@/lib/repeat"

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
  value: RepeatRule
  onChange: (value: RepeatRule) => void
  disabled?: boolean
  size?: "sm" | "default"
  className?: string
}) {
  return (
    <Select
      items={REPEAT_RULE_LABELS}
      value={value}
      onValueChange={(next) => onChange(next ?? "NONE")}
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
        <SelectValue />
      </SelectTrigger>
      <SelectContent>
        {REPEAT_RULES.map((rule) => (
          <SelectItem key={rule} value={rule}>
            {REPEAT_RULE_LABELS[rule]}
          </SelectItem>
        ))}
      </SelectContent>
    </Select>
  )
}
