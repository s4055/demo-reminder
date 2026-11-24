import {
  ErrorsInstrumentation,
  SessionInstrumentation,
  WebVitalsInstrumentation,
  type BrowserConfig,
  type TransportItem,
} from "@grafana/faro-web-sdk"

export type FaroEnv = {
  url?: string
  appName?: string
  appVersion?: string
  environment?: string
}

const DEFAULT_APP_NAME = "reminder-frontend"
const REDACTED = "[REDACTED]"

const URL_QUERY_PATTERN = /\b(https?:\/\/[^\s?#"'<>]+)[?#][^\s"'<>]*/g
const BEARER_PATTERN = /\bBearer\s+[A-Za-z0-9\-._~+/]+=*/gi
const JWT_PATTERN = /\beyJ[A-Za-z0-9_-]*\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]*/g
const EMAIL_PATTERN = /[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}/g

export function createFaroConfig(env: FaroEnv): BrowserConfig | null {
  const url = env.url?.trim()
  if (!url) return null

  return {
    url,
    app: {
      name: env.appName || DEFAULT_APP_NAME,
      version: env.appVersion,
      environment: env.environment,
    },
    instrumentations: [
      new ErrorsInstrumentation(),
      new WebVitalsInstrumentation(),
      new SessionInstrumentation(),
    ],
    pageTracking: {
      generatePageId: (location) => location.pathname,
    },
    beforeSend: sanitizeTransportItem,
  }
}

export function sanitizeText(text: string): string {
  return text
    .replace(URL_QUERY_PATTERN, "$1")
    .replace(BEARER_PATTERN, `Bearer ${REDACTED}`)
    .replace(JWT_PATTERN, REDACTED)
    .replace(EMAIL_PATTERN, REDACTED)
}

export function sanitizeTransportItem(item: TransportItem): TransportItem {
  return sanitizeValue(item) as TransportItem
}

function sanitizeValue(value: unknown): unknown {
  if (typeof value === "string") return sanitizeText(value)
  if (Array.isArray(value)) return value.map(sanitizeValue)
  if (isPlainObject(value)) {
    return Object.fromEntries(
      Object.entries(value).map(([key, entry]) => [key, sanitizeValue(entry)])
    )
  }
  return value
}

function isPlainObject(value: unknown): value is Record<string, unknown> {
  if (value === null || typeof value !== "object") return false
  const prototype = Object.getPrototypeOf(value)
  return prototype === Object.prototype || prototype === null
}
