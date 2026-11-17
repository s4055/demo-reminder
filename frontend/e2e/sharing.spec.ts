import type { Page } from "@playwright/test"
import { expect, test } from "./support/fixtures"
import {
  addReminder,
  completeCheckbox,
  expectSidebarCount,
  openEditDialog,
  openShareDialog,
  reminderButton,
  saveDialog,
  selectList,
  sharedListSection,
  sidebar,
  SYNC_TIMEOUT,
} from "./support/ui"

test.describe.configure({ timeout: 90_000 })

function mixedCase(email: string): string {
  return email.replace(/^e2e/, "E2e").replace("example.com", "Example.COM")
}

function sharedIcon(page: Page, memberCount: number) {
  return sidebar(page).getByLabel(`공유된 리스트 (멤버 ${memberCount}명)`)
}

test("inviteMember_withMixedCaseEmail_addsEditorToMembers", async ({
  signedInPage: page,
  api,
  memberApi,
}) => {
  await api.createList({ name: "가족 일정" })
  await page.reload()

  const dialog = await openShareDialog(page, "가족 일정")
  await dialog.getByLabel("이메일로 초대").fill(mixedCase(memberApi.user.email))
  await dialog.getByRole("button", { name: "초대" }).click()

  await expect(dialog.getByText("공유 멤버님을 초대했습니다.")).toBeVisible()
  await expect(dialog.getByLabel("이메일로 초대")).toHaveValue("")
  const members = dialog.getByRole("region", { name: "멤버" })
  await expect(members.getByRole("heading")).toHaveText("멤버 2명")
  const invited = members.getByRole("listitem").filter({ hasText: memberApi.user.email })
  await expect(invited).toContainText("공유 멤버")
  await expect(invited).toContainText("편집자")
  await expect(members.getByRole("listitem").filter({ hasText: api.user.email })).toContainText(
    "소유자"
  )
})

test("inviteMember_withInvalidTargets_showsMatchingMessages", async ({
  signedInPage: page,
  api,
  memberApi,
}) => {
  await api.shareList({ name: "초대 확인" }, memberApi)
  await page.reload()
  const dialog = await openShareDialog(page, "초대 확인")
  const emailInput = dialog.getByLabel("이메일로 초대")
  const inviteButton = dialog.getByRole("button", { name: "초대" })

  await emailInput.fill("nobody-" + memberApi.user.email)
  await inviteButton.click()
  await expect(dialog.getByText("해당 이메일로 가입한 사용자가 없습니다.")).toBeVisible()

  await emailInput.fill(mixedCase(memberApi.user.email))
  await inviteButton.click()
  await expect(dialog.getByText("이미 이 리스트의 멤버입니다.")).toBeVisible()

  const inviteRequests: string[] = []
  page.on("request", (request) => {
    if (request.method() === "POST" && request.url().endsWith("/members")) {
      inviteRequests.push(request.url())
    }
  })
  await emailInput.fill(`${"a".repeat(89)}@example.com`)
  await inviteButton.click()
  await expect(dialog.getByText("이메일은 100자 이하로 입력해 주세요.")).toBeVisible()
  expect(inviteRequests).toHaveLength(0)
  await expect(dialog.getByRole("region", { name: "멤버" }).getByRole("heading")).toHaveText(
    "멤버 2명"
  )
})

test("sharedList_showsInMemberSidebar_withSharedIconAndMemberCount", async ({
  signedInPage: page,
  api,
  memberApi,
  openSession,
}) => {
  await api.createList({ name: "내 리스트" })
  const shared = await api.shareList({ name: "함께 쓰는 리스트" }, memberApi)
  await api.createReminder({ title: "공유된 할 일", listId: shared.id })
  await page.reload()

  await expect(sharedIcon(page, 2)).toHaveCount(1)
  await expect(
    sidebar(page).getByRole("listitem").filter({ hasText: "함께 쓰는 리스트" }).getByLabel(
      "공유된 리스트 (멤버 2명)"
    )
  ).toBeVisible()

  const memberPage = await openSession(memberApi.user)
  const section = sharedListSection(memberPage)
  await expect(section.getByRole("heading", { name: "공유받은 리스트" })).toBeVisible()
  const item = section.getByRole("listitem").filter({ hasText: "함께 쓰는 리스트" })
  await expect(item.getByLabel("공유된 리스트 (멤버 2명)")).toBeVisible()
  await expectSidebarCount(memberPage, "함께 쓰는 리스트", 1)
  await expect(sidebar(memberPage).getByText("내 리스트")).toHaveCount(0)

  await selectList(memberPage, "함께 쓰는 리스트")
  await expect(reminderButton(memberPage, "공유된 할 일")).toBeVisible()
})

