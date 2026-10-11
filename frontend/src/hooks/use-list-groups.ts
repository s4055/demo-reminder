import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import {
  createListGroup,
  deleteListGroup,
  getListGroups,
  renameListGroup,
  reorderListGroups,
  type ListGroup,
} from "@/lib/list-groups-api"
import { listGroupsQueryKey, listsQueryKey } from "@/hooks/query-keys"

// 그룹은 본인만 바꾸므로 주기적으로 다시 조회하지 않는다.
export function useListGroups() {
  return useQuery({
    queryKey: listGroupsQueryKey,
    queryFn: getListGroups,
  })
}

export function useCreateListGroup() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (name: string) => createListGroup(name),
    meta: { errorMessage: "그룹을 만들지 못했습니다." },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: listGroupsQueryKey }) // 쿼리 캐시 무효화
    },
  })
}

export function useRenameListGroup() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, name }: { id: number; name: string }) =>
      renameListGroup(id, name),
    meta: { errorMessage: "그룹 이름을 바꾸지 못했습니다." },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: listGroupsQueryKey }) // 쿼리 캐시 무효화
    },
  })
}

// 소속 리스트의 groupId가 비워지므로 리스트도 다시 조회한다.
export function useDeleteListGroup() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => deleteListGroup(id),
    meta: { errorMessage: "그룹을 삭제하지 못했습니다." },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: listGroupsQueryKey }) // 쿼리 캐시 무효화
      queryClient.invalidateQueries({ queryKey: listsQueryKey }) // 쿼리 캐시 무효화
    },
  })
}

// 드롭 즉시 그룹 순서를 바꾸고(optimistic update), 실패하면 원래 순서로 되돌린다.
export function useReorderListGroups() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (ids: number[]) => reorderListGroups(ids),
    meta: { errorMessage: "그룹 순서를 변경하지 못했습니다." },
    onMutate: async (ids) => {
      await queryClient.cancelQueries({ queryKey: listGroupsQueryKey })
      const previous = queryClient.getQueryData<ListGroup[]>(listGroupsQueryKey)
      if (previous) {
        const byId = new Map(previous.map((group) => [group.id, group]))
        queryClient.setQueryData(
          listGroupsQueryKey,
          ids.flatMap((id) => byId.get(id) ?? [])
        )
      }
      return { previous }
    },
    onError: (_error, _ids, context) => {
      if (context?.previous) {
        queryClient.setQueryData(listGroupsQueryKey, context.previous)
      }
    },
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: listGroupsQueryKey }) // 쿼리 캐시 무효화
    },
  })
}
