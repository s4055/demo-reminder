import type { Locator, Page } from "@playwright/test"
import { expect, test } from "./support/fixtures"
import {
  dragWithKeyboard,
  reminderTitles,
  selectList,
  selectSmartView,
  sidebar,
} from "./support/ui"

async function labelsOf(locator: Locator, suffix: string): Promise<string[]> {
  const labels = await locator.evaluateAll((elements) =>
    elements.map((element) => element.getAttribute("aria-label") ?? "")
  )
  return labels.map((label) => label.replace(suffix, ""))
}

function listHandles(scope: Locator) {
  return scope.getByRole("button", { name: / 리스트 순서 이동$/ })
}

async function ungroupedListNames(page: Page): Promise<string[]> {
  const labels = await listHandles(sidebar(page)).evaluateAll((elements) =>
    elements
      .filter((element) => !element.closest('ul[aria-label$="그룹의 리스트"]'))
      .map((element) => element.getAttribute("aria-label") ?? "")
  )
  return labels.map((label) => label.replace(" 리스트 순서 이동", ""))
}

function groupHandles(page: Page) {
  return sidebar(page).getByRole("button", { name: / 그룹 순서 이동$/ })
}

function groupLists(page: Page, name: string) {
  return sidebar(page).getByRole("list", { name: `${name} 그룹의 리스트` })
}

test("reorderReminders_withKeyboard_persistsAfterReload", async ({ signedInPage: page, api }) => {
  const list = await api.createList({ name: "순서 리스트" })
  for (const title of ["하나", "둘", "셋"]) {
    await api.createReminder({ title, listId: list.id })
  }
  await page.reload()
  await selectList(page, "순서 리스트")
  await expect.poll(() => reminderTitles(page)).toEqual(["하나", "둘", "셋"])

  await dragWithKeyboard(page, page.getByRole("button", { name: "하나 순서 이동" }), "ArrowDown", 2)

  await expect.poll(() => reminderTitles(page)).toEqual(["둘", "셋", "하나"])
  await page.reload()
  await selectList(page, "순서 리스트")
  await expect.poll(() => reminderTitles(page)).toEqual(["둘", "셋", "하나"])
})

test("reorderSidebar_listsGroupsAndListsInGroup_persistAfterReload", async ({
  signedInPage: page,
  api,
}) => {
  for (const name of ["가", "나", "다"]) {
    await api.createList({ name })
  }
  const first = await api.createGroup("첫 그룹")
  await api.createGroup("둘째 그룹")
  for (const name of ["안1", "안2"]) {
    const list = await api.createList({ name })
    await api.assignListToGroup(list.id, first.id)
  }
  await page.reload()

  const ungrouped = () => ungroupedListNames(page)
  const groups = () => labelsOf(groupHandles(page), " 그룹 순서 이동")
  const inGroup = () => labelsOf(listHandles(groupLists(page, "첫 그룹")), " 리스트 순서 이동")
  await expect.poll(ungrouped).toEqual(["가", "나", "다"])
  await expect.poll(groups).toEqual(["첫 그룹", "둘째 그룹"])
  await expect.poll(inGroup).toEqual(["안1", "안2"])

  await dragWithKeyboard(page, sidebar(page).getByRole("button", { name: "가 리스트 순서 이동" }), "ArrowDown", 1)
  await expect.poll(ungrouped).toEqual(["나", "가", "다"])

  await dragWithKeyboard(page, sidebar(page).getByRole("button", { name: "둘째 그룹 그룹 순서 이동" }), "ArrowUp", 1)
  await expect.poll(groups).toEqual(["둘째 그룹", "첫 그룹"])

  await dragWithKeyboard(page, sidebar(page).getByRole("button", { name: "안2 리스트 순서 이동" }), "ArrowUp", 1)
  await expect.poll(inGroup).toEqual(["안2", "안1"])

  await page.reload()
  await expect.poll(ungrouped).toEqual(["나", "가", "다"])
  await expect.poll(groups).toEqual(["둘째 그룹", "첫 그룹"])
  await expect.poll(inGroup).toEqual(["안2", "안1"])
})

test("smartViewAndSearch_haveNoReorderHandles", async ({ signedInPage: page, api }) => {
  const list = await api.createList({ name: "손잡이 리스트" })
  await api.createReminder({ title: "손잡이 확인", listId: list.id })
  await page.reload()
  const main = page.locator("main")
  const handles = main.getByRole("button", { name: /순서 이동$/ })

  await selectList(page, "손잡이 리스트")
  await expect(handles).toHaveCount(1)

  await selectSmartView(page, "전체")
  await expect(main.getByRole("checkbox", { name: "손잡이 확인 완료 처리" })).toBeVisible()
  await expect(handles).toHaveCount(0)

  await page.getByRole("searchbox", { name: "리마인더 검색" }).fill("손잡이")
  await expect(page.getByRole("heading", { level: 1 })).toHaveText("“손잡이” 검색 결과")
  await expect(main.getByRole("checkbox", { name: "손잡이 확인 완료 처리" })).toBeVisible()
  await expect(handles).toHaveCount(0)
})
