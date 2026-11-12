import { addSeconds, format } from "date-fns"
import { ApiError } from "@/lib/api"

export function loginErrorMessage(error: unknown, now: Date = new Date()): string {
  if (!(error instanceof ApiError)) {
    return "로그인하지 못했습니다. 잠시 후 다시 시도해 주세요."
  }
  if (error.status === 401) return "이메일 또는 비밀번호가 올바르지 않습니다."
  if (error.status === 429) {
    if (error.retryAfterSeconds === undefined) {
      return "로그인 시도가 너무 많습니다. 잠시 후 다시 시도해 주세요."
    }
    const retryAt = format(addSeconds(now, error.retryAfterSeconds), "HH:mm")
    return `로그인 시도가 너무 많습니다. ${retryAt} 이후에 다시 시도해 주세요.`
  }
  return "로그인하지 못했습니다. 잠시 후 다시 시도해 주세요."
}
