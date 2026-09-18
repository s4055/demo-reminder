import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import {
  createReminder,
  deleteReminder,
  getReminders,
  toggleReminderComplete,
  type CreateReminderInput,
} from "@/lib/reminders-api"

const remindersQueryKey = ["reminders"] as const

export function useReminders() {
  return useQuery({
    queryKey: remindersQueryKey,
    queryFn: getReminders,
  })
}

export function useCreateReminder() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (input: CreateReminderInput) => createReminder(input),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: remindersQueryKey })
    },
  })
}

export function useToggleReminderComplete() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => toggleReminderComplete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: remindersQueryKey })
    },
  })
}

export function useDeleteReminder() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => deleteReminder(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: remindersQueryKey })
    },
  })
}
