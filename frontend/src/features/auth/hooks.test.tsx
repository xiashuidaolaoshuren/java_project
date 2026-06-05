import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { renderHook, waitFor } from '@testing-library/react'
import type { ReactNode } from 'react'
import { afterEach, describe, expect, it, vi } from 'vitest'

import type { UserResponse } from '@/types/api'

import { currentUserQueryKey, useCurrentUser } from '@/features/auth/hooks'

vi.mock('@/features/auth/api', () => ({
  getCurrentUser: vi.fn(),
}))

import { getCurrentUser } from '@/features/auth/api'

const mockedGetCurrentUser = vi.mocked(getCurrentUser)

function createWrapper() {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
    },
  })

  return function Wrapper({ children }: { children: ReactNode }) {
    return (
      <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    )
  }
}

describe('useCurrentUser', () => {
  afterEach(() => {
    vi.clearAllMocks()
  })

  it('uses a stable query key for the current user', () => {
    expect(currentUserQueryKey).toEqual(['auth', 'me'])
  })

  it('returns the authenticated user when the session is valid', async () => {
    const user: UserResponse = {
      id: 1,
      email: 'user@example.com',
      username: 'user',
    }
    mockedGetCurrentUser.mockResolvedValue(user)

    const { result } = renderHook(() => useCurrentUser(), {
      wrapper: createWrapper(),
    })

    await waitFor(() => expect(result.current.isSuccess).toBe(true))

    expect(mockedGetCurrentUser).toHaveBeenCalled()
    expect(result.current.data).toEqual(user)
    expect(result.current.isAuthenticated).toBe(true)
  })

  it('returns unauthenticated state when no user session exists', async () => {
    mockedGetCurrentUser.mockResolvedValue(null)

    const { result } = renderHook(() => useCurrentUser(), {
      wrapper: createWrapper(),
    })

    await waitFor(() => expect(result.current.isSuccess).toBe(true))

    expect(result.current.data).toBeNull()
    expect(result.current.isAuthenticated).toBe(false)
  })
})
