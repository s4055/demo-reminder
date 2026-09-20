import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import {
  createList,
  deleteList,
  getLists,
  type ReminderListInput,
} from "@/lib/lists-api"

export const listsQueryKey = ["lists"] as const

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
      queryClient.invalidateQueries({ queryKey: listsQueryKey })
    },
  })
}

export function useDeleteList() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => deleteList(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: listsQueryKey })
      queryClient.invalidateQueries({ queryKey: ["reminders"] })
    },
  })
}
