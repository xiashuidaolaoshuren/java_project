/** Frontend API contracts mirrored from Milestone 1 backend DTOs. */

/** Mirrors `com.focusflow.common.error.ApiErrorResponse` */
export type ApiErrorResponse = {
  timestamp?: string
  status: number
  error?: string
  message: string
  path?: string
  details?: Record<string, string[]>
}

export type LoginRequest = {
  username: string
  password: string
}

export type RegisterRequest = {
  email: string
  username: string
  password: string
}

export type UserResponse = {
  id: number
  email: string
  username: string
}

export type TaskPriority = 'LOW' | 'MEDIUM' | 'HIGH'

export type TaskStatus = 'OPEN' | 'IN_PROGRESS' | 'DONE' | 'CANCELLED'

export type TaskResponse = {
  id: number
  title: string
  description: string | null
  priority: TaskPriority
  status: TaskStatus
  dueDate: string | null
  estimatedMinutes: number | null
}

export type CreateTaskRequest = {
  title: string
  description?: string | null
  priority?: TaskPriority
  dueDate?: string | null
  estimatedMinutes?: number | null
}

export type UpdateTaskRequest = {
  title: string
  description?: string | null
  priority?: TaskPriority
  status?: TaskStatus
  dueDate?: string | null
  estimatedMinutes?: number | null
}

export type GeneratePlanRequest = {
  availableMinutes: number
  planDate?: string | null
}

export type DailyPlanItemResponse = {
  position: number
  task: TaskResponse
}

export type DailyPlanResponse = {
  id: number
  planDate: string
  createdAt: string
  items: DailyPlanItemResponse[]
}
