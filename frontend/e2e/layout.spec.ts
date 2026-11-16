import { expect, test } from "./support/fixtures"
import { pageTitle, reminderButton, sidebar } from "./support/ui"

test.use({ viewport: { width: 390, height: 844 } })

test("narrowScreen_menuOpensSidebar_andClosesAfterSelectingList", async ({
  signedInPage: page,
  api,
}) => {
  const list = await api.createList({ name: "모바일 리스트" })
  await api.createReminder({ title: "모바일 할일", listId: list.id })
  await page.reload()
  await expect(sidebar(page)).toBeHidden()

  await page.getByRole("button", { name: "메뉴 열기" }).click()
  const menu = page.getByRole("dialog", { name: "메뉴" })
  await expect(menu).toBeVisible()
  await menu.getByRole("button", { name: "모바일 리스트 1" }).click()

  await expect(menu).toBeHidden()
  await expect(pageTitle(page)).toHaveText("모바일 리스트")
  await expect(reminderButton(page, "모바일 할일")).toBeVisible()
})
