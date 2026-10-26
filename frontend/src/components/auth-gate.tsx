"use client"

import { useEffect, type ReactNode } from "react"
import { useRouter } from "next/navigation"
import { Button } from "@/components/ui/button"
import { isUnauthorized, useMe } from "@/hooks/use-auth"

export function AuthGate({ children }: { children: ReactNode }) {
  const router = useRouter()
  const { data: user, error, isError, refetch } = useMe()
  const unauthorized = isError && isUnauthorized(error)

  useEffect(() => {
    if (unauthorized) router.replace("/login")
  }, [unauthorized, router])

  if (user) return children

  if (isError && !unauthorized) {
    return (
      <div className="flex flex-1 flex-col items-center justify-center gap-3 p-6 text-sm text-muted-foreground">
        <p>서버에 연결하지 못했습니다.</p>
        <Button variant="outline" size="sm" onClick={() => refetch()}>
          다시 시도
        </Button>
      </div>
    )
  }

  return (
    <div
      role="status"
      className="flex flex-1 items-center justify-center p-6 text-sm text-muted-foreground"
    >
      불러오는 중...
    </div>
  )
}
