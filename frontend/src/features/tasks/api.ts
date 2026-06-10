import { apiRequest } from '@/lib/api'
import type { TaskResponse } from '@/types/api'

export async function listTasks(): Promise<TaskResponse[]> {
  return apiRequest<TaskResponse[]>('/api/tasks')
}
