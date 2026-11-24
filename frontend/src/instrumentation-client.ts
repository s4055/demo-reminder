import { initializeFaro } from "@grafana/faro-web-sdk"
import { createFaroConfig } from "@/lib/faro"

const faroConfig = createFaroConfig({
  url: process.env.NEXT_PUBLIC_FARO_URL,
  appName: process.env.NEXT_PUBLIC_FARO_APP_NAME,
  appVersion: process.env.NEXT_PUBLIC_APP_VERSION,
  environment: process.env.NEXT_PUBLIC_FARO_ENVIRONMENT ?? process.env.NODE_ENV,
})

if (faroConfig) {
  initializeFaro(faroConfig)
}
