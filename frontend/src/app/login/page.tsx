import type { Metadata } from "next"
import { LoginForm } from "@/components/auth-forms"

export const metadata: Metadata = {
  title: "로그인 | 리마인더",
}

export default function LoginPage() {
  return <LoginForm />
}
