import { expect, test } from "./support/fixtures"
import { openEditDialog, selectList, sidebar } from "./support/ui"

test.describe.configure({ timeout: 90_000 })

const CONFLICT_MESSAGE = "다른 사용자가 먼저 수정했습니다. 최신 내용을 불러왔습니다."

test("saveReminder_afterAnotherMemberSaved_showsConflictAndLoadsLatest", { tag: "@essential" }, async ({
  signedInPage: page,
  api,
  memberApi,
  openSession,
}) => {
  const list = await api.shareList({ name: "충돌 리스트" }, memberApi)
  await api.createReminder({ title: "같이 고칠 일", listId: list.id })
  await page.reload()
  await selectList(page, "충돌 리스트")
  const memberPage = await openSession(memberApi.user)
  await selectList(memberPage, "충돌 리스트")

  const ownerDialog = await openEditDialog(page, "같이 고칠 일")
  const memberDialog = await openEditDialog(memberPage, "같이 고칠 일")

  await ownerDialog.getByLabel("제목").fill("소유자가 고친 제목")
  await ownerDialog.getByLabel("메모").fill("소유자 메모")
  await ownerDialog.getByRole("button", { name: "저장" }).click()
  await expect(ownerDialog).toBeHidden()

  await memberDialog.getByLabel("제목").fill("멤버가 고친 제목")
  await memberDialog.getByRole("button", { name: "저장" }).click()

  await expect(memberPage.getByText(CONFLICT_MESSAGE)).toBeVisible()
  await expect(memberDialog).toBeVisible()
  await expect(memberDialog.getByLabel("제목")).toHaveValue("소유자가 고친 제목")
  await expect(memberDialog.getByLabel("메모")).toHaveValue("소유자 메모")
  await expect(page.getByText(CONFLICT_MESSAGE)).toHaveCount(0)
})

test("saveList_afterAnotherSessionSaved_showsConflictAndLoadsLatest", { tag: "@essential" }, async ({
  signedInPage: page,
  api,
  openSession,
}) => {
  await api.createList({ name: "원래 이름" })
  await page.reload()
  const otherPage = await openSession(api.user)

  await sidebar(page).getByRole("button", { name: "원래 이름 리스트 편집" }).click()
  const firstDialog = page.getByRole("dialog", { name: "리스트 편집" })
  await sidebar(otherPage).getByRole("button", { name: "원래 이름 리스트 편집" }).click()
  const secondDialog = otherPage.getByRole("dialog", { name: "리스트 편집" })
  await expect(secondDialog).toBeVisible()

  await firstDialog.getByLabel("이름").fill("먼저 저장한 이름")
  await firstDialog.getByRole("button", { name: "저장" }).click()
  await expect(firstDialog).toBeHidden()

  await secondDialog.getByLabel("이름").fill("나중에 저장한 이름")
  await secondDialog.getByRole("button", { name: "저장" }).click()

  await expect(otherPage.getByText(CONFLICT_MESSAGE)).toBeVisible()
  await expect(secondDialog).toBeVisible()
  await expect(secondDialog.getByLabel("이름")).toHaveValue("먼저 저장한 이름")
})
