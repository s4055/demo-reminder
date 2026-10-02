/**
 * API 연동을 useMutation, useQuery, useQueryClient 감싼 파일
 * - useMutation: 서버 데이터를 변경(생성/수정/삭제)하는 훅
 * - useQuery: 서버 데이터를 조회(읽기)하고, 그 결과를 자동으로 캐싱-재사용하는 훅
 * - useQueryClient: 현재 앱 전역의 QueryClient 인스턴스(모든 쿼리 캐시를 관리하는 중앙 저장소)에 접근하기 위한 훅
 */

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import {
  createReminder,
  deleteReminder,
  getReminders,
  getSmartReminders,
  reorderReminders,
  toggleReminderComplete,
  toggleReminderFlag,
  updateReminder,
  type CreateReminderInput,
  type Reminder,
  type UpdateReminderInput,
} from "@/lib/reminders-api"
import { listsQueryKey, remindersQueryKey } from "@/hooks/query-keys"
import type { Selection } from "@/lib/selection"

function remindersQueryKeyOf(selection: Selection) {
  return [...remindersQueryKey, selection] as const
}

export function useReminders(selection: Selection) {
  return useQuery({
    queryKey: remindersQueryKeyOf(selection),
    queryFn: () =>
      selection.type === "list"
        ? getReminders(selection.listId)
        : getSmartReminders(selection.view),
  })
}

// 리마인더 변경은 목록 조회 결과와 사이드바의 리스트별 개수에 모두 영향을 준다.
function useInvalidateReminderQueries() {
  const queryClient = useQueryClient()
  return () => {
    queryClient.invalidateQueries({ queryKey: remindersQueryKey }) // 쿼리 캐시 무효화
    queryClient.invalidateQueries({ queryKey: listsQueryKey }) // 쿼리 캐시 무효화
  }
}

export function useCreateReminder() {
  const invalidate = useInvalidateReminderQueries()
  return useMutation({
    mutationFn: (input: CreateReminderInput) => createReminder(input),
    onSuccess: invalidate,
  })
}

export function useUpdateReminder() {
  const invalidate = useInvalidateReminderQueries()
  return useMutation({
    mutationFn: ({ id, input }: { id: number; input: UpdateReminderInput }) =>
      updateReminder(id, input),
    onSuccess: invalidate,
  })
}

// 드롭 즉시 리스트 화면의 순서를 바꾸고(optimistic update), 실패하면 원래 순서로 되돌린다.
export function useReorderReminders() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ listId, ids }: { listId: number; ids: number[] }) =>
      reorderReminders(listId, ids),
    meta: { errorMessage: "리마인더 순서를 변경하지 못했습니다." },
    onMutate: async ({ listId, ids }) => {
      const queryKey = remindersQueryKeyOf({ type: "list", listId })
      await queryClient.cancelQueries({ queryKey })
      const previous = queryClient.getQueryData<Reminder[]>(queryKey)
      if (previous) {
        const byId = new Map(previous.map((reminder) => [reminder.id, reminder]))
        const reordered = ids.flatMap((id) => byId.get(id) ?? [])
        const rest = previous.filter((reminder) => !ids.includes(reminder.id))
        queryClient.setQueryData(queryKey, [...reordered, ...rest])
      }
      return { queryKey, previous }
    },
    onError: (_error, _input, context) => {
      if (context?.previous) {
        queryClient.setQueryData(context.queryKey, context.previous)
      }
    },
    onSettled: (_data, _error, { listId }) => {
      queryClient.invalidateQueries({
        queryKey: remindersQueryKeyOf({ type: "list", listId }),
      }) // 쿼리 캐시 무효화
    },
  })
}

export function useToggleReminderComplete() {
  const invalidate = useInvalidateReminderQueries()
  return useMutation({
    mutationFn: (id: number) => toggleReminderComplete(id),
    onSuccess: invalidate,
  })
}

export function useToggleReminderFlag() {
  const invalidate = useInvalidateReminderQueries()
  return useMutation({
    mutationFn: (id: number) => toggleReminderFlag(id),
    onSuccess: invalidate,
  })
}

export function useDeleteReminder() {
  const invalidate = useInvalidateReminderQueries()
  return useMutation({
    mutationFn: (id: number) => deleteReminder(id),
    onSuccess: invalidate,
  })
}
