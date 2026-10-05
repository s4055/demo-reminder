"use client"

import { LogOutIcon, UserIcon } from "lucide-react"
import { Button } from "@/components/ui/button"
import { useLogout, useMe } from "@/hooks/use-auth"

// 사이드바 하단의 로그인한 사용자 이름과 로그아웃 버튼
export function UserMenu() {
  const { data: user } = useMe()
  const logout = useLogout()

  if (!user) return null

  return (
    <div className="flex items-center gap-2 border-t border-border px-2.5 pt-3">
      <UserIcon className="size-4 shrink-0 text-muted-foreground" />
      <div className="flex min-w-0 flex-1 flex-col">
        <span className="truncate text-sm font-medium">{user.name}</span>
        <span className="truncate text-xs text-muted-foreground">
          {user.email}
        </span>
      </div>
      <Button
        variant="ghost"
        size="icon-sm"
        onClick={() => logout.mutate()}
        disabled={logout.isPending}
        aria-label="로그아웃"
        title="로그아웃"
      >
        <LogOutIcon />
      </Button>
    </div>
  )
}
