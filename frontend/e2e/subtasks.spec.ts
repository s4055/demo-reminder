import type { Page } from "@playwright/test"
import { expect, test } from "./support/fixtures"
import {
  completeCheckbox,
  expectSidebarCount,
  openEditDialog,
  reminderButton,
  saveDialog,
  selectList,
} from "./support/ui"

function subtaskList(page: Page, parent: string) {
  return page.locator("main").getByRole("list", { name: `${parent} 하위 작업` })
}

test("addSubtask_inEditDialog_showsIndentedAndCanCollapse", async ({
  signedInPage: page,
  api,
}) => {
  const list = await api.createList({ name: "프로젝트" })
  await api.createReminder({ title: "출시 준비", listId: list.id })
  await page.reload()
  await selectList(page, "프로젝트")

  const dialog = await openEditDialog(page, "출시 준비")
  const input = dialog.getByLabel("하위 작업")
  await input.fill("문서 작성")
  await input.press("Enter")
  await expect(dialog.getByText("문서 작성", { exact: true })).toBeVisible()
  await expect(input).toHaveValue("")
  await saveDialog(dialog)

  await expect(subtaskList(page, "출시 준비").getByRole("button", { name: "문서 작성 편집" })).toBeVisible()
  await expect(page.locator("main").getByText("0/1")).toBeVisible()

  await page.getByRole("button", { name: "출시 준비 하위 작업 접기" }).click()
  await expect(subtaskList(page, "출시 준비")).toBeHidden()

  await page.getByRole("button", { name: "출시 준비 하위 작업 펼치기" }).click()
  await expect(subtaskList(page, "출시 준비").getByRole("button", { name: "문서 작성 편집" })).toBeVisible()
})

test("subtask_canBeCompletedEditedAndDeletedOnItsOwn", async ({ signedInPage: page, api }) => {
  const list = await api.createList({ name: "프로젝트" })
  const parent = await api.createReminder({ title: "여행 계획", listId: list.id })
  await api.createReminder({ title: "항공권", parentId: parent.id })
  await api.createReminder({ title: "숙소", parentId: parent.id })
  await page.reload()
  await selectList(page, "프로젝트")

  await completeCheckbox(page, "항공권").click()
  await expect(completeCheckbox(page, "항공권")).toBeChecked()
  await expect(completeCheckbox(page, "여행 계획")).not.toBeChecked()
  await expect(page.locator("main").getByText("1/2")).toBeVisible()

  const dialog = await openEditDialog(page, "숙소")
  await expect(dialog.getByRole("heading", { name: "하위 작업 편집" })).toBeVisible()
  await dialog.getByLabel("제목").fill("호텔 예약")
  await saveDialog(dialog)
  await expect(subtaskList(page, "여행 계획").getByRole("button", { name: "호텔 예약 편집" })).toBeVisible()

  await page.locator("main").getByRole("button", { name: "항공권 삭제" }).click()
  await expect(reminderButton(page, "항공권")).toHaveCount(0)
  await expect(reminderButton(page, "여행 계획")).toBeVisible()
  await expect(page.locator("main").getByText("0/1")).toBeVisible()
})

test("completingOrDeletingParent_appliesToSubtasks", async ({ signedInPage: page, api }) => {
  const list = await api.createList({ name: "프로젝트" })
  const parent = await api.createReminder({ title: "이사", listId: list.id })
  await api.createReminder({ title: "짐 싸기", parentId: parent.id })
  await api.createReminder({ title: "전입 신고", parentId: parent.id })
  await page.reload()
  await selectList(page, "프로젝트")

  await completeCheckbox(page, "이사").click()
  await expect(completeCheckbox(page, "이사")).toBeChecked()
  await expect(completeCheckbox(page, "짐 싸기")).toBeChecked()
  await expect(completeCheckbox(page, "전입 신고")).toBeChecked()

  await page.locator("main").getByRole("button", { name: "이사 삭제" }).click()
  await expect(reminderButton(page, "이사")).toHaveCount(0)
  await expect(reminderButton(page, "짐 싸기")).toHaveCount(0)
  await expect(reminderButton(page, "전입 신고")).toHaveCount(0)
  await expect(page.getByText("이 리스트에 리마인더가 없습니다.")).toBeVisible()
})

test("listCount_includesOnlyTopLevelReminders", async ({ signedInPage: page, api }) => {
  const list = await api.createList({ name: "프로젝트" })
  const parent = await api.createReminder({ title: "발표", listId: list.id })
  await api.createReminder({ title: "슬라이드", parentId: parent.id })
  await api.createReminder({ title: "리허설", parentId: parent.id })
  await page.reload()

  await expectSidebarCount(page, "프로젝트", 1)
})
