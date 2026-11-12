import { describe, expect, it } from "vitest"
import { ApiError } from "@/lib/api"
import { loginErrorMessage } from "@/lib/login-error"

describe("loginErrorMessage", () => {
  const now = new Date(2026, 10, 12, 20, 50, 10)

  it("showsRetryTimeForTooManyAttempts", () => {
    const error = new ApiError("x", 429, "TOO_MANY_LOGIN_ATTEMPTS", 900)

    expect(loginErrorMessage(error, now)).toBe("로그인 시도가 너무 많습니다. 21:05 이후에 다시 시도해 주세요.")
  })

  it("fallsBackWhenRetryTimeUnknown", () => {
    const error = new ApiError("x", 429, "TOO_MANY_LOGIN_ATTEMPTS")

    expect(loginErrorMessage(error, now)).toBe("로그인 시도가 너무 많습니다. 잠시 후 다시 시도해 주세요.")
  })

  it("explainsInvalidCredentials", () => {
    expect(loginErrorMessage(new ApiError("x", 401, "UNAUTHORIZED"), now)).toBe(
      "이메일 또는 비밀번호가 올바르지 않습니다."
    )
  })

  it("usesGenericMessageForOtherErrors", () => {
    expect(loginErrorMessage(new Error("network"), now)).toBe("로그인하지 못했습니다. 잠시 후 다시 시도해 주세요.")
  })
})
