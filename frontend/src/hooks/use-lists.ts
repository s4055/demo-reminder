/**
 * API 연동을 useMutation, useQuery, useQueryClient 감싼 파일
 * - useMutation: 서버 데이터를 변경(생성/수정/삭제)하는 훅
 * - useQuery: 서버 데이터를 조회(읽기)하고, 그 결과를 자동으로 캐싱-재사용하는 훅
 * - useQueryClient: 현재 앱 전역의 QueryClient 인스턴스(모든 쿼리 캐시를 관리하는 중앙 저장소)에 접근하기 위한 훅
 */

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import {
  createList,
  deleteList,
  getLists,
  updateList,
  type ReminderListInput,
} from "@/lib/lists-api"
import { listsQueryKey, remindersQueryKey } from "@/hooks/query-keys"

export function useLists() {
  return useQuery({
    queryKey: listsQueryKey,
    queryFn: getLists,
  })
}

export function useCreateList() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (input: ReminderListInput) => createList(input),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: listsQueryKey }) // 쿼리 캐시 무효화
    },
  })
}

export function useUpdateList() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, input }: { id: number; input: ReminderListInput }) =>
      updateList(id, input),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: listsQueryKey }) // 쿼리 캐시 무효화
    },
  })
}

export function useDeleteList() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => deleteList(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: listsQueryKey }) // 쿼리 캐시 무효화
      queryClient.invalidateQueries({ queryKey: remindersQueryKey }) // 쿼리 캐시 무효화
    },
  })
}
