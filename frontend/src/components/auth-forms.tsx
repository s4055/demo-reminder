"use client"

import { useEffect, type ReactNode } from "react"
import Link from "next/link"
import { useRouter } from "next/navigation"
import { useForm } from "react-hook-form"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { useLogin, useMe, useSignup } from "@/hooks/use-auth"
import { ApiError } from "@/lib/api"

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
const PASSWORD_MIN_LENGTH = 8
// 서버(BCrypt)가 72바이트까지만 받으므로 같은 기준으로 미리 막는다.
const PASSWORD_MAX_BYTES = 72

type LoginValues = {
  email: string
  password: string
}

type SignupValues = {
  email: string
  name: string
  password: string
  passwordConfirm: string
}

export function LoginForm() {
  const login = useLogin()
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<LoginValues>({ defaultValues: { email: "", password: "" } })

  function onSubmit(values: LoginValues) {
    login.mutate({ email: values.email.trim(), password: values.password })
  }

  const serverError = login.error
    ? login.error instanceof ApiError && login.error.status === 401
      ? "이메일 또는 비밀번호가 올바르지 않습니다."
      : "로그인하지 못했습니다. 잠시 후 다시 시도해 주세요."
    : undefined

  return (
    <AuthCard
      title="로그인"
      footer={
        <>
          계정이 없으신가요?{" "}
          <Link href="/signup" className="font-medium text-primary underline-offset-4 hover:underline">
            회원가입
          </Link>
        </>
      }
    >
      <form onSubmit={handleSubmit(onSubmit)} noValidate className="grid gap-4">
        <Field id="login-email" label="이메일" error={errors.email?.message}>
          <Input
            id="login-email"
            type="email"
            autoComplete="email"
            autoFocus
            aria-invalid={errors.email ? true : undefined}
            {...register("email", {
              validate: (value) =>
                value.trim().length > 0 || "이메일을 입력해 주세요.",
            })}
          />
        </Field>
        <Field id="login-password" label="비밀번호" error={errors.password?.message}>
          <Input
            id="login-password"
            type="password"
            autoComplete="current-password"
            aria-invalid={errors.password ? true : undefined}
            {...register("password", { required: "비밀번호를 입력해 주세요." })}
          />
        </Field>
        {serverError && (
          <p role="alert" className="text-sm text-destructive">
            {serverError}
          </p>
        )}
        <Button type="submit" disabled={login.isPending || login.isSuccess}>
          {login.isPending ? "로그인 중..." : "로그인"}
        </Button>
      </form>
    </AuthCard>
  )
}

export function SignupForm() {
  const signup = useSignup()
  const {
    register,
    handleSubmit,
    setError,
    getValues,
    formState: { errors },
  } = useForm<SignupValues>({
    defaultValues: { email: "", name: "", password: "", passwordConfirm: "" },
  })

  function onSubmit(values: SignupValues) {
    signup.mutate(
      {
        email: values.email.trim(),
        name: values.name.trim(),
        password: values.password,
      },
      {
        onError: (error) => {
          if (error instanceof ApiError && error.status === 409) {
            setError("email", { message: "이미 가입한 이메일입니다." })
          }
        },
      }
    )
  }

  // 이메일 중복(409)은 이메일 칸에 표시하고, 나머지 실패만 폼 아래에 안내한다.
  const serverError =
    signup.error &&
    !(signup.error instanceof ApiError && signup.error.status === 409)
      ? "회원가입하지 못했습니다. 입력값을 확인하거나 잠시 후 다시 시도해 주세요."
      : undefined

  return (
    <AuthCard
      title="회원가입"
      footer={
        <>
          이미 계정이 있으신가요?{" "}
          <Link href="/login" className="font-medium text-primary underline-offset-4 hover:underline">
            로그인
          </Link>
        </>
      }
    >
      <form onSubmit={handleSubmit(onSubmit)} noValidate className="grid gap-4">
        <Field id="signup-email" label="이메일" error={errors.email?.message}>
          <Input
            id="signup-email"
            type="email"
            autoComplete="email"
            autoFocus
            aria-invalid={errors.email ? true : undefined}
            {...register("email", {
              validate: (value) => {
                if (!value.trim()) return "이메일을 입력해 주세요."
                return (
                  EMAIL_PATTERN.test(value.trim()) ||
                  "이메일 형식이 올바르지 않습니다."
                )
              },
            })}
          />
        </Field>
        <Field id="signup-name" label="이름" error={errors.name?.message}>
          <Input
            id="signup-name"
            autoComplete="name"
            aria-invalid={errors.name ? true : undefined}
            {...register("name", {
              validate: (value) => {
                if (!value.trim()) return "이름을 입력해 주세요."
                return (
                  value.trim().length <= 50 || "이름은 50자 이하로 입력해 주세요."
                )
              },
            })}
          />
        </Field>
        <Field id="signup-password" label="비밀번호" error={errors.password?.message}>
          <Input
            id="signup-password"
            type="password"
            autoComplete="new-password"
            aria-invalid={errors.password ? true : undefined}
            {...register("password", {
              required: "비밀번호를 입력해 주세요.",
              minLength: {
                value: PASSWORD_MIN_LENGTH,
                message: `비밀번호는 ${PASSWORD_MIN_LENGTH}자 이상이어야 합니다.`,
              },
              validate: (value) =>
                new TextEncoder().encode(value).length <= PASSWORD_MAX_BYTES ||
                "비밀번호가 너무 깁니다.",
            })}
          />
        </Field>
        <Field
          id="signup-password-confirm"
          label="비밀번호 확인"
          error={errors.passwordConfirm?.message}
        >
          <Input
            id="signup-password-confirm"
            type="password"
            autoComplete="new-password"
            aria-invalid={errors.passwordConfirm ? true : undefined}
            {...register("passwordConfirm", {
              validate: (value) =>
                value === getValues("password") || "비밀번호가 일치하지 않습니다.",
            })}
          />
        </Field>
        {serverError && (
          <p role="alert" className="text-sm text-destructive">
            {serverError}
          </p>
        )}
        <Button type="submit" disabled={signup.isPending || signup.isSuccess}>
          {signup.isPending ? "가입 중..." : "가입하기"}
        </Button>
      </form>
    </AuthCard>
  )
}

// 로그인/회원가입 화면 공통 틀. 이미 로그인한 사용자는 메인 화면으로 보낸다.
function AuthCard({
  title,
  footer,
  children,
}: {
  title: string
  footer: ReactNode
  children: ReactNode
}) {
  const router = useRouter()
  const { data: user } = useMe()

  useEffect(() => {
    if (user) router.replace("/")
  }, [user, router])

  return (
    <main className="flex flex-1 items-center justify-center px-4 py-10">
      <div className="flex w-full max-w-sm flex-col gap-6 rounded-xl border border-border bg-background p-6 shadow-sm">
        <div className="flex flex-col gap-1">
          <p className="text-sm text-muted-foreground">리마인더</p>
          <h1 className="text-2xl font-semibold">{title}</h1>
        </div>
        {children}
        <p className="text-center text-sm text-muted-foreground">{footer}</p>
      </div>
    </main>
  )
}

function Field({
  id,
  label,
  error,
  children,
}: {
  id: string
  label: string
  error?: string
  children: ReactNode
}) {
  return (
    <div className="grid gap-1.5">
      <Label htmlFor={id}>{label}</Label>
      {children}
      {error && <p className="text-xs text-destructive">{error}</p>}
    </div>
  )
}
