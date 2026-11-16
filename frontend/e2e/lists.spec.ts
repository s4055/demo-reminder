import { expect, test } from "./support/fixtures"
import {
  addReminder,
  completeCheckbox,
  expectSidebarCount,
  pageTitle,
  reminderButton,
  selectList,
  sidebar,
  sidebarItem,
} from "./support/ui"

test("createList_withColorAndIcon_showsInSidebarAndSelectsIt", async ({ signedInPage: page }) => {
  await sidebar(page).getByRole("button", { name: "리스트 추가" }).click()
  const dialog = page.getByRole("dialog", { name: "새 리스트" })
  await dialog.getByLabel("이름").fill("여행")
  await dialog.getByRole("button", { name: "색상 #34C759" }).click()
  await dialog.getByRole("button", { name: "아이콘 plane" }).click()
  await dialog.getByRole("button", { name: "추가" }).click()
  await expect(dialog).toBeHidden()

  const item = sidebarItem(page, "여행")
  await expect(item).toHaveAccessibleName("여행 0")
  await expect(item).toHaveAttribute("aria-current", "page")
  await expect(item.locator('svg[data-icon="plane"]')).toHaveCSS("color", "rgb(52, 199, 89)")
  await expect(pageTitle(page)).toHaveText("여행")
  await expect(pageTitle(page)).toHaveCSS("color", "rgb(52, 199, 89)")
})

test("editList_changesNameColorAndIcon_reflectsInSidebarAndTitle", async ({
  signedInPage: page,
  api,
}) => {
  await api.createList({ name: "장보기", color: "#FF3B30" })
  await page.reload()
  await selectList(page, "장보기")

  await sidebar(page).getByRole("button", { name: "장보기 리스트 편집" }).click()
  const dialog = page.getByRole("dialog", { name: "리스트 편집" })
  await dialog.getByLabel("이름").fill("마트")
  await dialog.getByRole("button", { name: "색상 #007AFF" }).click()
  await dialog.getByRole("button", { name: "아이콘 shopping-cart" }).click()
  await dialog.getByRole("button", { name: "저장" }).click()
  await expect(dialog).toBeHidden()

  const item = sidebarItem(page, "마트")
  await expect(item).toHaveAccessibleName("마트 0")
  await expect(item.locator('svg[data-icon="shopping-cart"]')).toHaveCSS(
    "color",
    "rgb(0, 122, 255)"
  )
  await expect(sidebarItem(page, "장보기")).toHaveCount(0)
  await expect(pageTitle(page)).toHaveText("마트")
  await expect(pageTitle(page)).toHaveCSS("color", "rgb(0, 122, 255)")
})

test("deleteList_afterConfirm_removesListAndItsReminders", async ({ signedInPage: page, api }) => {
  const doomed = await api.createList({ name: "지울 리스트" })
  const kept = await api.createList({ name: "남길 리스트" })
  await api.createReminder({ title: "사라질 할일", listId: doomed.id })
  await api.createReminder({ title: "남을 할일", listId: kept.id })
  await page.reload()
  await expect(reminderButton(page, "사라질 할일")).toBeVisible()

  await sidebar(page).getByRole("button", { name: "지울 리스트 리스트 삭제" }).click()
  const confirm = page.getByRole("alertdialog")
  await expect(confirm).toContainText("“지울 리스트” 리스트를 삭제할까요?")
  await confirm.getByRole("button", { name: "삭제" }).click()
  await expect(confirm).toBeHidden()

  await expect(sidebarItem(page, "지울 리스트")).toHaveCount(0)
  await expect(pageTitle(page)).toHaveText("전체")
  await expect(reminderButton(page, "사라질 할일")).toHaveCount(0)
  await expect(reminderButton(page, "남을 할일")).toBeVisible()
})

test("listCount_countsIncompleteTopLevelReminders_andFollowsChanges", async ({
  signedInPage: page,
  api,
}) => {
  const list = await api.createList({ name: "카운트" })
  const parent = await api.createReminder({ title: "첫째", listId: list.id })
  await api.createReminder({ title: "첫째의 하위", parentId: parent.id })
  await api.createReminder({ title: "둘째", listId: list.id })
  await api.createCompletedReminder({ title: "끝난 일", listId: list.id })
  await page.reload()
  await expectSidebarCount(page, "카운트", 2)

  await selectList(page, "카운트")
  await addReminder(page, "셋째")
  await expectSidebarCount(page, "카운트", 3)

  await completeCheckbox(page, "둘째").click()
  await expect(completeCheckbox(page, "둘째")).toBeChecked()
  await expectSidebarCount(page, "카운트", 2)

  await page.locator("main").getByRole("button", { name: "셋째 삭제" }).click()
  await expect(reminderButton(page, "셋째")).toHaveCount(0)
  await expectSidebarCount(page, "카운트", 1)
})
