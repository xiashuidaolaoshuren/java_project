import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'

import { DashboardPage } from '@/routes/DashboardPage'

vi.mock('@/features/tasks/hooks', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@/features/tasks/hooks')>()
  return {
    ...actual,
    useTasks: vi.fn(),
    useCreateTask: vi.fn(),
    useUpdateTask: vi.fn(),
    useDeleteTask: vi.fn(),
  }
})

vi.mock('@/features/plans/hooks', () => ({
  useTodayPlan: vi.fn(),
  useGeneratePlan: vi.fn(),
}))

vi.mock('sonner', () => ({
  toast: {
    success: vi.fn(),
  },
}))

import { useCreateTask, useDeleteTask, useTasks, useUpdateTask } from '@/features/tasks/hooks'
import { useGeneratePlan, useTodayPlan } from '@/features/plans/hooks'
import type { DailyPlanResponse } from '@/types/api'

const mockedUseTasks = vi.mocked(useTasks)
const mockedUseCreateTask = vi.mocked(useCreateTask)
const mockedUseUpdateTask = vi.mocked(useUpdateTask)
const mockedUseDeleteTask = vi.mocked(useDeleteTask)
const mockedUseTodayPlan = vi.mocked(useTodayPlan)
const mockedUseGeneratePlan = vi.mocked(useGeneratePlan)

const sampleTask = {
  id: 1,
  title: 'Write report',
  description: 'Quarterly summary',
  priority: 'HIGH' as const,
  status: 'OPEN' as const,
  dueDate: '2026-06-15',
  estimatedMinutes: 60,
}

const samplePlan: DailyPlanResponse = {
  id: 1,
  planDate: '2026-06-15',
  createdAt: '2026-06-15T09:00:00Z',
  availableMinutes: null,
  warning: null,
  items: [
    {
      position: 1,
      task: {
        id: 10,
        title: 'Write tests',
        description: null,
        priority: 'HIGH',
        status: 'OPEN',
        dueDate: '2026-06-15',
        estimatedMinutes: 45,
      },
    },
  ],
}

function renderDashboard() {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  })

  const view = render(
    <QueryClientProvider client={queryClient}>
      <DashboardPage />
    </QueryClientProvider>,
  )

  return {
    ...view,
    rerenderDashboard: () =>
      view.rerender(
        <QueryClientProvider client={queryClient}>
          <DashboardPage />
        </QueryClientProvider>,
      ),
  }
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

function mockNoPlan() {
  mockedUseTodayPlan.mockReturnValue({
    isPending: false,
    isError: false,
    plan: null,
    hasPlan: false,
    refetch: vi.fn(),
  } as unknown as ReturnType<typeof useTodayPlan>)
  mockedUseGeneratePlan.mockReturnValue({
    mutate: vi.fn(),
    isPending: false,
    isError: false,
    error: null,
  } as unknown as ReturnType<typeof useGeneratePlan>)
}

function mockPendingPlan() {
  mockedUseTodayPlan.mockReturnValue({
    isPending: true,
    isError: false,
    plan: null,
    hasPlan: false,
    refetch: vi.fn(),
  } as unknown as ReturnType<typeof useTodayPlan>)
  mockedUseGeneratePlan.mockReturnValue({
    mutate: vi.fn(),
    isPending: false,
    isError: false,
    error: null,
  } as unknown as ReturnType<typeof useGeneratePlan>)
}

function mockPlanError() {
  mockedUseTodayPlan.mockReturnValue({
    isPending: false,
    isError: true,
    plan: null,
    hasPlan: false,
    refetch: vi.fn(),
  } as unknown as ReturnType<typeof useTodayPlan>)
  mockedUseGeneratePlan.mockReturnValue({
    mutate: vi.fn(),
    isPending: false,
    isError: false,
    error: null,
  } as unknown as ReturnType<typeof useGeneratePlan>)
}

function mockExistingPlan() {
  mockedUseTodayPlan.mockReturnValue({
    isPending: false,
    isError: false,
    plan: samplePlan,
    hasPlan: true,
    refetch: vi.fn(),
  } as unknown as ReturnType<typeof useTodayPlan>)
  mockedUseGeneratePlan.mockReturnValue({
    mutate: vi.fn(),
    isPending: false,
    isError: false,
    error: null,
  } as unknown as ReturnType<typeof useGeneratePlan>)
}

