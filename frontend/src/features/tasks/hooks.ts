import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { createTask, deleteTask, listTasks, updateTask } from '@/features/tasks/api'
import type { CreateTaskRequest, UpdateTaskRequest } from '@/types/api'

export const tasksQueryKey = ['tasks', 'list'] as const

export function useTasks() {
  const query = useQuery({
    queryKey: tasksQueryKey,
    queryFn: listTasks,
  })

  return {
    ...query,
    tasks: query.data ?? [],
    isEmpty: query.data != null && query.data.length === 0,
  }
}

export function useCreateTask() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (request: CreateTaskRequest) => createTask(request),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: tasksQueryKey })
    },
  })
}

export function useUpdateTask() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({
      id,
      request,
    }: {
      id: number
      request: UpdateTaskRequest
    }) => updateTask(id, request),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: tasksQueryKey })
    },
  })
}

export function useDeleteTask() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (id: number) => deleteTask(id),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: tasksQueryKey })
    },
  })
}
