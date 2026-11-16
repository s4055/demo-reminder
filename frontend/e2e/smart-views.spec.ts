import { expect, test } from "./support/fixtures"
import { seoulDate } from "./support/dates"
import { reminderCheckboxes, reminderItem, reminderTitles, selectSmartView } from "./support/ui"

test("smartViews_showOnlyMatchingReminders_withListName", async ({ signedInPage: page, api }) => {
  const now = new Date()
  await page.clock.install({ time: now })
  const list = await api.createList({ name: "생활" })
  await api.createReminder({
    title: "오늘 할일",
    listId: list.id,
    dueAt: seoulDate(0, 12, now).toISOString(),
  })
  await api.createReminder({
    title: "내일 할일",
    listId: list.id,
    dueAt: seoulDate(1, 12, now).toISOString(),
  })
  await api.createReminder({ title: "마감 없는 할일", listId: list.id })
  const flagged = await api.createReminder({ title: "플래그 할일", listId: list.id })
  await api.toggleFlag(flagged.id)
  await api.createCompletedReminder({
    title: "끝낸 할일",
    listId: list.id,
    dueAt: seoulDate(0, 12, now).toISOString(),
  })
  await page.reload()

  const expectations: [string, string[]][] = [
    ["오늘", ["오늘 할일"]],
    ["예정됨", ["오늘 할일", "내일 할일"]],
    ["전체", ["오늘 할일", "내일 할일", "마감 없는 할일", "플래그 할일"]],
    ["플래그 지정됨", ["플래그 할일"]],
    ["완료됨", ["끝낸 할일"]],
  ]
  for (const [view, titles] of expectations) {
    await selectSmartView(page, view)
    await expect(reminderCheckboxes(page)).toHaveCount(titles.length)
    expect((await reminderTitles(page)).toSorted()).toEqual(titles.toSorted())
    for (const title of titles) {
      await expect(reminderItem(page, title)).toContainText("생활")
    }
  }
})

test("smartViews_withoutReminders_showViewSpecificEmptyMessage", async ({
  signedInPage: page,
}) => {
  const messages: [string, string][] = [
    ["오늘", "오늘 마감인 리마인더가 없습니다."],
    ["예정됨", "마감일이 설정된 리마인더가 없습니다."],
    ["전체", "리마인더가 없습니다."],
    ["플래그 지정됨", "플래그 지정된 리마인더가 없습니다."],
    ["완료됨", "완료된 리마인더가 없습니다."],
  ]
  for (const [view, message] of messages) {
    await selectSmartView(page, view)
    await expect(page.locator("main").getByText(message, { exact: true })).toBeVisible()
  }
})
