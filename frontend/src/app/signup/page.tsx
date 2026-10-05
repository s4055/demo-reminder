import type { Metadata } from "next"
import { SignupForm } from "@/components/auth-forms"

export const metadata: Metadata = {
  title: "회원가입 | 리마인더",
}

export default function SignupPage() {
  return <SignupForm />
}
