import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'

import { DashboardPage } from '@/routes/DashboardPage'

vi.mock('@/features/tasks/hooks', () => ({
  useTasks: vi.fn(),
  useCreateTask: vi.fn(),
  useUpdateTask: vi.fn(),
  useDeleteTask: vi.fn(),
}))

import { useCreateTask, useDeleteTask, useTasks, useUpdateTask } from '@/features/tasks/hooks'

const mockedUseTasks = vi.mocked(useTasks)
const mockedUseCreateTask = vi.mocked(useCreateTask)
const mockedUseUpdateTask = vi.mocked(useUpdateTask)
const mockedUseDeleteTask = vi.mocked(useDeleteTask)

const sampleTask = {
  id: 1,
  title: 'Write report',
  description: 'Quarterly summary',
  priority: 'HIGH' as const,
  status: 'OPEN' as const,
  dueDate: '2026-06-15',
  estimatedMinutes: 60,
}

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

function mockLoadedTasks() {
  mockedUseTasks.mockReturnValue({
    isPending: false,
    isError: false,
    isEmpty: false,
    tasks: [sampleTask],
    refetch: vi.fn(),
  } as unknown as ReturnType<typeof useTasks>)
}

function mockMutations() {
  mockedUseCreateTask.mockReturnValue({
    mutate: vi.fn(),
    isPending: false,
    isError: false,
    error: null,
  } as unknown as ReturnType<typeof useCreateTask>)
  mockedUseUpdateTask.mockReturnValue({
    mutate: vi.fn(),
    isPending: false,
  } as unknown as ReturnType<typeof useUpdateTask>)
  mockedUseDeleteTask.mockReturnValue({
    mutate: vi.fn(),
    isPending: false,
  } as unknown as ReturnType<typeof useDeleteTask>)
}

describe('DashboardPage', () => {
  it('shows actionable empty-state CTA copy', () => {
    mockEmptyTasks()
    mockMutations()

    renderDashboard()

    expect(
      screen.getByText(/click new task to create your first one/i),
    ).toBeInTheDocument()
  })

  it('opens the create-task sheet when New Task is clicked', async () => {
    mockEmptyTasks()
    mockMutations()

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
    mockedUseUpdateTask.mockReturnValue({
      mutate: vi.fn(),
      isPending: false,
    } as unknown as ReturnType<typeof useUpdateTask>)
    mockedUseDeleteTask.mockReturnValue({
      mutate: vi.fn(),
      isPending: false,
    } as unknown as ReturnType<typeof useDeleteTask>)

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

  it('opens edit sheet with selected task when edit action is clicked', async () => {
    mockLoadedTasks()
    mockMutations()

    renderDashboard()

    fireEvent.click(screen.getByRole('button', { name: /task actions for write report/i }))
    fireEvent.click(await screen.findByRole('menuitem', { name: /edit/i }))

    await waitFor(() => {
      expect(screen.getByRole('dialog')).toBeInTheDocument()
    })
    expect(
      screen.getByRole('heading', { name: /edit task/i }),
    ).toBeInTheDocument()
    expect(screen.getByLabelText(/^title$/i)).toHaveValue('Write report')
  })

  it('triggers status update mutation from quick status control', () => {
    const updateMutate = vi.fn()
    mockLoadedTasks()
    mockMutations()
    mockedUseUpdateTask.mockReturnValue({
      mutate: updateMutate,
      isPending: false,
    } as unknown as ReturnType<typeof useUpdateTask>)

    renderDashboard()

    fireEvent.change(screen.getByLabelText(/change status for write report/i), {
      target: { value: 'IN_PROGRESS' },
    })

    expect(updateMutate).toHaveBeenCalledWith({
      id: 1,
      request: {
        title: 'Write report',
        description: 'Quarterly summary',
        priority: 'HIGH',
        status: 'IN_PROGRESS',
        dueDate: '2026-06-15',
        estimatedMinutes: 60,
      },
    })
  })

  it('deletes task only after confirmation dialog is accepted', async () => {
    const deleteMutate = vi.fn()
    mockLoadedTasks()
    mockMutations()
    mockedUseDeleteTask.mockReturnValue({
      mutate: deleteMutate,
      isPending: false,
    } as unknown as ReturnType<typeof useDeleteTask>)

    renderDashboard()

    fireEvent.click(screen.getByRole('button', { name: /task actions for write report/i }))
    fireEvent.click(await screen.findByRole('menuitem', { name: /delete/i }))

    expect(deleteMutate).not.toHaveBeenCalled()
    fireEvent.click(screen.getByRole('button', { name: /^delete task$/i }))
    expect(deleteMutate).toHaveBeenCalledWith(1)
  })
})
