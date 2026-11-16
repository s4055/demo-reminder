import { test as base, type Browser, type BrowserContext, type Page } from "@playwright/test"
import { ApiClient, loginViaApi, signupViaApi } from "./api"
import { newTestUser, type TestUser } from "./users"

export type UserSession = {
  user: TestUser
  context: BrowserContext
  page: Page
}

export async function newUserSession(
  browser: Browser,
  user: TestUser = newTestUser()
): Promise<UserSession> {
  const context = await browser.newContext()
  await signupViaApi(context.request, user)
  const page = await context.newPage()
  return { user, context, page }
}

type Fixtures = {
  user: TestUser
  api: ApiClient
  signedInPage: Page
}

export const test = base.extend<Fixtures>({
  user: async ({}, provide) => {
    await provide(newTestUser())
  },
  api: async ({ user }, provide) => {
    const api = await ApiClient.signup(user)
    await provide(api)
    await api.dispose()
  },
  signedInPage: async ({ api, page }, provide) => {
    await loginViaApi(page.context().request, api.user)
    await page.goto("/")
    await provide(page)
  },
})

export { expect } from "@playwright/test"
