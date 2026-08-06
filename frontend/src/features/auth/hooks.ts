import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'

import { getCurrentUser, login, logout, register } from '@/features/auth/api'
import type { LoginRequest, RegisterRequest } from '@/types/api'

export const currentUserQueryKey = ['auth', 'me'] as const

export function useCurrentUser() {
  const query = useQuery({
    queryKey: currentUserQueryKey,
    queryFn: getCurrentUser,
  })

  return {
    ...query,
    user: query.data ?? null,
    isAuthenticated: query.data != null,
  }
}

export function useLogin() {
  const queryClient = useQueryClient()
  const navigate = useNavigate()

  return useMutation({
    mutationFn: (request: LoginRequest) => login(request),
    onSuccess: (user) => {
      queryClient.setQueryData(currentUserQueryKey, user)
      navigate('/dashboard')
    },
  })
}

export function useRegister() {
  const queryClient = useQueryClient()
  const navigate = useNavigate()

  return useMutation({
    mutationFn: (request: RegisterRequest) => register(request),
    onSuccess: (user) => {
      queryClient.setQueryData(currentUserQueryKey, user)
      navigate('/dashboard')
    },
  })
}

export function useLogout() {
  const queryClient = useQueryClient()
  const navigate = useNavigate()

  return useMutation({
    mutationFn: () => logout(),
    onSuccess: () => {
      queryClient.setQueryData(currentUserQueryKey, null)
      queryClient.clear()
      navigate('/login')
    },
  })
}
