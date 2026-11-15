import { describe, expect, it } from "vitest"
import { EMAIL_MAX_LENGTH, isEmailTooLong } from "@/lib/email"

const emailOfLength = (length: number) => "a".repeat(length - "@example.com".length) + "@example.com"

describe("isEmailTooLong", () => {
  it("allowsEmailAtMaxLength", () => {
    expect(isEmailTooLong(emailOfLength(EMAIL_MAX_LENGTH))).toBe(false)
  })

  it("rejectsEmailOverMaxLength", () => {
    expect(isEmailTooLong(emailOfLength(EMAIL_MAX_LENGTH + 1))).toBe(true)
  })

  it("ignoresSurroundingWhitespace", () => {
    expect(isEmailTooLong(`  ${emailOfLength(EMAIL_MAX_LENGTH)}  `)).toBe(false)
  })
})
