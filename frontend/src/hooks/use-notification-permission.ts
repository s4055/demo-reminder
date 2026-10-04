import { useSyncExternalStore } from "react"

// unknown: 서버 렌더링 중이라 아직 브라우저 상태를 모름
export type NotificationPermissionState =
  | NotificationPermission
  | "unsupported"
  | "unknown"

// 알림 권한은 브라우저가 가진 외부 상태다. 권한 요청 결과와 브라우저 설정 변경을 구독자에게 알린다.
const listeners = new Set<() => void>()

function emit() {
  listeners.forEach((listener) => listener())
}

function isSupported(): boolean {
  return typeof window !== "undefined" && "Notification" in window
}

function subscribe(listener: () => void) {
  listeners.add(listener)
  // 사이트 설정에서 권한을 바꾸는 경우도 반영한다 (permissions API가 없으면 생략).
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
