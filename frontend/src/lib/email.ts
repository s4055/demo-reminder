export const EMAIL_MAX_LENGTH = 100

export const EMAIL_TOO_LONG_MESSAGE = `이메일은 ${EMAIL_MAX_LENGTH}자 이하로 입력해 주세요.`

export function isEmailTooLong(email: string): boolean {
  return email.trim().length > EMAIL_MAX_LENGTH
}
