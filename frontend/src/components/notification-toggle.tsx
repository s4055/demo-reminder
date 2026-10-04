"use client"

import { BellIcon, BellOffIcon, BellRingIcon } from "lucide-react"
import { Button } from "@/components/ui/button"
import { useNotificationPermission } from "@/hooks/use-notification-permission"

// 사이드바 하단의 브라우저 알림 상태. 권한은 브라우저가 관리하므로 켜기(권한 요청)만 제공하고,
// 끄거나 차단을 풀려면 브라우저 사이트 설정을 안내한다.
export function NotificationToggle() {
  const { permission, requestPermission } = useNotificationPermission()

  switch (permission) {
    case "default":
      return (
        <Button
          variant="ghost"
          className="justify-start"
          onClick={requestPermission}
        >
          <BellIcon />
          알림 켜기
        </Button>
      )
    case "granted":
      return (
        <p
          role="status"
          className="flex items-center gap-2 px-2.5 text-sm text-muted-foreground"
        >
          <BellRingIcon className="size-4" />
          알림 켜짐
        </p>
      )
    case "denied":
      return (
        <div role="status" className="flex flex-col gap-0.5 px-2.5">
          <p className="flex items-center gap-2 text-sm text-destructive">
            <BellOffIcon className="size-4" />
            알림이 차단되었습니다
          </p>
          <p className="text-xs text-muted-foreground">
            브라우저의 사이트 설정에서 알림을 허용해 주세요.
          </p>
        </div>
      )
    case "unsupported":
      return (
        <p className="px-2.5 text-xs text-muted-foreground">
          이 브라우저는 알림을 지원하지 않습니다.
        </p>
      )
    case "unknown":
      return null
  }
}
