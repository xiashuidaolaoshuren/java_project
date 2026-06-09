import { apiRequest, ApiError } from '@/lib/api'
import type {
  LoginRequest,
  RegisterRequest,
  UserResponse,
} from '@/types/api'

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

export async function login(request: LoginRequest): Promise<UserResponse> {
  return apiRequest<UserResponse>('/api/auth/login', {
    method: 'POST',
    body: request,
  })
}

export async function register(
  request: RegisterRequest,
): Promise<UserResponse> {
  return apiRequest<UserResponse>('/api/auth/register', {
    method: 'POST',
    body: request,
  })
}

export async function logout(): Promise<void> {
  await apiRequest<void>('/api/auth/logout', {
    method: 'POST',
  })
}
