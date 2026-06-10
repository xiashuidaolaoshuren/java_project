import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { renderHook, waitFor } from '@testing-library/react'
import type { ReactNode } from 'react'
import { afterEach, describe, expect, it, vi } from 'vitest'

import { ApiError } from '@/lib/api'
import type { TaskResponse } from '@/types/api'

import { tasksQueryKey, useTasks } from '@/features/tasks/hooks'

vi.mock('@/features/tasks/api', () => ({
  listTasks: vi.fn(),
}))

import { listTasks } from '@/features/tasks/api'

const mockedListTasks = vi.mocked(listTasks)

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

describe('useTasks', () => {
  afterEach(() => {
    vi.clearAllMocks()
  })

  it('uses a stable query key for the task list', () => {
    expect(tasksQueryKey).toEqual(['tasks', 'list'])
  })

  it('returns tasks when the query succeeds', async () => {
    const tasks: TaskResponse[] = [
      {
        id: 1,
        title: 'Task A',
        description: null,
        priority: 'HIGH',
        status: 'OPEN',
        dueDate: '2026-06-15',
        estimatedMinutes: 30,
      },
    ]
    mockedListTasks.mockResolvedValue(tasks)

    const { result } = renderHook(() => useTasks(), {
      wrapper: createWrapper(),
    })

    await waitFor(() => expect(result.current.isSuccess).toBe(true))

    expect(mockedListTasks).toHaveBeenCalled()
    expect(result.current.data).toEqual(tasks)
    expect(result.current.isEmpty).toBe(false)
  })

  it('returns empty state when the user has no tasks', async () => {
    mockedListTasks.mockResolvedValue([])

    const { result } = renderHook(() => useTasks(), {
      wrapper: createWrapper(),
    })

    await waitFor(() => expect(result.current.isSuccess).toBe(true))

    expect(result.current.data).toEqual([])
    expect(result.current.isEmpty).toBe(true)
  })

  it('surfaces query errors', async () => {
    mockedListTasks.mockRejectedValue(
      new ApiError({ status: 500, message: 'Server error' }),
    )

    const { result } = renderHook(() => useTasks(), {
      wrapper: createWrapper(),
    })

    await waitFor(() => expect(result.current.isError).toBe(true))

    expect(result.current.error).toMatchObject({
      status: 500,
      message: 'Server error',
    })
  })
})
