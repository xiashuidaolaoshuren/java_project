import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { renderHook, waitFor } from '@testing-library/react'
import type { ReactNode } from 'react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { MemoryRouter, useLocation } from 'react-router-dom'

import type { UserResponse } from '@/types/api'

import {
  currentUserQueryKey,
  useCurrentUser,
  useLogin,
  useRegister,
} from '@/features/auth/hooks'

vi.mock('@/features/auth/api', () => ({
  getCurrentUser: vi.fn(),
  login: vi.fn(),
  register: vi.fn(),
}))

import { getCurrentUser, login, register } from '@/features/auth/api'

const mockedGetCurrentUser = vi.mocked(getCurrentUser)
const mockedLogin = vi.mocked(login)
const mockedRegister = vi.mocked(register)

function createWrapper(queryClient?: QueryClient) {
  const client =
    queryClient ??
    new QueryClient({
      defaultOptions: {
        queries: { retry: false },
        mutations: { retry: false },
      },
    })

  return function Wrapper({ children }: { children: ReactNode }) {
    return (
      <QueryClientProvider client={client}>
        <MemoryRouter initialEntries={['/login']}>{children}</MemoryRouter>
      </QueryClientProvider>
    )
  }
}

function LocationDisplay() {
  const location = useLocation()
  return <div data-testid="location">{location.pathname}</div>
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

describe('useLogin', () => {
  afterEach(() => {
    vi.clearAllMocks()
  })

  it('invalidates the current user query and navigates to /dashboard on success', async () => {
    const user: UserResponse = {
      id: 1,
      email: 'user@example.com',
      username: 'user',
    }
    mockedLogin.mockResolvedValue(user)

    const queryClient = new QueryClient({
      defaultOptions: {
        queries: { retry: false },
        mutations: { retry: false },
      },
    })
    const invalidateSpy = vi.spyOn(queryClient, 'invalidateQueries')

    const { result } = renderHook(
      () => ({
        login: useLogin(),
        location: useLocation(),
      }),
      {
        wrapper: ({ children }) => (
          <QueryClientProvider client={queryClient}>
            <MemoryRouter initialEntries={['/login']}>
              {children}
              <LocationDisplay />
            </MemoryRouter>
          </QueryClientProvider>
        ),
      },
    )

    result.current.login.mutate({ username: 'user', password: 'password123' })

    await waitFor(() => expect(result.current.login.isSuccess).toBe(true))

    expect(mockedLogin).toHaveBeenCalledWith({
      username: 'user',
      password: 'password123',
    })
    expect(invalidateSpy).toHaveBeenCalledWith({
      queryKey: currentUserQueryKey,
    })
    expect(result.current.location.pathname).toBe('/dashboard')
  })
})

describe('useRegister', () => {
  afterEach(() => {
    vi.clearAllMocks()
  })

  it('sets current user query data and navigates to /dashboard on success', async () => {
    const user: UserResponse = {
      id: 2,
      email: 'new@example.com',
      username: 'newuser',
    }
    mockedRegister.mockResolvedValue(user)

    const queryClient = new QueryClient({
      defaultOptions: {
        queries: { retry: false },
        mutations: { retry: false },
      },
    })
    const setQueryDataSpy = vi.spyOn(queryClient, 'setQueryData')

    const { result } = renderHook(
      () => ({
        register: useRegister(),
        location: useLocation(),
      }),
      {
        wrapper: ({ children }) => (
          <QueryClientProvider client={queryClient}>
            <MemoryRouter initialEntries={['/register']}>
              {children}
              <LocationDisplay />
            </MemoryRouter>
          </QueryClientProvider>
        ),
      },
    )

    result.current.register.mutate({
      email: 'new@example.com',
      username: 'newuser',
      password: 'password123',
    })

    await waitFor(() => expect(result.current.register.isSuccess).toBe(true))

    expect(mockedRegister).toHaveBeenCalledWith({
      email: 'new@example.com',
      username: 'newuser',
      password: 'password123',
    })
    expect(setQueryDataSpy).toHaveBeenCalledWith(currentUserQueryKey, user)
    expect(result.current.location.pathname).toBe('/dashboard')
  })
})
