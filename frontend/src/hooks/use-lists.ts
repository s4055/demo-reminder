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
  getListMembers,
  getLists,
  isOwner,
  inviteListMember,
  removeListMember,
  reorderLists,
  updateList,
  type ReminderList,
  type ReminderListInput,
} from "@/lib/lists-api"
import {
  SYNC_INTERVAL_MS,
  listMembersQueryKey,
  listsQueryKey,
  remindersQueryKey,
  tagsQueryKey,
} from "@/hooks/query-keys"

export function useLists() {
  return useQuery({
    queryKey: listsQueryKey,
    queryFn: getLists,
    refetchInterval: SYNC_INTERVAL_MS, // 공유받거나 공유가 해제된 리스트, 다른 멤버가 바꾼 개수를 반영한다
  })
}

export function useCreateList() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (input: ReminderListInput) => createList(input),
    onSuccess: (created) => {
      // 만든 리스트를 바로 선택하므로, 다시 조회하기 전에도 목록에 있도록 소유한 리스트의 마지막에 넣어 둔다.
      queryClient.setQueryData<ReminderList[]>(listsQueryKey, (previous) =>
        previous && [
          ...previous.filter(isOwner),
          created,
          ...previous.filter((list) => !isOwner(list)),
        ]
      )
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
        // ids에는 소유한 리스트만 있으므로 공유받은 리스트는 원래 순서대로 뒤에 둔다.
        const byId = new Map(previous.map((list) => [list.id, list]))
        queryClient.setQueryData(listsQueryKey, [
          ...ids.flatMap((id) => byId.get(id) ?? []),
          ...previous.filter((list) => !isOwner(list)),
        ])
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

export function useListMembers(listId: number | undefined) {
  return useQuery({
    queryKey: listMembersQueryKey(listId ?? -1),
    queryFn: () => getListMembers(listId!),
    enabled: listId !== undefined,
    refetchInterval: SYNC_INTERVAL_MS,
  })
}

// 실패(없는 사용자, 이미 멤버)는 공유 다이얼로그에서 직접 안내하므로 전역 토스트는 띄우지 않는다.
// 리스트 목록의 멤버 수가 바뀌므로 리스트도 다시 조회한다 (멤버 목록도 listsQueryKey 아래에 있다).
export function useInviteListMember() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ listId, email }: { listId: number; email: string }) =>
      inviteListMember(listId, email),
    meta: { silent: true },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: listsQueryKey }) // 쿼리 캐시 무효화
    },
  })
}

// 멤버 제거와 나가기(본인 제거)에 함께 쓴다. 나가면 그 리스트의 리마인더가 스마트 뷰/태그에서도 빠진다.
export function useRemoveListMember() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ listId, userId }: { listId: number; userId: number }) =>
      removeListMember(listId, userId),
    meta: { errorMessage: "멤버를 제거하지 못했습니다." },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: listsQueryKey }) // 쿼리 캐시 무효화
      queryClient.invalidateQueries({ queryKey: remindersQueryKey }) // 쿼리 캐시 무효화
      queryClient.invalidateQueries({ queryKey: tagsQueryKey }) // 쿼리 캐시 무효화
    },
  })
}
