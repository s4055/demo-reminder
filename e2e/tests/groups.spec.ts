import type { Page } from "@playwright/test"
import { expect, test } from "./support/fixtures"
import { reminderButton, selectList, sidebar, sidebarItem } from "./support/ui"

function groupToggle(page: Page, name: string) {
  return sidebar(page).getByRole("button", {
    name: new RegExp(`^${name} 리스트 \\d+개$`),
  })
}

function groupLists(page: Page, name: string) {
  return sidebar(page).getByRole("list", { name: `${name} 그룹의 리스트` })
}

async function moveListToGroup(page: Page, listName: string, groupLabel: string) {
  await sidebar(page).getByRole("button", { name: `${listName} 리스트 그룹으로 이동` }).click()
  await page
    .getByRole("list", { name: "이동할 그룹" })
    .getByRole("button", { name: groupLabel, exact: true })
    .click()
}

test("createGroup_andMoveListIntoIt_showsListUnderGroup", async ({ signedInPage: page, api }) => {
  await api.createList({ name: "독서" })
  await page.reload()

  await sidebar(page).getByRole("button", { name: "그룹 추가" }).click()
  const dialog = page.getByRole("dialog", { name: "새 그룹" })
  await dialog.getByLabel("이름").fill("취미")
  await dialog.getByRole("button", { name: "추가" }).click()
  await expect(dialog).toBeHidden()
  await expect(groupToggle(page, "취미")).toHaveAccessibleName("취미 리스트 0개")

  await moveListToGroup(page, "독서", "취미")

  await expect(groupLists(page, "취미").getByRole("button", { name: "독서 0" })).toBeVisible()
  await expect(groupToggle(page, "취미")).toHaveAccessibleName("취미 리스트 1개")
})

test("collapseGroup_hidesLists_andStaysCollapsedAfterReload", async ({
  signedInPage: page,
  api,
}) => {
  const group = await api.createGroup("회사")
  const list = await api.createList({ name: "회의" })
  await api.assignListToGroup(list.id, group.id)
  await page.reload()
  await expect(groupLists(page, "회사").getByRole("button", { name: "회의 0" })).toBeVisible()

  await groupToggle(page, "회사").click()
  await expect(groupToggle(page, "회사")).toHaveAttribute("aria-expanded", "false")
  await expect(groupLists(page, "회사")).toBeHidden()

  await page.reload()
  await expect(groupToggle(page, "회사")).toHaveAttribute("aria-expanded", "false")
  await expect(groupLists(page, "회사")).toBeHidden()

  await groupToggle(page, "회사").click()
  await expect(groupLists(page, "회사").getByRole("button", { name: "회의 0" })).toBeVisible()
})

test("renameAndDeleteGroup_keepsListsAndReminders", { tag: "@essential" }, async ({ signedInPage: page, api }) => {
  const group = await api.createGroup("임시")
  const list = await api.createList({ name: "보관" })
  await api.assignListToGroup(list.id, group.id)
  await api.createReminder({ title: "보관된 할일", listId: list.id })
  await page.reload()

  await sidebar(page).getByRole("button", { name: "임시 그룹 이름 변경" }).click()
  const renameDialog = page.getByRole("dialog", { name: "그룹 이름 변경" })
  await renameDialog.getByLabel("이름").fill("보관함")
  await renameDialog.getByRole("button", { name: "저장" }).click()
  await expect(renameDialog).toBeHidden()
  await expect(groupToggle(page, "보관함")).toHaveAccessibleName("보관함 리스트 1개")
  await expect(groupToggle(page, "임시")).toHaveCount(0)

  await sidebar(page).getByRole("button", { name: "보관함 그룹 삭제" }).click()
  const confirm = page.getByRole("alertdialog")
  await expect(confirm).toContainText("“보관함” 그룹을 삭제할까요?")
  await confirm.getByRole("button", { name: "삭제" }).click()
  await expect(confirm).toBeHidden()

  await expect(groupToggle(page, "보관함")).toHaveCount(0)
  await expect(sidebarItem(page, "보관")).toHaveAccessibleName("보관 1")
  await selectList(page, "보관")
  await expect(reminderButton(page, "보관된 할일")).toBeVisible()
})

test("removeListFromGroup_returnsItToUngroupedLists", async ({ signedInPage: page, api }) => {
  const group = await api.createGroup("가족")
  const list = await api.createList({ name: "생일" })
  await api.assignListToGroup(list.id, group.id)
  await page.reload()
  await expect(groupLists(page, "가족").getByRole("button", { name: "생일 0" })).toBeVisible()

  await moveListToGroup(page, "생일", "그룹 없음")

  await expect(groupToggle(page, "가족")).toHaveAccessibleName("가족 리스트 0개")
  await expect(groupLists(page, "가족").getByRole("button", { name: "생일 0" })).toHaveCount(0)
  await expect(sidebarItem(page, "생일")).toBeVisible()
})
