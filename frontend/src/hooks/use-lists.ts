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
  reorderLists,
  updateList,
  type ReminderList,
  type ReminderListInput,
} from "@/lib/lists-api"
import { listsQueryKey, remindersQueryKey, tagsQueryKey } from "@/hooks/query-keys"

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

// 드롭 즉시 사이드바 순서를 바꾸고(optimistic update), 실패하면 원래 순서로 되돌린다.
export function useReorderLists() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (ids: number[]) => reorderLists(ids),
    meta: { errorMessage: "리스트 순서를 변경하지 못했습니다." },
    onMutate: async (ids) => {
      await queryClient.cancelQueries({ queryKey: listsQueryKey })
      const previous = queryClient.getQueryData<ReminderList[]>(listsQueryKey)
      if (previous) {
        const byId = new Map(previous.map((list) => [list.id, list]))
        queryClient.setQueryData(
          listsQueryKey,
          ids.flatMap((id) => byId.get(id) ?? [])
        )
      }
      return { previous }
    },
    onError: (_error, _ids, context) => {
      if (context?.previous) {
        queryClient.setQueryData(listsQueryKey, context.previous)
      }
    },
    onSettled: () => {
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
      queryClient.invalidateQueries({ queryKey: tagsQueryKey }) // 리스트와 함께 리마인더가 지워지면 태그별 개수도 바뀐다
    },
  })
}
