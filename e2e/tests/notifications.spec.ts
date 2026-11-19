import type { Page } from "@playwright/test"
import { expect, test } from "./support/fixtures"
import { minutesFrom } from "./support/dates"
import { selectList, sidebar } from "./support/ui"

type RecordedNotification = { title: string }

async function useFakeNotification(page: Page, permission: NotificationPermission) {
  await page.addInitScript((granted) => {
    const recorded: { title: string; onclick: (() => void) | null }[] = []
    class FakeNotification {
      static permission = granted
      static requestPermission = async () => granted
      onclick: (() => void) | null = null
      constructor(readonly title: string) {
        recorded.push(this)
      }
      close() {}
    }
    Object.assign(window, { Notification: FakeNotification, __notifications: recorded })
  }, permission)
}

function recordedNotifications(page: Page): Promise<RecordedNotification[]> {
  return page.evaluate(() =>
    (window as unknown as { __notifications: RecordedNotification[] }).__notifications.map(
      ({ title }) => ({ title })
    )
  )
}

async function openUpcomingAt(page: Page, now: Date) {
  await page.clock.install({ time: now })
  const upcoming = page.waitForResponse((response) =>
    response.url().includes("/api/reminders/upcoming")
  )
  await page.reload()
  await upcoming
}

test("notificationPermission_showsBlockedNoticeOnlyWhenDenied", async ({
  signedInPage: page,
}) => {
  await useFakeNotification(page, "denied")
  await page.reload()
  await expect(sidebar(page).getByText("알림이 차단되었습니다")).toBeVisible()

  await useFakeNotification(page, "granted")
  await page.reload()
  await expect(sidebar(page).getByText("알림 켜짐")).toBeVisible()
  await expect(sidebar(page).getByText("알림이 차단되었습니다")).toHaveCount(0)
})

test("dueReminder_notifiesOnce_andAgainAfterReschedule", async ({ signedInPage: page, api }) => {
  const now = new Date()
  const reminder = await api.createReminder({
    title: "약 먹기",
    dueAt: minutesFrom(now, 1.5).toISOString(),
  })
  await useFakeNotification(page, "granted")
  await openUpcomingAt(page, now)
  await expect(sidebar(page).getByText("알림 켜짐")).toBeVisible()
  expect(await recordedNotifications(page)).toEqual([])

  await page.clock.fastForward("02:00")
  await expect.poll(() => recordedNotifications(page)).toEqual([{ title: "약 먹기" }])

  await page.clock.fastForward("05:00")
  expect(await recordedNotifications(page)).toEqual([{ title: "약 먹기" }])

  const browserNow = new Date(await page.evaluate(() => Date.now()))
  await api.rescheduleReminder(reminder.id, minutesFrom(browserNow, 1.5).toISOString())
  await page.clock.fastForward("01:00")
  await page.clock.fastForward("01:00")
  await expect
    .poll(() => recordedNotifications(page))
    .toEqual([{ title: "약 먹기" }, { title: "약 먹기" }])
})

test("clickNotification_opensEditDialog", async ({ signedInPage: page, api }) => {
  const now = new Date()
  const list = await api.createList({ name: "알림 리스트" })
  await api.createReminder({
    title: "회의 참석",
    listId: list.id,
    dueAt: minutesFrom(now, 1).toISOString(),
  })
  await useFakeNotification(page, "granted")
  await openUpcomingAt(page, now)
  await selectList(page, "알림 리스트")

  await page.clock.fastForward("01:30")
  await expect.poll(() => recordedNotifications(page)).toEqual([{ title: "회의 참석" }])
  await page.evaluate(() =>
    (window as unknown as { __notifications: { onclick: () => void }[] }).__notifications[0].onclick()
  )

  const dialog = page.getByRole("dialog", { name: "리마인더 편집" })
  await expect(dialog).toBeVisible()
  await expect(dialog.getByLabel("제목")).toHaveValue("회의 참석")
})
