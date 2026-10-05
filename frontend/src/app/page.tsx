/**
 * Next.js 프레임워크가 파일 규칙에 따라 자동으로 layout.tsx의 children 자리 주입
 */

import { AuthGate } from "@/components/auth-gate"
import { RemindersApp } from "@/components/reminders-app"

export default function Home() {
  return (
    <AuthGate>
      <RemindersApp />
    </AuthGate>
  )
}
