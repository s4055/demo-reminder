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

// 로그인한 사용자. 로그인하지 않았으면 401 ApiError로 실패하며, 이 경우 재시도하지 않는다.
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

// 사용자가 바뀌므로 이전 사용자의 캐시를 모두 지우고 새 사용자로 메인 화면에 들어간다.
function useEnterApp() {
  const queryClient = useQueryClient()
  const router = useRouter()
  return (user: User) => {
    queryClient.clear()
    queryClient.setQueryData(meQueryKey, user)
    router.replace("/")
  }
}

// 로그인/가입 실패는 폼에 직접 안내하므로 전역 토스트는 띄우지 않는다.
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
