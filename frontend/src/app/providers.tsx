/**
 * 전역으로 선언하며, 특정 조건에서 사용 및 헨들링하기 위한 설정
 */

'use client' // 클라이언트에서 관리되는 컴포넌트 선언

import type { ReactNode } from 'react'
import { MutationCache, QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { toast } from 'sonner'
import { Toaster } from '@/components/ui/sonner'

function createQueryClient() {
  return new QueryClient({ // 서버에서 가져온 데이터를 관리
    mutationCache: new MutationCache({
      // mutation의 meta.errorMessage가 있으면 그 문구로, 없으면 공통 문구로 알린다.
      onError: (_error, _variables, _context, mutation) => {
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
  return (
    <QueryClientProvider client={getQueryClient()}>
      {children}

      {/* 전역으로 사용할 토스트 컴포넌트 */}
      <Toaster position="top-center" />
    </QueryClientProvider>
  )
}
