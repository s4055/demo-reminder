'use client'

import type { ReactNode } from 'react'
import { MutationCache, QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { toast } from 'sonner'
import { Toaster } from '@/components/ui/sonner'

function createQueryClient() {
  return new QueryClient({
    mutationCache: new MutationCache({
      onError: () => {
        toast.error('요청을 처리하지 못했습니다. 잠시 후 다시 시도해 주세요.')
      },
    }),
  })
}

let browserQueryClient: QueryClient | undefined

function getQueryClient() {
  if (typeof window === 'undefined') return createQueryClient()
  browserQueryClient ??= createQueryClient()
  return browserQueryClient
}

export function Providers({ children }: { children: ReactNode }) {
  return (
    <QueryClientProvider client={getQueryClient()}>
      {children}
      <Toaster position="top-center" />
    </QueryClientProvider>
  )
}
