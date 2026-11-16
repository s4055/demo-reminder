import { expect, test } from "./support/fixtures"
import {
  reminderButton,
  reminderCheckboxes,
  reminderTitles,
  selectList,
  selectSmartView,
} from "./support/ui"

test("hideCompleted_inList_staysHiddenAfterReload_andCanBeShownAgain", async ({
  signedInPage: page,
  api,
}) => {
  const list = await api.createList({ name: "숨김 리스트" })
  await api.createReminder({ title: "남은 일", listId: list.id })
  await api.createCompletedReminder({ title: "끝난 일", listId: list.id })
  await page.reload()
  await selectList(page, "숨김 리스트")
  await expect(reminderButton(page, "끝난 일")).toBeVisible()

  await page.getByRole("button", { name: "숨기기" }).click()
  await expect(reminderButton(page, "끝난 일")).toHaveCount(0)
  await expect(page.getByText("완료된 항목 1개")).toBeVisible()

  await page.reload()
  await selectList(page, "숨김 리스트")
  await expect(reminderButton(page, "남은 일")).toBeVisible()
  await expect(reminderButton(page, "끝난 일")).toHaveCount(0)

  await page.getByRole("button", { name: "보기" }).click()
  await expect(reminderButton(page, "끝난 일")).toBeVisible()
})

test("clearCompleted_showsCountAndDeletesOnlyThatList", async ({ signedInPage: page, api }) => {
  const target = await api.createList({ name: "정리할 리스트" })
  const other = await api.createList({ name: "다른 리스트" })
  await api.createReminder({ title: "진행 중", listId: target.id })
  await api.createCompletedReminder({ title: "완료 A", listId: target.id })
  await api.createCompletedReminder({ title: "완료 B", listId: target.id })
  await api.createCompletedReminder({ title: "다른 완료", listId: other.id })
  await page.reload()
  await selectList(page, "정리할 리스트")

  await page.locator("main").getByRole("button", { name: "지우기" }).click()
  const confirm = page.getByRole("alertdialog")
  await expect(confirm).toContainText("완료된 항목 2개를 지울까요?")
  await confirm.getByRole("button", { name: "지우기" }).click()
  await expect(confirm).toBeHidden()

  await expect(reminderButton(page, "완료 A")).toHaveCount(0)
  await expect(reminderButton(page, "완료 B")).toHaveCount(0)
  await expect(reminderButton(page, "진행 중")).toBeVisible()
  await expect(page.getByText(/완료된 항목 \d+개$/)).toHaveCount(0)

  await selectSmartView(page, "완료됨")
  await expect.poll(() => reminderTitles(page)).toEqual(["다른 완료"])
})

test("completedView_loadsFirst50_thenAllOnScroll", async ({ signedInPage: page, api }) => {
  test.setTimeout(120_000)
  const list = await api.createList({ name: "많은 완료" })
  const titles = Array.from({ length: 60 }, (_, index) => `완료 ${String(index + 1).padStart(2, "0")}`)
  for (const title of titles) {
    await api.createCompletedReminder({ title, listId: list.id })
  }
  const newestFirst = titles.toReversed()
  await page.reload()

  await selectSmartView(page, "완료됨")
  await expect(reminderCheckboxes(page)).toHaveCount(50)
  expect(await reminderTitles(page)).toEqual(newestFirst.slice(0, 50))

  await page.getByLabel("리마인더 더 불러오기").scrollIntoViewIfNeeded()
  await expect(reminderCheckboxes(page)).toHaveCount(60)
  expect(await reminderTitles(page)).toEqual(newestFirst)
})