test("editor_managesRemindersInSharedList", async ({ api, memberApi, openSession }) => {
  await api.shareList({ name: "공동 작업" }, memberApi)
  const memberPage = await openSession(memberApi.user)
  await selectList(memberPage, "공동 작업")

  await addReminder(memberPage, "같이 할 일")
  const dialog = await openEditDialog(memberPage, "같이 할 일")
  await dialog.getByLabel("제목").fill("고친 할 일")
  await saveDialog(dialog)
  await expect(reminderButton(memberPage, "고친 할 일")).toBeVisible()

  const main = memberPage.locator("main")
  await main.getByRole("button", { name: "고친 할 일 플래그 지정" }).click()
  await expect(main.getByRole("button", { name: "고친 할 일 플래그 해제" })).toHaveAttribute(
    "aria-pressed",
    "true"
  )

  await completeCheckbox(memberPage, "고친 할 일").click()
  await expect(completeCheckbox(memberPage, "고친 할 일")).toBeChecked()

  await main.getByRole("button", { name: "고친 할 일 삭제" }).click()
  await expect(reminderButton(memberPage, "고친 할 일")).toHaveCount(0)
})

test("editor_hasNoListEditDeleteOrInvite_andSeesMembersOnly", async ({
  api,
  memberApi,
  openSession,
}) => {
  await api.shareList({ name: "보기 전용 관리" }, memberApi)
  const memberPage = await openSession(memberApi.user)
  await selectList(memberPage, "보기 전용 관리")

  const memberSidebar = sidebar(memberPage)
  await expect(memberSidebar.getByRole("button", { name: "보기 전용 관리 리스트 편집" })).toHaveCount(0)
  await expect(memberSidebar.getByRole("button", { name: "보기 전용 관리 리스트 삭제" })).toHaveCount(0)
  await expect(
    memberSidebar.getByRole("button", { name: "보기 전용 관리 리스트 공유" })
  ).toHaveAttribute("title", "멤버 보기")

  const dialog = await openShareDialog(memberPage, "보기 전용 관리")
  await expect(dialog.getByText("테스트 사용자님이 공유한 리스트입니다.", { exact: false })).toBeVisible()
  await expect(dialog.getByLabel("이메일로 초대")).toHaveCount(0)
  await expect(dialog.getByRole("button", { name: "초대" })).toHaveCount(0)
  await expect(dialog.getByRole("button", { name: /멤버 제거$/ })).toHaveCount(0)
  const members = dialog.getByRole("region", { name: "멤버" })
  await expect(members.getByRole("heading")).toHaveText("멤버 2명")
  await expect(members.getByRole("listitem").filter({ hasText: "(나)" })).toContainText("편집자")
  await expect(dialog.getByRole("button", { name: "리스트 나가기" })).toBeVisible()
})

test("editor_leavesList_afterConfirm_removesItFromSidebar", async ({
  api,
  memberApi,
  openSession,
}) => {
  await api.shareList({ name: "나갈 리스트" }, memberApi)
  const memberPage = await openSession(memberApi.user)
  await selectList(memberPage, "나갈 리스트")

  const dialog = await openShareDialog(memberPage, "나갈 리스트")
  await dialog.getByRole("button", { name: "리스트 나가기" }).click()
  await expect(dialog.getByText("나가면 이 리스트와 리마인더가 더 이상 보이지 않습니다.")).toBeVisible()
  await dialog.getByRole("button", { name: "나가기", exact: true }).click()

  await expect(dialog).toBeHidden()
  await expect(sidebar(memberPage).getByText("나갈 리스트")).toHaveCount(0)
  await expect(sharedListSection(memberPage)).toHaveCount(0)
  await memberPage.reload()
  await expect(sidebar(memberPage).getByText("나갈 리스트")).toHaveCount(0)
})

test("owner_removesMember_listDisappearsFromMemberScreen", async ({
  signedInPage: page,
  api,
  memberApi,
  openSession,
}) => {
  await api.shareList({ name: "정리할 공유" }, memberApi)
  await page.reload()
  const memberPage = await openSession(memberApi.user)
  await selectList(memberPage, "정리할 공유")

  const dialog = await openShareDialog(page, "정리할 공유")
  await dialog.getByRole("button", { name: "공유 멤버 멤버 제거" }).click()
  const members = dialog.getByRole("region", { name: "멤버" })
  await expect(members.getByRole("heading")).toHaveText("멤버 1명")
  await expect(members.getByText(memberApi.user.email)).toHaveCount(0)
  await page.keyboard.press("Escape")
  await expect(sharedIcon(page, 2)).toHaveCount(0)

  await expect(sidebar(memberPage).getByText("정리할 공유")).toHaveCount(0, {
    timeout: SYNC_TIMEOUT,
  })
  await expect(sharedListSection(memberPage)).toHaveCount(0)
})
