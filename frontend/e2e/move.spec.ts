import { expect, test } from "./support/fixtures"
import {
  chooseOption,
  expectSidebarCount,
  openEditDialog,
  reminderButton,
  saveDialog,
  selectList,
} from "./support/ui"

test("moveReminder_toOtherList_takesSubtasksAndUpdatesCounts", async ({
  signedInPage: page,
  api,
}) => {
  const from = await api.createList({ name: "출발" })
  await api.createList({ name: "도착" })
  const parent = await api.createReminder({ title: "옮길 일", listId: from.id })
  await api.createReminder({ title: "딸린 일 1", parentId: parent.id })
  await api.createReminder({ title: "딸린 일 2", parentId: parent.id })
  await api.createReminder({ title: "남을 일", listId: from.id })
  await page.reload()
  await selectList(page, "출발")
  await expectSidebarCount(page, "출발", 2)
  await expectSidebarCount(page, "도착", 0)

  const dialog = await openEditDialog(page, "옮길 일")
  await chooseOption(page, dialog.getByRole("combobox", { name: "리스트" }), "도착")
  await saveDialog(dialog)

  await expect(page.getByText("“도착”(으)로 이동했습니다.")).toBeVisible()
  await expectSidebarCount(page, "출발", 1)
  await expectSidebarCount(page, "도착", 1)
  await expect(reminderButton(page, "옮길 일")).toHaveCount(0)
  await expect(reminderButton(page, "딸린 일 1")).toHaveCount(0)

  await selectList(page, "도착")
  const subtasks = page.locator("main").getByRole("list", { name: "옮길 일 하위 작업" })
  await expect(reminderButton(page, "옮길 일")).toBeVisible()
  await expect(subtasks.getByRole("button", { name: "딸린 일 1 편집" })).toBeVisible()
  await expect(subtasks.getByRole("button", { name: "딸린 일 2 편집" })).toBeVisible()
})

test("subtaskEditDialog_hasNoListSelect", async ({ signedInPage: page, api }) => {
  const list = await api.createList({ name: "출발" })
  const parent = await api.createReminder({ title: "부모 일", listId: list.id })
  await api.createReminder({ title: "자식 일", parentId: parent.id })
  await page.reload()
  await selectList(page, "출발")

  const parentDialog = await openEditDialog(page, "부모 일")
  await expect(parentDialog.getByRole("combobox", { name: "리스트" })).toBeVisible()
  await page.keyboard.press("Escape")
  await expect(parentDialog).toBeHidden()

  const subtaskDialog = await openEditDialog(page, "자식 일")
  await expect(subtaskDialog.getByRole("heading", { name: "하위 작업 편집" })).toBeVisible()
  await expect(subtaskDialog.getByRole("combobox", { name: "리스트" })).toHaveCount(0)
})
