import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import {
  createReminder,
  deleteReminder,
  getReminders,
  getSmartReminders,
  toggleReminderComplete,
  toggleReminderFlag,
  type CreateReminderInput,
} from "@/lib/reminders-api"
import { listsQueryKey } from "@/hooks/use-lists"
import type { Selection } from "@/lib/selection"

const remindersQueryKey = ["reminders"] as const

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
    queryClient.invalidateQueries({ queryKey: remindersQueryKey })
    queryClient.invalidateQueries({ queryKey: listsQueryKey })
  }
}

export function useCreateReminder() {
  const invalidate = useInvalidateReminderQueries()
  return useMutation({
    mutationFn: (input: CreateReminderInput) => createReminder(input),
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
