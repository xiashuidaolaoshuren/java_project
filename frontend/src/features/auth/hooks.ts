import { useQuery } from '@tanstack/react-query'

import { getCurrentUser } from '@/features/auth/api'

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
