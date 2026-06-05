import { apiRequest, ApiError } from '@/lib/api'
import type { UserResponse } from '@/types/api'

export async function getCurrentUser(): Promise<UserResponse | null> {
  try {
    return await apiRequest<UserResponse>('/api/auth/me')
  } catch (error) {
    if (error instanceof ApiError && error.status === 401) {
      return null
    }
    throw error
  }
}
