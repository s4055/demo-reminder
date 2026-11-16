import { expect, test } from "./support/fixtures"
import { formatDueAt, minutesFrom } from "./support/dates"
import {
  addReminder,
  chooseOption,
  completeCheckbox,
  expectSidebarCount,
  openEditDialog,
  pickDueDate,
  reminderButton,
  reminderItem,
  saveDialog,
  selectList,
} from "./support/ui"

test.describe("리스트 화면", () => {
  test.beforeEach(async ({ signedInPage: page, api }) => {
    await api.createList({ name: "할일 목록" })
    await page.reload()
    await selectList(page, "할일 목록")
  })

  test("addReminder_withoutDueDateOrRepeat_appearsInList", async ({ signedInPage: page }) => {
    await addReminder(page, "우유 사기")

    await expect(reminderButton(page, "우유 사기")).toHaveText("우유 사기")
    await expect(page.getByLabel("리마인더 제목")).toHaveValue("")
    await expectSidebarCount(page, "할일 목록", 1)
  })

  test("toggleFlag_fromList_turnsOnAndOff", async ({ signedInPage: page }) => {
    await addReminder(page, "플래그 대상")
    const main = page.locator("main")

    await main.getByRole("button", { name: "플래그 대상 플래그 지정" }).click()
    const unflag = main.getByRole("button", { name: "플래그 대상 플래그 해제" })
    await expect(unflag).toHaveAttribute("aria-pressed", "true")

    await unflag.click()
    await expect(main.getByRole("button", { name: "플래그 대상 플래그 지정" })).toHaveAttribute(
      "aria-pressed",
      "false"
    )
  })

  test("completeReminder_thenUncomplete_togglesCompletedState", async ({ signedInPage: page }) => {
    await addReminder(page, "완료 대상")
    const checkbox = completeCheckbox(page, "완료 대상")

    await checkbox.click()
    await expect(checkbox).toBeChecked()
    await expect(page.getByText("완료된 항목 1개")).toBeVisible()
    await expectSidebarCount(page, "할일 목록", 0)

    await checkbox.click()
    await expect(checkbox).not.toBeChecked()
    await expect(page.getByText(/완료된 항목/)).toHaveCount(0)
    await expectSidebarCount(page, "할일 목록", 1)
  })

  test("deleteReminder_removesItFromList", async ({ signedInPage: page }) => {
    await addReminder(page, "삭제 대상")

    await page.locator("main").getByRole("button", { name: "삭제 대상 삭제" }).click()

    await expect(reminderButton(page, "삭제 대상")).toHaveCount(0)
    await expect(page.getByText("이 리스트에 리마인더가 없습니다.")).toBeVisible()
  })
})

test("editReminder_changesAllFields_reflectsInList", async ({ signedInPage: page, api }) => {
  const list = await api.createList({ name: "편집 리스트" })
  await api.createReminder({ title: "원래 제목", listId: list.id })
  await page.clock.install({ time: new Date("2027-03-08T10:00:00+09:00") })
  await page.reload()
  await selectList(page, "편집 리스트")

  const dialog = await openEditDialog(page, "원래 제목")
  await dialog.getByLabel("제목").fill("바뀐 제목")
  await dialog.getByLabel("메모").fill("메모 내용")
  await pickDueDate(
    page,
    dialog.getByRole("button", { name: "마감일" }),
    { year: 2027, month: 3, day: 15 },
    "14:30"
  )
  await chooseOption(page, dialog.getByRole("combobox", { name: "우선순위" }), "높음")
  await dialog.getByRole("checkbox", { name: "플래그 지정" }).check()
  await saveDialog(dialog)

  const row = reminderButton(page, "바뀐 제목")
  await expect(row).toContainText("!!!")
  await expect(row).toContainText("우선순위 높음")
  await expect(row).toContainText("3월 15일 (월) 14:30")
  await expect(row).toContainText("메모 내용")
  await expect(
    page.locator("main").getByRole("button", { name: "바뀐 제목 플래그 해제" })
  ).toHaveAttribute("aria-pressed", "true")
  await expect(reminderButton(page, "원래 제목")).toHaveCount(0)
})

test("overdueReminder_isMarkedPastDue", async ({ signedInPage: page, api }) => {
  const now = new Date()
  const pastDue = minutesFrom(now, 60)
  const upcoming = minutesFrom(now, 180)
  const list = await api.createList({ name: "마감 리스트" })
  await api.createReminder({ title: "지난 할일", listId: list.id, dueAt: pastDue.toISOString() })
  await api.createReminder({ title: "남은 할일", listId: list.id, dueAt: upcoming.toISOString() })
  await page.clock.install({ time: minutesFrom(now, 120) })
  await page.reload()
  await selectList(page, "마감 리스트")

  await expect(reminderItem(page, "지난 할일").getByText(formatDueAt(pastDue))).toHaveClass(
    /text-destructive/
  )
  await expect(reminderItem(page, "남은 할일").getByText(formatDueAt(upcoming))).not.toHaveClass(
    /text-destructive/
  )
})

test("dueTime_isShownInEachBrowserTimeZone", async ({ api, openSession }) => {
  const list = await api.createList({ name: "시간대 리스트" })
  await api.createReminder({
    title: "화상 회의",
    listId: list.id,
    dueAt: "2027-03-10T00:30:00Z",
  })

  const seoul = await openSession(api.user, { timezoneId: "Asia/Seoul" })
  const newYork = await openSession(api.user, { timezoneId: "America/New_York" })

  await expect(reminderButton(seoul, "화상 회의")).toContainText("3월 10일 (수) 09:30")
  await expect(reminderButton(newYork, "화상 회의")).toContainText("3월 9일 (화) 19:30")
})
