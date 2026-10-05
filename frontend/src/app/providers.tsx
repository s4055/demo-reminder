/**
 * 전역으로 선언하며, 특정 조건에서 사용 및 헨들링하기 위한 설정
 */

'use client' // 클라이언트에서 관리되는 컴포넌트 선언

import { useEffect, type ReactNode } from 'react'
import { useRouter } from 'next/navigation'
import { MutationCache, QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { toast } from 'sonner'
import { Toaster } from '@/components/ui/sonner'
import { setUnauthorizedHandler } from '@/lib/api'

function createQueryClient() {
  return new QueryClient({ // 서버에서 가져온 데이터를 관리
    mutationCache: new MutationCache({
      // mutation의 meta.errorMessage가 있으면 그 문구로, 없으면 공통 문구로 알린다.
      // meta.silent가 true이면(로그인 폼처럼 화면에서 직접 안내하는 경우) 알리지 않는다.
      onError: (_error, _variables, _context, mutation) => {
        if (mutation.meta?.silent) return
        const message = mutation.meta?.errorMessage
        toast.error(
          typeof message === 'string'
            ? message
            : '요청을 처리하지 못했습니다. 잠시 후 다시 시도해 주세요.'
        )
      },
    }),
  })
}

let browserQueryClient: QueryClient | undefined // 브라우저 또는 서버를 구분하기 위한 변수

// 윈도우 객체 여부 -> 브라우저, 서버 환경 판단
function getQueryClient() {
  // 서버 환경이므로 사용자 간 캐시 데이터가 섞이지 않도록 신규 생성 
  if (typeof window === 'undefined') return createQueryClient()
  // 브라우저 환경
  browserQueryClient ??= createQueryClient()
  return browserQueryClient
}

export function Providers({ children }: { children: ReactNode }) {
  const queryClient = getQueryClient()
  const router = useRouter()

  // 세션이 만료된 상태로 API를 호출하면(401) 이전 사용자의 캐시를 지우고 로그인 페이지로 보낸다.
  useEffect(() => {
    setUnauthorizedHandler(() => {
      queryClient.clear()
      router.replace('/login')
    })
    return () => setUnauthorizedHandler(undefined)
  }, [queryClient, router])

  return (
    <QueryClientProvider client={queryClient}>
      {children}

      {/* 전역으로 사용할 토스트 컴포넌트 */}
      <Toaster position="top-center" />
    </QueryClientProvider>
  )
}
