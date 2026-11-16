import { expect, type Page } from "@playwright/test"
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
