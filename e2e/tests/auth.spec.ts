import type { Page } from "@playwright/test"
import { loginViaApi } from "./support/api"
import { expect, test } from "./support/fixtures"
import { expectSignedIn, loginViaUi, signupViaUi } from "./support/ui"
import { BACKEND_URL, uniqueEmail } from "./support/users"

const INVALID_CREDENTIALS = "이메일 또는 비밀번호가 올바르지 않습니다."
const EMAIL_TOO_LONG = "이메일은 100자 이하로 입력해 주세요."

function tooLongEmail(): string {
  return `${"a".repeat(90)}@example.com`
}

async function submitLogin(page: Page, email: string, password: string) {
  await page.getByLabel("이메일").fill(email)
  await page.getByLabel("비밀번호", { exact: true }).fill(password)
  await Promise.all([
    page.waitForResponse((response) => response.url().endsWith("/api/auth/login")),
    page.getByRole("button", { name: "로그인" }).click(),
  ])
}

function seoulTime(date: Date): string {
  return new Intl.DateTimeFormat("ko-KR", {
    timeZone: "Asia/Seoul",
    hour: "2-digit",
    minute: "2-digit",
    hourCycle: "h23",
  }).format(date)
}

test.describe("회원가입", () => {
  test("signup_movesToMainAndShowsEmail", { tag: "@essential" }, async ({ page, user }) => {
    await signupViaUi(page, user)

    await expectSignedIn(page, user.email)
  })

  test("signup_withInvalidInput_showsValidationMessages", async ({ page }) => {
    await page.goto("/signup")
    const submit = page.getByRole("button", { name: "가입하기" })
    const email = page.getByLabel("이메일")
    const password = page.getByLabel("비밀번호", { exact: true })
    const passwordConfirm = page.getByLabel("비밀번호 확인")

    await submit.click()
    await expect(page.getByText("이메일을 입력해 주세요.")).toBeVisible()
    await expect(page.getByText("이름을 입력해 주세요.")).toBeVisible()
    await expect(page.getByText("비밀번호를 입력해 주세요.")).toBeVisible()

    await email.fill("not-an-email")
    await submit.click()
    await expect(page.getByText("이메일 형식이 올바르지 않습니다.")).toBeVisible()

    await email.fill(tooLongEmail())
    await submit.click()
    await expect(page.getByText(EMAIL_TOO_LONG)).toBeVisible()

    await password.fill("short12")
    await submit.click()
    await expect(page.getByText("비밀번호는 8자 이상이어야 합니다.")).toBeVisible()

    await password.fill("가".repeat(25))
    await submit.click()
    await expect(page.getByText("비밀번호가 너무 깁니다.")).toBeVisible()

    await password.fill("password1234")
    await passwordConfirm.fill("password5678")
    await submit.click()
    await expect(page.getByText("비밀번호가 일치하지 않습니다.")).toBeVisible()

    await expect(page).toHaveURL("/signup")
  })

  test("signup_withRegisteredEmail_showsDuplicateMessage", async ({ page, api }) => {
    await signupViaUi(page, api.user)

    await expect(page.getByText("이미 가입한 이메일입니다.")).toBeVisible()
    await expect(page).toHaveURL("/signup")
  })
})

test.describe("로그인", () => {
  test("logout_thenLoginWithMixedCaseEmail_signsIn", { tag: "@essential" }, async ({ signedInPage: page, user }) => {
    await expect(page.getByText(user.email, { exact: true })).toBeVisible()

    await page.getByRole("button", { name: "로그아웃" }).click()
    await expect(page).toHaveURL("/login")

    await loginViaUi(page, { email: user.email.toUpperCase(), password: user.password })

    await expectSignedIn(page, user.email)
  })

  test("login_withWrongCredentials_showsErrorMessages", async ({ page, api }) => {
    await page.goto("/login")

    await submitLogin(page, api.user.email, "wrong-password")
    await expect(page.locator("form").getByRole("alert")).toHaveText(INVALID_CREDENTIALS)

    await submitLogin(page, uniqueEmail("missing"), api.user.password)
    await expect(page.locator("form").getByRole("alert")).toHaveText(INVALID_CREDENTIALS)

    await page.getByLabel("이메일").fill(tooLongEmail())
    await page.getByRole("button", { name: "로그인" }).click()
    await expect(page.getByText(EMAIL_TOO_LONG)).toBeVisible()
    await expect(page).toHaveURL("/login")
  })

  test("login_afterFiveFailures_blocksAndShowsRetryTime", async ({ page, api }) => {
    await page.goto("/login")
    for (let attempt = 0; attempt < 5; attempt++) {
      await submitLogin(page, api.user.email, "wrong-password")
      await expect(page.locator("form").getByRole("alert")).toHaveText(INVALID_CREDENTIALS)
    }

    await submitLogin(page, api.user.email, api.user.password)

    const alert = page.locator("form").getByRole("alert")
    await expect(alert).toHaveText(
      /^로그인 시도가 너무 많습니다\. \d{2}:\d{2} 이후에 다시 시도해 주세요\.$/
    )
    const retryAt = (await alert.textContent())?.match(/\d{2}:\d{2}/)?.[0]
    const now = Date.now()
    const expected = [14, 15, 16].map((minutes) => seoulTime(new Date(now + minutes * 60_000)))
    expect(expected).toContain(retryAt)
    await expect(page).toHaveURL("/login")
  })
})

test.describe("인증 상태", () => {
  test("rootWithoutLogin_redirectsToLogin", { tag: "@essential" }, async ({ page }) => {
    await page.goto("/")

    await expect(page).toHaveURL("/login")
  })

  test("loginPageWhenSignedIn_redirectsToMain", async ({ signedInPage: page, user }) => {
    await expect(page.getByText(user.email, { exact: true })).toBeVisible()

    await page.goto("/login")

    await expectSignedIn(page, user.email)
  })

  test("reload_keepsSession", { tag: "@essential" }, async ({ signedInPage: page, user }) => {
    await expect(page.getByText(user.email, { exact: true })).toBeVisible()

    await page.reload()

    await expectSignedIn(page, user.email)
  })

  test("reloadTwoTabsAtOnce_keepsBothSessions", { tag: "@essential" }, async ({ signedInPage: page, user }) => {
    const otherPage = await page.context().newPage()
    await otherPage.goto("/")
    await expectSignedIn(page, user.email)
    await expectSignedIn(otherPage, user.email)

    await Promise.all([page.reload(), otherPage.reload()])

    await expectSignedIn(page, user.email)
    await expectSignedIn(otherPage, user.email)
    await otherPage.reload()
    await expectSignedIn(otherPage, user.email)
  })

  test("addReminderAfterAccessTokenExpires_continuesRequest", { tag: "@essential" }, async ({
    signedInPage: page,
    user,
    request,
  }) => {
    test.setTimeout(120_000)
    await expect(page.getByText(user.email, { exact: true })).toBeVisible()
    const laterToken = await loginViaApi(request, user)
    await expect
      .poll(
        async () =>
          (
            await request.get(`${BACKEND_URL}/api/auth/me`, {
              headers: { Authorization: `Bearer ${laterToken}` },
            })
          ).status(),
        { timeout: 90_000, intervals: [2_000] }
      )
      .toBe(401)

    await page.getByLabel("리마인더 제목").fill("토큰 만료 뒤 추가")
    await page.getByRole("button", { name: "추가", exact: true }).click()

    await expect(page.getByRole("button", { name: "토큰 만료 뒤 추가 편집" })).toBeVisible()
    await expectSignedIn(page, user.email)
  })
})
