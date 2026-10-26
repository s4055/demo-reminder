import { useSyncExternalStore } from "react"

export type NotificationPermissionState =
  | NotificationPermission
  | "unsupported"
  | "unknown"

const listeners = new Set<() => void>()

function emit() {
  listeners.forEach((listener) => listener())
}

function isSupported(): boolean {
  return typeof window !== "undefined" && "Notification" in window
}

function subscribe(listener: () => void) {
  listeners.add(listener)
  let status: PermissionStatus | undefined
  navigator.permissions
    ?.query({ name: "notifications" })
    .then((result) => {
      status = result
      status.addEventListener("change", emit)
    })
    .catch(() => {})
  return () => {
    listeners.delete(listener)
    status?.removeEventListener("change", emit)
  }
}

function getSnapshot(): NotificationPermissionState {
  return isSupported() ? Notification.permission : "unsupported"
}

// 서버 렌더링에서는 브라우저 상태를 알 수 없으므로 unknown으로 두고, 하이드레이션 뒤 실제 값으로 바뀐다.
function getServerSnapshot(): NotificationPermissionState {
  return "unknown"
}

export function useNotificationPermission() {
  const permission = useSyncExternalStore(subscribe, getSnapshot, getServerSnapshot)

  async function requestPermission() {
    if (!isSupported()) return
    await Notification.requestPermission()
    emit()
  }

  return { permission, requestPermission }
}