describe('DashboardPage', () => {
  it('shows actionable empty-state CTA copy', () => {
    mockEmptyTasks()
    mockMutations()
    mockNoPlan()

    renderDashboard()

    expect(
      screen.getByText(/click new task to create your first one/i),
    ).toBeInTheDocument()
  })

  it('opens the create-task sheet when New Task is clicked', async () => {
    mockEmptyTasks()
    mockMutations()
    mockNoPlan()

    renderDashboard()

    fireEvent.click(screen.getAllByRole('button', { name: /^new task$/i })[0]!)

    await waitFor(() => {
      expect(screen.getByRole('dialog')).toBeInTheDocument()
    })
    expect(
      screen.getByRole('heading', { name: /new task/i }),
    ).toBeInTheDocument()
  })

  it('opens the create-task sheet when empty-state New Task CTA is clicked', async () => {
    mockEmptyTasks()
    mockMutations()
    mockNoPlan()

    renderDashboard()

    const newTaskButtons = screen.getAllByRole('button', { name: /^new task$/i })
    fireEvent.click(newTaskButtons[newTaskButtons.length - 1]!)

    await waitFor(() => {
      expect(screen.getByRole('dialog')).toBeInTheDocument()
    })
    expect(
      screen.getByRole('heading', { name: /new task/i }),
    ).toBeInTheDocument()
  })

  it('closes the sheet after a successful create', async () => {
    mockEmptyTasks()
    mockNoPlan()
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

    fireEvent.click(screen.getAllByRole('button', { name: /^new task$/i })[0]!)

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
    mockNoPlan()

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
    mockNoPlan()
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
    mockNoPlan()
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

  it('displays existing today plan on page load', () => {
    mockEmptyTasks()
    mockMutations()
    mockExistingPlan()

    renderDashboard()

    expect(screen.getByText('Write tests')).toBeInTheDocument()
    expect(screen.getByText('45 min')).toBeInTheDocument()
  })

  it('shows plan skeleton while today plan is loading', () => {
    mockEmptyTasks()
    mockMutations()
    mockPendingPlan()

    renderDashboard()

    expect(screen.getByRole('status', { name: /loading plan/i })).toBeInTheDocument()
    expect(screen.queryByText(/no plan for today yet/i)).not.toBeInTheDocument()
  })

  it('shows plan error alert when today plan fails to load', () => {
    mockEmptyTasks()
    mockMutations()
    mockPlanError()

    renderDashboard()

    expect(screen.getByRole('alert')).toHaveTextContent(/could not load plan/i)
  })

  it('updates displayed plan after successful generate', () => {
    mockEmptyTasks()
    mockMutations()
    mockedUseTodayPlan.mockReturnValue({
      isPending: false,
      plan: null,
      hasPlan: false,
    } as unknown as ReturnType<typeof useTodayPlan>)

    const mutate = vi.fn(
      (_payload: unknown, options?: { onSuccess?: () => void }) => {
        mockedUseTodayPlan.mockReturnValue({
          isPending: false,
          plan: samplePlan,
          hasPlan: true,
        } as unknown as ReturnType<typeof useTodayPlan>)
        options?.onSuccess?.()
      },
    )
    mockedUseGeneratePlan.mockReturnValue({
      mutate,
      isPending: false,
      isError: false,
      error: null,
    } as unknown as ReturnType<typeof useGeneratePlan>)

    const { rerenderDashboard } = renderDashboard()

    expect(screen.getByText(/no plan for today yet/i)).toBeInTheDocument()

    fireEvent.change(screen.getByLabelText(/available focus time/i), {
      target: { value: '90' },
    })
    fireEvent.click(
      screen.getByRole('button', { name: /generate today's plan/i }),
    )

    rerenderDashboard()

    expect(screen.getByText('Write tests')).toBeInTheDocument()
    expect(mutate).toHaveBeenCalledWith(
      { availableMinutes: 90 },
      expect.objectContaining({
        onSuccess: expect.any(Function),
      }),
    )
  })
})
