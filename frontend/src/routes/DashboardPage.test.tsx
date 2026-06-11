import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'

import { DashboardPage } from '@/routes/DashboardPage'

vi.mock('@/features/tasks/hooks', () => ({
  useTasks: vi.fn(),
  useCreateTask: vi.fn(),
}))

import { useCreateTask, useTasks } from '@/features/tasks/hooks'

const mockedUseTasks = vi.mocked(useTasks)
const mockedUseCreateTask = vi.mocked(useCreateTask)

function renderDashboard() {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  })

  return render(
    <QueryClientProvider client={queryClient}>
      <DashboardPage />
    </QueryClientProvider>,
  )
}

function mockEmptyTasks() {
  mockedUseTasks.mockReturnValue({
    isPending: false,
    isError: false,
    isEmpty: true,
    tasks: [],
    refetch: vi.fn(),
  } as unknown as ReturnType<typeof useTasks>)
}

describe('DashboardPage', () => {
  it('shows actionable empty-state CTA copy', () => {
    mockEmptyTasks()
    mockedUseCreateTask.mockReturnValue({
      mutate: vi.fn(),
      isPending: false,
      isError: false,
      error: null,
    } as unknown as ReturnType<typeof useCreateTask>)

    renderDashboard()

    expect(
      screen.getByText(/click new task to create your first one/i),
    ).toBeInTheDocument()
  })

  it('opens the create-task sheet when New Task is clicked', async () => {
    mockEmptyTasks()
    mockedUseCreateTask.mockReturnValue({
      mutate: vi.fn(),
      isPending: false,
      isError: false,
      error: null,
    } as unknown as ReturnType<typeof useCreateTask>)

    renderDashboard()

    fireEvent.click(screen.getByRole('button', { name: /new task/i }))

    await waitFor(() => {
      expect(screen.getByRole('dialog')).toBeInTheDocument()
    })
    expect(
      screen.getByRole('heading', { name: /new task/i }),
    ).toBeInTheDocument()
  })

  it('closes the sheet after a successful create', async () => {
    mockEmptyTasks()
    const mutate = vi.fn(
      (_payload: unknown, options?: { onSuccess?: () => void }) => {
        options?.onSuccess?.()
      },
    )
    mockedUseCreateTask.mockReturnValue({
      mutate,
      isPending: false,
      isError: false,
      error: null,
    } as unknown as ReturnType<typeof useCreateTask>)

    renderDashboard()

    fireEvent.click(screen.getByRole('button', { name: /new task/i }))

    await waitFor(() => {
      expect(screen.getByRole('dialog')).toBeInTheDocument()
    })

    fireEvent.change(screen.getByLabelText(/^title$/i), {
      target: { value: 'Write tests' },
    })
    fireEvent.click(screen.getByRole('button', { name: /create task/i }))

    await waitFor(() => {
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    })
    expect(mutate).toHaveBeenCalled()
  })
})
