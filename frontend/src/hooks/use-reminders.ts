/**
 * API 연동을 useMutation, useQuery, useQueryClient 감싼 파일
 * - useMutation: 서버 데이터를 변경(생성/수정/삭제)하는 훅
 * - useQuery: 서버 데이터를 조회(읽기)하고, 그 결과를 자동으로 캐싱-재사용하는 훅
 * - useQueryClient: 현재 앱 전역의 QueryClient 인스턴스(모든 쿼리 캐시를 관리하는 중앙 저장소)에 접근하기 위한 훅
 */

import {
  useInfiniteQuery,
  useMutation,
  useQueryClient,
  type InfiniteData,
} from "@tanstack/react-query"
import {
  createReminder,
  deleteCompletedReminders,
  deleteReminder,
  getCompletedReminders,
  getReminderIfExists,
  getReminders,
  getRemindersByTag,
  getSmartReminders,
  moveReminder,
  reorderReminders,
  searchReminders,
  toggleReminderComplete,
  toggleReminderFlag,
  updateReminder,
  type CreateReminderInput,
  type Reminder,
  type Slice,
  type UpdateReminderInput,
} from "@/lib/reminders-api"
import {
  SYNC_INTERVAL_MS,
  listsQueryKey,
  remindersQueryKey,
  tagsQueryKey,
} from "@/hooks/query-keys"
import type { Selection } from "@/lib/selection"

function remindersQueryKeyOf(selection: Selection) {
  return [...remindersQueryKey, selection] as const
}

type ReminderPages = InfiniteData<Slice<Reminder>, string | null>

function fetchReminderPage(
  selection: Selection,
  cursor: string | null
): Promise<Slice<Reminder>> {
  switch (selection.type) {
    case "list":
      return singlePage(getReminders(selection.listId))
    case "tag":
      return singlePage(getRemindersByTag(selection.name))
    case "search":
      return searchReminders(selection.query, cursor)
    default:
      return selection.view === "completed"
        ? getCompletedReminders(cursor)
        : singlePage(getSmartReminders(selection.view))
  }
}

async function singlePage(request: Promise<Reminder[]>): Promise<Slice<Reminder>> {
  return { items: await request, nextCursor: null }
}

export function useReminders(selection: Selection) {
  return useInfiniteQuery({
    queryKey: remindersQueryKeyOf(selection),
    queryFn: ({ pageParam }) => fetchReminderPage(selection, pageParam),
    initialPageParam: null as string | null,
    getNextPageParam: (lastPage) => lastPage.nextCursor,
    select: (data) => data.pages.flatMap((page) => page.items),
    refetchInterval: SYNC_INTERVAL_MS, // 공유 리스트에서 다른 멤버가 바꾼 내용을 반영한다
  })
}

function useInvalidateReminderQueries() {
  const queryClient = useQueryClient()
  return () => {
    queryClient.invalidateQueries({ queryKey: remindersQueryKey }) // 쿼리 캐시 무효화
    queryClient.invalidateQueries({ queryKey: listsQueryKey }) // 쿼리 캐시 무효화
    queryClient.invalidateQueries({ queryKey: tagsQueryKey }) // 쿼리 캐시 무효화
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

export function useFetchLatestReminder() {
  const invalidate = useInvalidateReminderQueries()
  return async (id: number) => {
    invalidate()
    return getReminderIfExists(id)
  }
}

export function useMoveReminder() {
  const invalidate = useInvalidateReminderQueries()
  return useMutation({
    mutationFn: ({ id, listId }: { id: number; listId: number }) =>
      moveReminder(id, listId),
    onSuccess: invalidate,
    meta: { errorMessage: "리마인더를 다른 리스트로 옮기지 못했습니다." },
  })
}

function reorderByIds(reminders: Reminder[], ids: number[]): Reminder[] {
  const byId = new Map(reminders.map((reminder) => [reminder.id, reminder]))
  const reordered = ids.flatMap((id) => byId.get(id) ?? [])
  const rest = reminders.filter((reminder) => !ids.includes(reminder.id))
  return [...reordered, ...rest]
}

export function useReorderReminders() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ listId, ids }: { listId: number; ids: number[] }) =>
      reorderReminders(listId, ids),
    meta: { errorMessage: "리마인더 순서를 변경하지 못했습니다." },
    onMutate: async ({ listId, ids }) => {
      const queryKey = remindersQueryKeyOf({ type: "list", listId })
      await queryClient.cancelQueries({ queryKey })
      const previous = queryClient.getQueryData<ReminderPages>(queryKey)
      if (previous) {
        queryClient.setQueryData<ReminderPages>(queryKey, {
          ...previous,
          pages: previous.pages.map((page) => ({
            ...page,
            items: reorderByIds(page.items, ids),
          })),
        })
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

// 반복 리마인더를 완료하면 서버가 다음 회차를 새로 만든다. 응답에는 현재 항목만 오므로
// 캐시를 직접 고치지 않고 모든 리마인더 조회를 무효화해 새 회차가 각 화면(리스트/예정됨 등)에 나타나게 한다.
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

export function useDeleteCompletedReminders() {
  const invalidate = useInvalidateReminderQueries()
  return useMutation({
    mutationFn: (listId: number) => deleteCompletedReminders(listId),
    onSuccess: invalidate,
    meta: { errorMessage: "완료된 항목을 지우지 못했습니다." },
  })
}
