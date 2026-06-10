import { useQuery } from '@tanstack/react-query'

import { listTasks } from '@/features/tasks/api'

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
