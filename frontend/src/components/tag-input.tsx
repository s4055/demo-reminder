"use client"

import { useState, type KeyboardEvent } from "react"
import { XIcon } from "lucide-react"
import { normalizeTagName } from "@/lib/tags-api"

const TAG_NAME_MAX_LENGTH = 50

/** 입력 후 Enter(또는 쉼표)로 태그 칩을 추가하고, 칩의 X 또는 빈 입력에서 Backspace로 제거한다. */
export function TagInput({
  id,
  value,
  onChange,
}: {
  id?: string
  value: string[]
  onChange: (value: string[]) => void
}) {
  const [draft, setDraft] = useState("")

  function commitDraft() {
    const name = normalizeTagName(draft)
    setDraft("")
    if (name && !value.includes(name)) onChange([...value, name])
  }

  function handleKeyDown(event: KeyboardEvent<HTMLInputElement>) {
    // 한글 조합 중 Enter는 조합 확정이므로 태그로 추가하지 않는다.
    if (event.nativeEvent.isComposing) return
    if (event.key === "Enter" || event.key === ",") {
      // 폼 제출을 막고 태그만 추가한다.
      event.preventDefault()
      commitDraft()
    } else if (event.key === "Backspace" && draft === "" && value.length > 0) {
      onChange(value.slice(0, -1))
    }
  }

  return (
    <div className="flex min-h-8 w-full flex-wrap items-center gap-1 rounded-lg border border-input bg-transparent px-1.5 py-1 text-sm transition-colors focus-within:border-ring focus-within:ring-3 focus-within:ring-ring/50 dark:bg-input/30">
      {value.map((name) => (
        <span
          key={name}
          className="inline-flex h-6 items-center gap-0.5 rounded-md bg-secondary pr-0.5 pl-2 text-xs text-secondary-foreground"
        >
          #{name}
          <button
            type="button"
            onClick={() => onChange(value.filter((item) => item !== name))}
            aria-label={`${name} 태그 제거`}
            className="inline-flex size-5 items-center justify-center rounded-sm outline-none hover:bg-foreground/10 focus-visible:ring-2 focus-visible:ring-ring"
          >
            <XIcon className="size-3" />
          </button>
        </span>
      ))}
      <input
        id={id}
        value={draft}
        maxLength={TAG_NAME_MAX_LENGTH}
        onChange={(event) => setDraft(event.target.value)}
        onKeyDown={handleKeyDown}
        // 입력 중인 태그도 포커스를 잃을 때(저장 버튼 클릭 등) 칩으로 확정한다.
        onBlur={commitDraft}
        placeholder={value.length === 0 ? "태그 입력 후 Enter" : ""}
        className="h-6 min-w-24 flex-1 bg-transparent px-1 outline-none placeholder:text-muted-foreground"
      />
    </div>
  )
}
