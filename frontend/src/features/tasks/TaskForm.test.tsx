import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'

import { TaskForm, TaskFormSubmitButton } from '@/features/tasks/TaskForm'
import { ApiError } from '@/lib/api'
import type { TaskResponse } from '@/types/api'

vi.mock('@/features/tasks/hooks', () => ({
  useCreateTask: vi.fn(),
  useUpdateTask: vi.fn(),
}))

import { useCreateTask, useUpdateTask } from '@/features/tasks/hooks'

const mockedUseCreateTask = vi.mocked(useCreateTask)
const mockedUseUpdateTask = vi.mocked(useUpdateTask)

function renderTaskForm(onSuccess = vi.fn(), task?: TaskResponse) {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  })

  const view = render(
    <QueryClientProvider client={queryClient}>
      <TaskForm onSuccess={onSuccess} task={task} />
      <TaskFormSubmitButton isEditMode={task != null} />
    </QueryClientProvider>,
  )

  return {
    ...view,
    onSuccess,
    rerenderForm: (nextOnSuccess = onSuccess, nextTask = task) =>
      view.rerender(
        <QueryClientProvider client={queryClient}>
          <TaskForm onSuccess={nextOnSuccess} task={nextTask} />
          <TaskFormSubmitButton isEditMode={nextTask != null} />
        </QueryClientProvider>,
      ),
  }
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

