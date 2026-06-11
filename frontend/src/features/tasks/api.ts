import { apiRequest } from '@/lib/api'
import type { CreateTaskRequest, TaskResponse } from '@/types/api'

export async function listTasks(): Promise<TaskResponse[]> {
  return apiRequest<TaskResponse[]>('/api/tasks')
}

export async function createTask(
  request: CreateTaskRequest,
): Promise<TaskResponse> {
  return apiRequest<TaskResponse>('/api/tasks', {
    method: 'POST',
    body: request,
  })
}
