"use client"

import { useEffect, useEffectEvent, useRef } from "react"
import { Skeleton } from "@/components/ui/skeleton"

export function LoadMoreTrigger({
  loading,
  onVisible,
}: {
  loading: boolean
  onVisible: () => void
}) {
  const ref = useRef<HTMLDivElement>(null)
  const handleVisible = useEffectEvent(onVisible)

  useEffect(() => {
    const element = ref.current
    if (!element || loading) return
    const observer = new IntersectionObserver(
      (entries) => {
        if (entries.some((entry) => entry.isIntersecting)) handleVisible()
      },
      { rootMargin: "200px" }
    )
    observer.observe(element)
    return () => observer.disconnect()
  }, [loading])

  return (
    <div ref={ref} aria-label="리마인더 더 불러오기" className="flex w-full flex-col gap-1">
      {loading && <Skeleton className="h-12 w-full" />}
    </div>
  )
}
