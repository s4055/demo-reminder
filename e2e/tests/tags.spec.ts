import { expect, test } from "./support/fixtures"
import {
  expectSidebarCount,
  openEditDialog,
  pageTitle,
  reminderButton,
  reminderCheckboxes,
  reminderTitles,
  saveDialog,
  selectList,
  sidebar,
  sidebarItem,
} from "./support/ui"

test("addTagChip_showsTagOnReminderAndInSidebar", async ({ signedInPage: page, api }) => {
  const list = await api.createList({ name: "태그 리스트" })
  await api.createReminder({ title: "태그 붙일 일", listId: list.id })
  await page.reload()
  await selectList(page, "태그 리스트")
  await expect(sidebar(page).getByRole("heading", { name: "태그" })).toHaveCount(0)

  const dialog = await openEditDialog(page, "태그 붙일 일")
  const tagInput = dialog.getByLabel("태그", { exact: true })
  await tagInput.fill("업무")
  await tagInput.press("Enter")
  await expect(dialog.getByRole("button", { name: "업무 태그 제거" })).toBeVisible()
  await expect(tagInput).toHaveValue("")
  await saveDialog(dialog)

  await expect(reminderButton(page, "태그 붙일 일")).toContainText("#업무")
  await expect(sidebar(page).getByRole("heading", { name: "태그" })).toBeVisible()
  await expectSidebarCount(page, "업무", 1)
})

test("removeTagChip_removesTagFromReminderAndSidebar", async ({ signedInPage: page, api }) => {
  const list = await api.createList({ name: "태그 리스트" })
  await api.createReminder({ title: "태그 뗄 일", listId: list.id, tagNames: ["임시"] })
  await page.reload()
  await selectList(page, "태그 리스트")
  await expectSidebarCount(page, "임시", 1)

  const dialog = await openEditDialog(page, "태그 뗄 일")
  await dialog.getByRole("button", { name: "임시 태그 제거" }).click()
  await saveDialog(dialog)

  await expect(reminderButton(page, "태그 뗄 일")).not.toContainText("#임시")
  await expect(sidebarItem(page, "임시")).toHaveCount(0)
})

test("selectTag_showsOnlyTaggedReminders_matchingSidebarCount", async ({
  signedInPage: page,
  api,
}) => {
  const list = await api.createList({ name: "집안일" })
  await api.createReminder({ title: "청소", listId: list.id, tagNames: ["집"] })
  await api.createReminder({ title: "빨래", listId: list.id, tagNames: ["집", "주말"] })
  await api.createReminder({ title: "출근", listId: list.id })
  await page.reload()
  await expectSidebarCount(page, "집", 2)

  await sidebarItem(page, "집").click()

  await expect(pageTitle(page)).toHaveText("#집")
  await expect(reminderCheckboxes(page)).toHaveCount(2)
  expect((await reminderTitles(page)).toSorted()).toEqual(["빨래", "청소"])
})

test("deleteTag_removesItFromSidebar_andKeepsReminders", { tag: "@essential" }, async ({ signedInPage: page, api }) => {
  const list = await api.createList({ name: "집안일" })
  await api.createReminder({ title: "설거지", listId: list.id, tagNames: ["집"] })
  await page.reload()
  await selectList(page, "집안일")
  await expect(reminderButton(page, "설거지")).toContainText("#집")

  await sidebar(page).getByRole("button", { name: "집 태그 삭제" }).click()

  await expect(sidebarItem(page, "집")).toHaveCount(0)
  await expect(reminderButton(page, "설거지")).toBeVisible()
  await expect(reminderButton(page, "설거지")).not.toContainText("#집")
})
