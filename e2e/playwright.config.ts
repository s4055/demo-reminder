import { defineConfig, devices } from "@playwright/test"

const isCI = !!process.env.CI
const gradlew = process.platform === "win32" ? ".\\gradlew.bat" : "./gradlew"
const backendArgs = [
  "--server.port=8080",
  "--spring.datasource.url=jdbc:h2:mem:e2e",
  "--app.auth.login-attempt.max-failures-per-ip=1000",
  "--app.jwt.access-token-ttl=30s",
].join(" ")

export default defineConfig({
  testDir: "./tests",
  fullyParallel: true,
  forbidOnly: isCI,
  retries: isCI ? 1 : 0,
  workers: isCI ? 2 : undefined,
  reporter: [["list"], ["html", { open: "never" }]],
  expect: { timeout: 10_000 },
  use: {
    baseURL: "http://localhost:3000",
    locale: "ko-KR",
    timezoneId: "Asia/Seoul",
    screenshot: "only-on-failure",
    trace: "retain-on-failure",
  },
  projects: [
    {
      name: "chromium",
      use: { ...devices["Desktop Chrome"] },
    },
  ],
  webServer: [
    {
      command: `${gradlew} bootRun --args="${backendArgs}"`,
      cwd: "../backend",
      url: "http://localhost:8080/api/auth/me",
      reuseExistingServer: !isCI,
      timeout: 300_000,
      stdout: "ignore",
      stderr: "pipe",
    },
    {
      command: isCI ? "npm run build && npm run start" : "npm run dev",
      cwd: "../frontend",
      url: "http://localhost:3000/login",
      reuseExistingServer: !isCI,
      timeout: 300_000,
      stdout: "ignore",
      stderr: "pipe",
    },
  ],
})
