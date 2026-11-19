import { expect, test } from "./support/fixtures"
import {
  expectSidebarCount,
  openShareDialog,
  pageTitle,
  reminderCheckboxes,
  reminderTitles,
  sidebarItem,
  SYNC_TIMEOUT,
} from "./support/ui"

test.describe.configure({ timeout: 90_000 })

test("sameNameTag_fromAnotherMember_countMatchesTagView", async ({
  signedInPage: page,
  api,
  memberApi,
}) => {
  const personal = await api.createList({ name: "개인" })
  await api.createReminder({ title: "내가 태그한 일", listId: personal.id, tagNames: ["공통"] })
  const shared = await api.shareList({ name: "공유 태그" }, memberApi)
  await memberApi.createReminder({ title: "멤버가 태그한 일", listId: shared.id, tagNames: ["공통"] })
  await page.reload()

  await expectSidebarCount(page, "공통", 2)
  await sidebarItem(page, "공통").click()

  await expect(pageTitle(page)).toHaveText("#공통")
  await expect(reminderCheckboxes(page)).toHaveCount(2)
  expect((await reminderTitles(page)).toSorted()).toEqual(["내가 태그한 일", "멤버가 태그한 일"])
})

test("removedMember_tagCountAndTagView_excludeSharedListReminders", async ({
  signedInPage: page,
  api,
  memberApi,
  openSession,
}) => {
  const personal = await memberApi.createList({ name: "멤버 개인" })
  await memberApi.createReminder({ title: "멤버 개인 일", listId: personal.id, tagNames: ["공통"] })
  const shared = await api.shareList({ name: "제거될 공유" }, memberApi)
  await api.createReminder({ title: "소유자 공유 일", listId: shared.id, tagNames: ["공통"] })
  await page.reload()

  const memberPage = await openSession(memberApi.user)
  await expectSidebarCount(memberPage, "공통", 2)
  await sidebarItem(memberPage, "공통").click()
  await expect(reminderCheckboxes(memberPage)).toHaveCount(2)

  const dialog = await openShareDialog(page, "제거될 공유")
  await dialog.getByRole("button", { name: "공유 멤버 멤버 제거" }).click()
  await expect(dialog.getByRole("region", { name: "멤버" }).getByRole("heading")).toHaveText(
    "멤버 1명"
  )

  await expectSidebarCount(memberPage, "공통", 1, { timeout: SYNC_TIMEOUT })
  await expect(reminderCheckboxes(memberPage)).toHaveCount(1, { timeout: SYNC_TIMEOUT })
  expect(await reminderTitles(memberPage)).toEqual(["멤버 개인 일"])
})
