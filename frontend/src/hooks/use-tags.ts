/**
 * 태그 API 연동 훅
 */

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { deleteTag, getTags } from "@/lib/tags-api"
import { SYNC_INTERVAL_MS, remindersQueryKey, tagsQueryKey } from "@/hooks/query-keys"

export function useTags() {
  return useQuery({
    queryKey: tagsQueryKey,
    queryFn: getTags,
    refetchInterval: SYNC_INTERVAL_MS,
  })
}

// 태그를 지우면 리마인더 항목의 #태그 표시도 바뀌므로 리마인더 조회도 무효화한다.
export function useDeleteTag() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => deleteTag(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: tagsQueryKey }) // 쿼리 캐시 무효화
      queryClient.invalidateQueries({ queryKey: remindersQueryKey }) // 쿼리 캐시 무효화
    },
  })
}
