import { SearchIcon, XIcon } from "lucide-react"
import { Input } from "@/components/ui/input"

export function SidebarSearch({
  value,
  onChange,
}: {
  value: string
  onChange: (text: string) => void
}) {
  return (
    <div role="search" className="relative">
      <SearchIcon
        aria-hidden
        className="pointer-events-none absolute top-1/2 left-2.5 size-4 -translate-y-1/2 text-muted-foreground"
      />
      <Input
        type="search"
        value={value}
        onChange={(event) => onChange(event.target.value)}
        onKeyDown={(event) => {
          if (event.key === "Escape") onChange("")
        }}
        placeholder="검색"
        aria-label="리마인더 검색"
        maxLength={100}
        className="pr-8 pl-8 [&::-webkit-search-cancel-button]:hidden"
      />
      {value && (
        <button
          type="button"
          onClick={() => onChange("")}
          aria-label="검색어 지우기"
          className="absolute top-1/2 right-2 flex -translate-y-1/2 items-center rounded-sm text-muted-foreground outline-none hover:text-foreground focus-visible:ring-3 focus-visible:ring-ring/50"
        >
          <XIcon className="size-4" />
        </button>
      )}
    </div>
  )
}
