import { expect, test } from "./support/fixtures"
import {
  addReminder,
  completeCheckbox,
  expectSidebarCount,
  reminderButton,
  selectList,
  SYNC_TIMEOUT,
} from "./support/ui"

test.describe.configure({ timeout: 90_000 })

test("sharedListChanges_byAnotherMember_appearWithoutReload", async ({
  signedInPage: page,
  api,
  memberApi,
  openSession,
}) => {
  await api.shareList({ name: "실시간 리스트" }, memberApi)
  await page.reload()
  await selectList(page, "실시간 리스트")
  const memberPage = await openSession(memberApi.user)
  await selectList(memberPage, "실시간 리스트")

  await addReminder(memberPage, "멤버가 추가한 일")

  await expect
    .poll(() => reminderButton(page, "멤버가 추가한 일").count(), { timeout: SYNC_TIMEOUT })
    .toBe(1)
  await expectSidebarCount(page, "실시간 리스트", 1)

  await completeCheckbox(memberPage, "멤버가 추가한 일").click()
  await expect(completeCheckbox(memberPage, "멤버가 추가한 일")).toBeChecked()

  await expect
    .poll(() => completeCheckbox(page, "멤버가 추가한 일").isChecked(), {
      timeout: SYNC_TIMEOUT,
    })
    .toBe(true)
  await expect(page.getByText("완료된 항목 1개")).toBeVisible()
  await expectSidebarCount(page, "실시간 리스트", 0)
})
