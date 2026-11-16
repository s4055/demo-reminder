import { expect, type Locator, type Page } from "@playwright/test"
import type { CalendarDate } from "./dates"
import type { TestUser } from "./users"

export async function loginViaUi(
  page: Page,
  user: Pick<TestUser, "email" | "password">
): Promise<void> {
  await page.goto("/login")
  await page.getByLabel("이메일").fill(user.email)
  await page.getByLabel("비밀번호", { exact: true }).fill(user.password)
  await page.getByRole("button", { name: "로그인" }).click()
}

export async function signupViaUi(page: Page, user: TestUser): Promise<void> {
  await page.goto("/signup")
  await page.getByLabel("이메일").fill(user.email)
  await page.getByLabel("이름").fill(user.name)
  await page.getByLabel("비밀번호", { exact: true }).fill(user.password)
  await page.getByLabel("비밀번호 확인").fill(user.password)
  await page.getByRole("button", { name: "가입하기" }).click()
}

export async function expectSignedIn(page: Page, email: string): Promise<void> {
  await expect(page).toHaveURL("/")
  await expect(page.getByText(email, { exact: true })).toBeVisible()
}

export function sidebar(page: Page): Locator {
  return page.locator("aside")
}

function escapeRegExp(text: string): string {
  return text.replace(/[.*+?^${}()|[\]\\]/g, "\\$&")
}

export function sidebarItem(page: Page, name: string): Locator {
  return sidebar(page).getByRole("button", {
    name: new RegExp(`^${escapeRegExp(name)} \\d+$`),
  })
}

export async function expectSidebarCount(page: Page, name: string, count: number): Promise<void> {
  await expect(sidebarItem(page, name)).toHaveAccessibleName(`${name} ${count}`)
}

export function pageTitle(page: Page): Locator {
  return page.getByRole("heading", { level: 1 })
}

export async function selectList(page: Page, name: string): Promise<void> {
  await sidebarItem(page, name).click()
  await expect(pageTitle(page)).toHaveText(name)
}

export async function selectSmartView(page: Page, label: string): Promise<void> {
  await sidebar(page)
    .getByRole("navigation")
    .getByRole("button", { name: label, exact: true })
    .click()
  await expect(pageTitle(page)).toHaveText(label)
}

export function reminderButton(page: Page, title: string): Locator {
  return page.locator("main").getByRole("button", { name: `${title} 편집`, exact: true })
}

export function reminderItem(page: Page, title: string): Locator {
  return page
    .locator("main li")
    .filter({ has: page.getByRole("button", { name: `${title} 편집`, exact: true }) })
    .last()
}

export function completeCheckbox(page: Page, title: string): Locator {
  return page.locator("main").getByRole("checkbox", { name: `${title} 완료 처리`, exact: true })
}

export function reminderCheckboxes(page: Page): Locator {
  return page.locator("main").getByRole("checkbox")
}

export async function reminderTitles(page: Page): Promise<string[]> {
  const labels = await reminderCheckboxes(page).evaluateAll((elements) =>
    elements.map((element) => element.getAttribute("aria-label") ?? "")
  )
  return labels.map((label) => label.replace(/ 완료 처리$/, ""))
}

export async function addReminder(page: Page, title: string): Promise<void> {
  await page.getByLabel("리마인더 제목").fill(title)
  await page.locator("main").getByRole("button", { name: "추가", exact: true }).click()
  await expect(reminderButton(page, title)).toBeVisible()
}

export async function openEditDialog(page: Page, title: string): Promise<Locator> {
  await reminderButton(page, title).click()
  const dialog = page.getByRole("dialog")
  await expect(dialog).toBeVisible()
  return dialog
}

export async function saveDialog(dialog: Locator): Promise<void> {
  await dialog.getByRole("button", { name: "저장" }).click()
  await expect(dialog).toBeHidden()
}

export async function pickDueDate(
  page: Page,
  trigger: Locator,
  date: CalendarDate,
  time?: string
): Promise<void> {
  await trigger.click()
  const grid = page.getByRole("grid")
  await grid
    .getByRole("button", { name: new RegExp(`${date.year}년 ${date.month}월 ${date.day}일 `) })
    .click()
  if (time) await page.getByRole("textbox", { name: "시간" }).fill(time)
  await page.keyboard.press("Escape")
  await expect(grid).toBeHidden()
}

export async function chooseOption(page: Page, combobox: Locator, option: string): Promise<void> {
  await combobox.click()
  await page.getByRole("option", { name: option, exact: true }).click()
  await expect(page.getByRole("listbox")).toBeHidden()
}

export async function dragWithKeyboard(
  page: Page,
  handle: Locator,
  key: "ArrowUp" | "ArrowDown",
  times: number
): Promise<void> {
  const announcement = page.getByRole("status").filter({ hasText: /was moved over/ })
  await handle.focus()
  await page.keyboard.press("Space")
  await expect(handle).toHaveAttribute("aria-pressed", "true")
  for (let step = 0; step < times; step++) {
    const before = (await announcement.allTextContents()).join()
    await page.keyboard.press(key)
    await expect.poll(async () => (await announcement.allTextContents()).join()).not.toBe(before)
  }
  await page.keyboard.press("Space")
  await expect(handle).not.toHaveAttribute("aria-pressed", "true")
}
