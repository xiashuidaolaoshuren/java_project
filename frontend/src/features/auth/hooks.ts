import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'

import { getCurrentUser, login, register } from '@/features/auth/api'
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
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: currentUserQueryKey })
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