describe('TaskForm', () => {
  it('renders fields, submits create payload, and disables submit while pending', async () => {
    const mutate = vi.fn()
    mockedUseCreateTask.mockReturnValue({
      mutate,
      isPending: false,
      isError: false,
      error: null,
    } as unknown as ReturnType<typeof useCreateTask>)

    const { rerenderForm } = renderTaskForm()

    expect(screen.getByLabelText(/^title$/i)).toBeInTheDocument()
    expect(screen.getByLabelText(/description/i)).toBeInTheDocument()
    expect(screen.getByRole('combobox', { name: /priority/i })).toBeInTheDocument()
    expect(screen.getByLabelText(/due date/i)).toBeInTheDocument()
    expect(screen.getByLabelText(/estimated minutes/i)).toBeInTheDocument()
    expect(
      screen.getByRole('button', { name: /create task/i }),
    ).toBeInTheDocument()

    fireEvent.change(screen.getByLabelText(/^title$/i), {
      target: { value: 'Write tests' },
    })
    fireEvent.change(screen.getByLabelText(/description/i), {
      target: { value: 'TDD coverage' },
    })
    fireEvent.click(screen.getByRole('combobox', { name: /priority/i }))
    const highOption = await screen.findByRole('option', { name: /^high$/i })
    fireEvent.pointerDown(highOption)
    fireEvent.click(highOption)
    fireEvent.change(screen.getByLabelText(/due date/i), {
      target: { value: '2026-06-01' },
    })
    fireEvent.change(screen.getByLabelText(/estimated minutes/i), {
      target: { value: '45' },
    })
    fireEvent.click(screen.getByRole('button', { name: /create task/i }))

    expect(mutate).toHaveBeenCalledWith(
      {
        title: 'Write tests',
        description: 'TDD coverage',
        priority: 'HIGH',
        dueDate: '2026-06-01',
        estimatedMinutes: 45,
      },
      expect.objectContaining({
        onSuccess: expect.any(Function),
      }),
    )

    mockedUseCreateTask.mockReturnValue({
      mutate,
      isPending: true,
      isError: false,
      error: null,
    } as unknown as ReturnType<typeof useCreateTask>)

    rerenderForm()

    expect(screen.getByRole('button', { name: /create task/i })).toBeDisabled()
  })

  it('shows inline field errors from ApiError details', () => {
    mockedUseCreateTask.mockReturnValue({
      mutate: vi.fn(),
      isPending: false,
      isError: true,
      error: new ApiError({
        status: 400,
        message: 'Validation failed',
        details: { title: ['must not be blank'] },
      }),
    } as unknown as ReturnType<typeof useCreateTask>)

    renderTaskForm()

    expect(screen.getByText('must not be blank')).toBeInTheDocument()
  })

  it('links field errors via aria-describedby', () => {
    mockedUseCreateTask.mockReturnValue({
      mutate: vi.fn(),
      isPending: false,
      isError: true,
      error: new ApiError({
        status: 400,
        message: 'Validation failed',
        details: {
          title: ['must not be blank'],
          description: ['must not exceed 500 characters'],
          dueDate: ['must be a valid date'],
          estimatedMinutes: ['must be positive'],
        },
      }),
    } as unknown as ReturnType<typeof useCreateTask>)

    renderTaskForm()

    expect(screen.getByLabelText(/^title$/i)).toHaveAttribute(
      'aria-describedby',
      'title-error',
    )
    expect(screen.getByText('must not be blank')).toHaveAttribute(
      'id',
      'title-error',
    )
    expect(screen.getByLabelText(/description/i)).toHaveAttribute(
      'aria-describedby',
      'description-error',
    )
    expect(screen.getByText('must not exceed 500 characters')).toHaveAttribute(
      'id',
      'description-error',
    )
    expect(screen.getByLabelText(/due date/i)).toHaveAttribute(
      'aria-describedby',
      'dueDate-error',
    )
    expect(screen.getByText('must be a valid date')).toHaveAttribute(
      'id',
      'dueDate-error',
    )
    expect(screen.getByLabelText(/estimated minutes/i)).toHaveAttribute(
      'aria-describedby',
      'estimatedMinutes-error',
    )
    expect(screen.getByText('must be positive')).toHaveAttribute(
      'id',
      'estimatedMinutes-error',
    )
  })

  it('shows a top-level alert for non-field errors', () => {
    mockedUseCreateTask.mockReturnValue({
      mutate: vi.fn(),
      isPending: false,
      isError: true,
      error: new ApiError({
        status: 500,
        message: 'Unexpected error',
      }),
    } as unknown as ReturnType<typeof useCreateTask>)

    renderTaskForm()

    expect(screen.getByRole('alert')).toHaveTextContent('Unexpected error')
  })

  it('prefills fields in edit mode and submits update mutation with task id', async () => {
    const mutate = vi.fn()
    mockedUseUpdateTask.mockReturnValue({
      mutate,
      isPending: false,
      isError: false,
      error: null,
    } as unknown as ReturnType<typeof useUpdateTask>)

    renderTaskForm(vi.fn(), sampleTask)

    expect(screen.getByLabelText(/^title$/i)).toHaveValue('Write report')
    expect(screen.getByLabelText(/description/i)).toHaveValue('Quarterly summary')
    expect(screen.getByRole('combobox', { name: /priority/i })).toHaveTextContent(
      /high/i,
    )
    expect(screen.getByLabelText(/due date/i)).toHaveValue('2026-06-15')
    expect(screen.getByLabelText(/estimated minutes/i)).toHaveValue(60)
    expect(screen.getByRole('combobox', { name: /status/i })).toHaveTextContent(
      /open/i,
    )
    expect(
      screen.getByRole('button', { name: /save task/i }),
    ).toBeInTheDocument()

    fireEvent.change(screen.getByLabelText(/^title$/i), {
      target: { value: 'Updated report' },
    })
    fireEvent.click(screen.getByRole('combobox', { name: /status/i }))
    const inProgressOption = await screen.findByRole('option', { name: /in progress/i })
    fireEvent.pointerDown(inProgressOption)
    fireEvent.click(inProgressOption)
    fireEvent.click(screen.getByRole('button', { name: /save task/i }))

    expect(mutate).toHaveBeenCalledWith(
      {
        id: 1,
        request: {
          title: 'Updated report',
          description: 'Quarterly summary',
          priority: 'HIGH',
          status: 'IN_PROGRESS',
          dueDate: '2026-06-15',
          estimatedMinutes: 60,
        },
      },
      expect.objectContaining({
        onSuccess: expect.any(Function),
      }),
    )
  })
})
