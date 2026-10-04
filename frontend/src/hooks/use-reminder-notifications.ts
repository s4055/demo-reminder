import { useEffect, useEffectEvent } from "react"
import { useQuery } from "@tanstack/react-query"
import { parseISO } from "date-fns"
import { remindersQueryKey } from "@/hooks/query-keys"
import { formatDueAt } from "@/lib/due-date"
import {
  hasNotified,
  markNotified,
  notificationKey,
} from "@/lib/notified-reminders"
import { getUpcomingReminders, type Reminder } from "@/lib/reminders-api"

// 1분마다 앞으로 2분 안에 마감되는 리마인더를 받아와 마감 시각에 맞춰 타이머를 건다.
// 조회 주기보다 넉넉한 구간을 받아 두면 다음 조회가 조금 늦어도 빠지는 리마인더가 없다.
const POLL_INTERVAL_MS = 60_000
const LOOKAHEAD_MS = 2 * 60_000
// 마감 시각을 이만큼 지난 리마인더까지는 바로 알린다 (탭이 잠시 멈췄다 깨어난 경우 등).
// 앱을 오랜만에 열었을 때 지난 리마인더가 한꺼번에 뜨지 않도록 짧게 둔다.
const GRACE_MS = 60_000

// 리마인더 변경 시 무효화되도록 리마인더 캐시 키 아래에 둔다 (완료/마감일 변경이 바로 반영된다).
const upcomingQueryKey = [...remindersQueryKey, "upcoming"] as const

/**
 * 앱이 열려 있는 동안 마감 시각이 된 리마인더를 브라우저 알림으로 띄운다.
 * 이미 알린 리마인더(id + dueAt)는 localStorage에 기록해 새로고침하거나 다시 조회해도 한 번만 알린다.
 */
export function useReminderNotifications({
  enabled,
  onOpen,
}: {
  enabled: boolean
  // 알림을 클릭하면 해당 리마인더를 연다.
  onOpen: (reminder: Reminder) => void
}) {
  const { data: upcoming } = useQuery({
    queryKey: upcomingQueryKey,
    queryFn: () => {
      const now = Date.now()
      return getUpcomingReminders(
        new Date(now - GRACE_MS),
        new Date(now + LOOKAHEAD_MS)
      )
    },
    enabled,
    refetchInterval: POLL_INTERVAL_MS,
    // 다른 탭을 보고 있을 때도 알려야 하므로 백그라운드에서도 조회한다.
    refetchIntervalInBackground: true,
  })

  const openReminder = useEffectEvent((reminder: Reminder) => onOpen(reminder))

  useEffect(() => {
    if (!enabled || !upcoming) return

    const timers = upcoming.flatMap((reminder) => {
      if (!reminder.dueAt) return []
      const key = notificationKey(reminder)
      if (hasNotified(key)) return []
      const delay = parseISO(reminder.dueAt).getTime() - Date.now()
      if (delay < -GRACE_MS) return []

      return [
        window.setTimeout(() => {
          // 그 사이 다른 탭이나 이전 타이머가 이미 알렸을 수 있으므로 띄우기 직전에 다시 확인한다.
          if (hasNotified(key) || Notification.permission !== "granted") return
          markNotified(key)
          showNotification(reminder, () => openReminder(reminder))
        }, Math.max(delay, 0)),
      ]
    })

    return () => timers.forEach((timer) => window.clearTimeout(timer))
  }, [enabled, upcoming])
}

function showNotification(reminder: Reminder, onClick: () => void) {
  const notification = new Notification(reminder.title, {
    body: reminder.dueAt ? `${formatDueAt(reminder.dueAt)} 마감` : undefined,
    // 같은 리마인더 알림은 여러 탭에서 떠도 하나로 합쳐진다.
    tag: notificationKey(reminder),
  })
  notification.onclick = () => {
    window.focus()
    onClick()
    notification.close()
  }
}
