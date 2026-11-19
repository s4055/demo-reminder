import type { Page } from "@playwright/test"
import { expect, test } from "./support/fixtures"
import {
  pageTitle,
  reminderCheckboxes,
  reminderItem,
  reminderTitles,
  selectList,
} from "./support/ui"

function searchBox(page: Page) {
  return page.getByRole("searchbox", { name: "리마인더 검색" })
}

test("search_matchesTitleMemoAndTag_withListName", async ({ signedInPage: page, api }) => {
  const list = await api.createList({ name: "장보기" })
  await api.createReminder({ title: "우유 사기", listId: list.id })
  await api.createReminder({ title: "마트 가기", memo: "우유 2개", listId: list.id })
  await api.createReminder({ title: "냉장고 확인", tagNames: ["우유"], listId: list.id })
  await api.createReminder({ title: "빵 사기", listId: list.id })
  await page.reload()

  await searchBox(page).fill("우유")

  await expect(pageTitle(page)).toHaveText("“우유” 검색 결과")
  await expect(reminderCheckboxes(page)).toHaveCount(3)
  expect((await reminderTitles(page)).toSorted()).toEqual(
    ["냉장고 확인", "마트 가기", "우유 사기"].toSorted()
  )
  for (const title of ["우유 사기", "마트 가기", "냉장고 확인"]) {
    await expect(reminderItem(page, title)).toContainText("장보기")
  }
})

test("search_withoutMatches_showsNoResultMessage", async ({ signedInPage: page, api }) => {
  await api.createReminder({ title: "아무 일" })
  await page.reload()

  await searchBox(page).fill("없는검색어")

  await expect(page.getByText("“없는검색어”와(과) 일치하는 리마인더가 없습니다.")).toBeVisible()
})

test("clearSearch_returnsToPreviousView", async ({ signedInPage: page, api }) => {
  const list = await api.createList({ name: "이전 화면" })
  await api.createReminder({ title: "검색 대상", listId: list.id })
  await page.reload()
  await selectList(page, "이전 화면")

  await searchBox(page).fill("검색")
  await expect(pageTitle(page)).toHaveText("“검색” 검색 결과")

  await page.getByRole("button", { name: "검색어 지우기" }).click()

  await expect(pageTitle(page)).toHaveText("이전 화면")
  await expect(searchBox(page)).toHaveValue("")
})

test("search_loadsFirst50_thenAllOnScroll", async ({ signedInPage: page, api }) => {
  test.setTimeout(120_000)
  const list = await api.createList({ name: "많은 결과" })
  const titles = Array.from({ length: 60 }, (_, index) => `찾기 ${String(index + 1).padStart(2, "0")}`)
  for (const title of titles) {
    await api.createReminder({ title, listId: list.id })
  }
  await page.reload()

  await searchBox(page).fill("찾기")
  await expect(reminderCheckboxes(page)).toHaveCount(50)
  expect(await reminderTitles(page)).toEqual(titles.slice(0, 50))

  await page.getByLabel("리마인더 더 불러오기").scrollIntoViewIfNeeded()
  await expect(reminderCheckboxes(page)).toHaveCount(60)
  expect(await reminderTitles(page)).toEqual(titles)
})
