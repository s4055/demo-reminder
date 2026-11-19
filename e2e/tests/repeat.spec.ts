import { expect, test } from "./support/fixtures"
import {
  chooseOption,
  openEditDialog,
  pickDueDate,
  reminderButton,
  reminderItem,
  saveDialog,
  selectList,
  selectSmartView,
} from "./support/ui"

const MARCH_15_2027 = { year: 2027, month: 3, day: 15 }

test.beforeEach(async ({ signedInPage: page, api }) => {
  await api.createList({ name: "반복 리스트" })
  await page.clock.install({ time: new Date("2027-03-08T10:00:00+09:00") })
  await page.reload()
  await selectList(page, "반복 리스트")
})

test("presetRepeat_showsSummaryOnReminder", async ({ signedInPage: page }) => {
  const main = page.locator("main")
  for (const preset of ["매일", "매주", "매월", "매년", "평일마다"]) {
    const title = `${preset} 할일`
    await pickDueDate(page, main.getByRole("button", { name: "마감일" }), MARCH_15_2027)
    await chooseOption(page, main.getByRole("combobox", { name: "반복" }), preset)
    await page.getByLabel("리마인더 제목").fill(title)
    await main.getByRole("button", { name: "추가", exact: true }).click()

    await expect(reminderButton(page, title)).toContainText(`반복:${preset}`)
  }
})

test("customRepeat_withIntervalAndDays_showsSummary", async ({ signedInPage: page }) => {
  const main = page.locator("main")
  await pickDueDate(page, main.getByRole("button", { name: "마감일" }), MARCH_15_2027)
  await chooseOption(page, main.getByRole("combobox", { name: "반복" }), "사용자 지정...")

  const dialog = page.getByRole("dialog", { name: "사용자 지정 반복" })
  await dialog.getByLabel("반복 간격").fill("2")
  await expect(dialog.getByRole("combobox", { name: "반복 단위" })).toContainText("주")
  const days = dialog.getByRole("group", { name: "요일" })
  await days.getByRole("button", { name: "월", exact: true }).click()
  await days.getByRole("button", { name: "수", exact: true }).click()
  await expect(dialog).toContainText("2주마다 월·수")
  await dialog.getByRole("button", { name: "완료" }).click()
  await expect(dialog).toBeHidden()

  await expect(main.getByRole("combobox", { name: "반복" })).toContainText("2주마다 월·수")
  await page.getByLabel("리마인더 제목").fill("격주 운동")
  await main.getByRole("button", { name: "추가", exact: true }).click()
  await expect(reminderButton(page, "격주 운동")).toContainText("반복:2주마다 월·수")
})

test("repeat_withoutDueDate_isDisabledAndCleared", async ({ signedInPage: page, api }) => {
  const main = page.locator("main")
  await expect(main.getByRole("combobox", { name: "반복" })).toBeDisabled()

  const list = (await api.createList({ name: "마감 해제 리스트" })).id
  await api.createReminder({
    title: "매일 산책",
    listId: list,
    dueAt: "2027-03-15T00:00:00Z",
    repeatRule: "DAILY",
  })
  await page.reload()
  await selectList(page, "마감 해제 리스트")
  await expect(reminderButton(page, "매일 산책")).toContainText("반복:매일")

  const dialog = await openEditDialog(page, "매일 산책")
  const repeat = dialog.getByRole("combobox", { name: "반복" })
  await expect(repeat).toContainText("매일")
  await dialog.getByRole("button", { name: "마감일 지우기" }).click()
  await expect(repeat).toContainText("반복 안 함")
  await expect(repeat).toBeDisabled()
  await saveDialog(dialog)

  await expect(reminderButton(page, "매일 산책")).not.toContainText("반복:")
})

test("completeWeeklyReminder_createsNextOccurrenceSevenDaysLater", { tag: "@essential" }, async ({
  signedInPage: page,
  api,
}) => {
  const list = await api.createList({ name: "주간 리스트" })
  await api.createReminder({
    title: "주간 회의",
    listId: list.id,
    dueAt: "2027-03-10T00:00:00Z",
    repeatRule: "WEEKLY",
  })
  await page.reload()
  await selectList(page, "주간 리스트")
  const current = reminderItem(page, "주간 회의")
  await expect(current).toContainText("3월 10일 (수) 09:00")

  await current.getByRole("checkbox").click()

  const main = page.locator("main")
  const next = main.locator("li").filter({ hasText: "3월 17일 (수) 09:00" })
  const done = main.locator("li").filter({ hasText: "3월 10일 (수) 09:00" })
  await expect(next.getByRole("checkbox", { name: "주간 회의 완료 처리" })).not.toBeChecked()
  await expect(done.getByRole("checkbox", { name: "주간 회의 완료 처리" })).toBeChecked()

  await selectSmartView(page, "완료됨")
  await expect(reminderButton(page, "주간 회의")).toHaveCount(1)
  await expect(reminderButton(page, "주간 회의")).toContainText("3월 10일 (수) 09:00")
})
