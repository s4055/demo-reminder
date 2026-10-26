import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { useRouter } from "next/navigation"
import { meQueryKey } from "@/hooks/query-keys"
import { ApiError } from "@/lib/api"
import {
  getMe,
  login,
  logout,
  signup,
  type LoginInput,
  type SignupInput,
  type User,
} from "@/lib/auth-api"

export function useMe() {
  return useQuery({
    queryKey: meQueryKey,
    queryFn: getMe,
    retry: (failureCount, error) =>
      !(error instanceof ApiError && error.status === 401) && failureCount < 2,
    staleTime: Infinity,
  })
}

export function isUnauthorized(error: unknown): boolean {
  return error instanceof ApiError && error.status === 401
}

function useEnterApp() {
  const queryClient = useQueryClient()
  const router = useRouter()
  return (user: User) => {
    queryClient.clear()
    queryClient.setQueryData(meQueryKey, user)
    router.replace("/")
  }
}

export function useLogin() {
  const enterApp = useEnterApp()
  return useMutation({
    mutationFn: (input: LoginInput) => login(input),
    meta: { silent: true },
    onSuccess: enterApp,
  })
}

export function useSignup() {
  const enterApp = useEnterApp()
  return useMutation({
    mutationFn: (input: SignupInput) => signup(input),
    meta: { silent: true },
    onSuccess: enterApp,
  })
}

export function useLogout() {
  const queryClient = useQueryClient()
  const router = useRouter()
  return useMutation({
    mutationFn: logout,
    meta: { errorMessage: "로그아웃하지 못했습니다." },
    onSuccess: () => {
      queryClient.clear()
      router.replace("/login")
    },
  })
}
