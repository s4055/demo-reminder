import {
  expect,
  test as base,
  type BrowserContext,
  type BrowserContextOptions,
  type Page,
} from "@playwright/test"
import { ApiClient, loginViaApi } from "./api"
import { newTestUser, type TestUser } from "./users"

type OpenSession = (user: TestUser, options?: BrowserContextOptions) => Promise<Page>

type Fixtures = {
  user: TestUser
  api: ApiClient
  signedInPage: Page
  openSession: OpenSession
}

async function gotoSignedIn(page: Page, url: string): Promise<void> {
  const refreshed = page.waitForResponse((response) =>
    response.url().endsWith("/api/auth/refresh")
  )
  await page.goto(url)
  expect((await refreshed).ok()).toBe(true)
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
    await gotoSignedIn(page, "/")
    await provide(page)
  },
  openSession: async ({ browser, baseURL, locale, timezoneId, viewport }, provide) => {
    const contexts: BrowserContext[] = []
    await provide(async (user, options = {}) => {
      const context = await browser.newContext({
        baseURL,
        locale,
        timezoneId,
        viewport,
        ...options,
      })
      contexts.push(context)
      await loginViaApi(context.request, user)
      const page = await context.newPage()
      await gotoSignedIn(page, "/")
      return page
    })
    await Promise.all(contexts.map((context) => context.close()))
  },
})

export { expect }
