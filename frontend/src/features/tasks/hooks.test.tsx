import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { renderHook, waitFor } from '@testing-library/react'
import type { ReactNode } from 'react'
import { afterEach, describe, expect, it, vi } from 'vitest'

import { ApiError } from '@/lib/api'
import type { TaskResponse } from '@/types/api'

import { tasksQueryKey, useCreateTask, useDeleteTask, useTasks, useUpdateTask } from '@/features/tasks/hooks'

vi.mock('@/features/tasks/api', () => ({
  listTasks: vi.fn(),
  createTask: vi.fn(),
  updateTask: vi.fn(),
  deleteTask: vi.fn(),
}))

import { createTask, deleteTask, listTasks, updateTask } from '@/features/tasks/api'

const mockedListTasks = vi.mocked(listTasks)
const mockedCreateTask = vi.mocked(createTask)
const mockedUpdateTask = vi.mocked(updateTask)
const mockedDeleteTask = vi.mocked(deleteTask)

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
      <QueryClientProvider client={client}>{children}</QueryClientProvider>
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

describe('useCreateTask', () => {
  afterEach(() => {
    vi.clearAllMocks()
  })

  it('calls createTask and invalidates the task list on success', async () => {
    const created: TaskResponse = {
      id: 3,
      title: 'Write tests',
      description: 'TDD coverage',
      priority: 'HIGH',
      status: 'OPEN',
      dueDate: '2026-06-01',
      estimatedMinutes: 45,
    }
    mockedCreateTask.mockResolvedValue(created)

    const queryClient = new QueryClient({
      defaultOptions: {
        queries: { retry: false },
        mutations: { retry: false },
      },
    })
    const invalidateSpy = vi.spyOn(queryClient, 'invalidateQueries')

    const { result } = renderHook(() => useCreateTask(), {
      wrapper: createWrapper(queryClient),
    })

    result.current.mutate({
      title: 'Write tests',
      description: 'TDD coverage',
      priority: 'HIGH',
      dueDate: '2026-06-01',
      estimatedMinutes: 45,
    })

    await waitFor(() => expect(result.current.isSuccess).toBe(true))

    expect(mockedCreateTask).toHaveBeenCalledWith({
      title: 'Write tests',
      description: 'TDD coverage',
      priority: 'HIGH',
      dueDate: '2026-06-01',
      estimatedMinutes: 45,
    })
    expect(invalidateSpy).toHaveBeenCalledWith({
      queryKey: tasksQueryKey,
    })
  })
})

describe('useUpdateTask', () => {
  afterEach(() => {
    vi.clearAllMocks()
  })

  it('calls updateTask and invalidates the task list on success', async () => {
    const updated: TaskResponse = {
      id: 1,
      title: 'Updated title',
      description: 'Updated description',
      priority: 'HIGH',
      status: 'IN_PROGRESS',
      dueDate: '2026-06-10',
      estimatedMinutes: 60,
    }
    mockedUpdateTask.mockResolvedValue(updated)

    const queryClient = new QueryClient({
      defaultOptions: {
        queries: { retry: false },
        mutations: { retry: false },
      },
    })
    const invalidateSpy = vi.spyOn(queryClient, 'invalidateQueries')

    const { result } = renderHook(() => useUpdateTask(), {
      wrapper: createWrapper(queryClient),
    })

    result.current.mutate({
      id: 1,
      request: {
        title: 'Updated title',
        description: 'Updated description',
        priority: 'HIGH',
        status: 'IN_PROGRESS',
        dueDate: '2026-06-10',
        estimatedMinutes: 60,
      },
    })

    await waitFor(() => expect(result.current.isSuccess).toBe(true))

    expect(mockedUpdateTask).toHaveBeenCalledWith(1, {
      title: 'Updated title',
      description: 'Updated description',
      priority: 'HIGH',
      status: 'IN_PROGRESS',
      dueDate: '2026-06-10',
      estimatedMinutes: 60,
    })
    expect(invalidateSpy).toHaveBeenCalledWith({
      queryKey: tasksQueryKey,
    })
  })
})

describe('useDeleteTask', () => {
  afterEach(() => {
    vi.clearAllMocks()
  })

  it('calls deleteTask and invalidates the task list on success', async () => {
    mockedDeleteTask.mockResolvedValue(undefined)

    const queryClient = new QueryClient({
      defaultOptions: {
        queries: { retry: false },
        mutations: { retry: false },
      },
    })
    const invalidateSpy = vi.spyOn(queryClient, 'invalidateQueries')

    const { result } = renderHook(() => useDeleteTask(), {
      wrapper: createWrapper(queryClient),
    })

    result.current.mutate(1)

    await waitFor(() => expect(result.current.isSuccess).toBe(true))

    expect(mockedDeleteTask).toHaveBeenCalledWith(1)
    expect(invalidateSpy).toHaveBeenCalledWith({
      queryKey: tasksQueryKey,
    })
  })
})
