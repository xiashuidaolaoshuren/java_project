import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'

import { ApiError } from '@/lib/api'
import type { TaskResponse } from '@/types/api'
import { TaskList } from '@/features/tasks/TaskList'

vi.mock('@/features/tasks/hooks', () => ({
  useTasks: vi.fn(),
}))

import { useTasks } from '@/features/tasks/hooks'

const mockedUseTasks = vi.mocked(useTasks)

function renderTaskList() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })

  return render(
    <QueryClientProvider client={queryClient}>
      <TaskList />
    </QueryClientProvider>,
  )
}

const sampleTask: TaskResponse = {
  id: 1,
  title: 'Write report',
  description: 'Quarterly summary',
  priority: 'HIGH',
  status: 'OPEN',
  dueDate: '2026-06-15',
  estimatedMinutes: 60,
}

describe('TaskList', () => {
  it('renders skeleton rows while loading', () => {
    mockedUseTasks.mockReturnValue({
      isPending: true,
      isError: false,
      isEmpty: false,
      tasks: [],
      refetch: vi.fn(),
    } as unknown as ReturnType<typeof useTasks>)

    renderTaskList()

    expect(screen.getByRole('status', { name: /loading tasks/i })).toBeInTheDocument()
    expect(screen.getAllByTestId('task-skeleton-row')).toHaveLength(3)
  })

  it('renders an empty state when there are no tasks', () => {
    mockedUseTasks.mockReturnValue({
      isPending: false,
      isSuccess: true,
      isError: false,
      isEmpty: true,
      tasks: [],
      refetch: vi.fn(),
    } as unknown as ReturnType<typeof useTasks>)

    renderTaskList()

    expect(screen.getByText(/no tasks yet/i)).toBeInTheDocument()
    expect(
      screen.getByText(/click new task to create your first one/i),
    ).toBeInTheDocument()
  })

  it('renders an error alert with retry action', () => {
    const refetch = vi.fn()
    mockedUseTasks.mockReturnValue({
      isPending: false,
      isError: true,
      isEmpty: false,
      tasks: [],
      error: new ApiError({ status: 500, message: 'Failed to load tasks' }),
      refetch,
    } as unknown as ReturnType<typeof useTasks>)

    renderTaskList()

    expect(screen.getByRole('alert')).toHaveTextContent('Failed to load tasks')
    fireEvent.click(screen.getByRole('button', { name: /retry/i }))
    expect(refetch).toHaveBeenCalled()
  })

  it('renders loaded task rows with key fields', () => {
    mockedUseTasks.mockReturnValue({
      isPending: false,
      isSuccess: true,
      isError: false,
      isEmpty: false,
      tasks: [sampleTask],
      refetch: vi.fn(),
    } as unknown as ReturnType<typeof useTasks>)

    renderTaskList()

    expect(screen.getByText('Write report')).toBeInTheDocument()
    expect(screen.getByText('HIGH')).toBeInTheDocument()
    expect(screen.getByText('OPEN')).toBeInTheDocument()
    expect(screen.getByText('2026-06-15')).toBeInTheDocument()
    expect(screen.getByText('60 min')).toBeInTheDocument()
  })
})
