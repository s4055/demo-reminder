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
  toggleReminderComplete,
  toggleReminderFlag,
  updateReminder,
  type CreateReminderInput,
  type UpdateReminderInput,
} from "@/lib/reminders-api"
import { listsQueryKey, remindersQueryKey } from "@/hooks/query-keys"
import type { Selection } from "@/lib/selection"

export function useReminders(selection: Selection) {
  return useQuery({
    queryKey: [...remindersQueryKey, selection],
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
