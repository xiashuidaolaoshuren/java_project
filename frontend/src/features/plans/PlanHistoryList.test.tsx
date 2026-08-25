import type { ComponentProps } from 'react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { PlanHistoryList } from '@/features/plans/PlanHistoryList'
import type { DailyPlanResponse } from '@/types/api'

vi.mock('@/features/plans/hooks', () => ({
  useDeletePlan: vi.fn(),
}))

import { useDeletePlan } from '@/features/plans/hooks'

const mockedUseDeletePlan = vi.mocked(useDeletePlan)

const samplePlans: DailyPlanResponse[] = [
  {
    id: 1,
    planDate: '2026-06-14',
    createdAt: '2026-06-14T09:00:00Z',
    availableMinutes: 30,
    warning: {
      minimumAvailableMinutes: 60,
      estimatedTasks: [
        { taskId: 10, title: 'Write tests', estimatedMinutes: 60 },
      ],
      unestimatedTasks: [],
    },
    items: [
      {
        position: 1,
        task: {
          id: 10,
          title: 'Write tests',
          description: null,
          priority: 'HIGH',
          status: 'OPEN',
          dueDate: '2026-06-14',
          estimatedMinutes: 45,
        },
      },
    ],
  },
  {
    id: 2,
    planDate: '2026-06-15',
    createdAt: '2026-06-15T09:00:00Z',
    availableMinutes: null,
    warning: null,
    items: [],
  },
]

function renderPlanHistoryList(
  props: Partial<ComponentProps<typeof PlanHistoryList>> = {},
) {
  const defaultProps = {
    plans: samplePlans,
    isLoading: false,
    isError: false,
    onRetry: vi.fn(),
  }

  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })

  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <PlanHistoryList {...defaultProps} {...props} />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('PlanHistoryList', () => {
  beforeEach(() => {
    mockedUseDeletePlan.mockReturnValue({
      mutate: vi.fn(),
      isPending: false,
    } as unknown as ReturnType<typeof useDeletePlan>)
  })
  it('renders a link per plan showing plan date and item count', () => {
    renderPlanHistoryList()

    const links = screen.getAllByRole('link')
    expect(links).toHaveLength(2)
    expect(links[0]).toHaveAttribute('href', '/plans/1')
    expect(links[0]).toHaveTextContent('2026-06-14')
    expect(links[0]).toHaveTextContent('1 block')
    expect(links[1]).toHaveAttribute('href', '/plans/2')
    expect(links[1]).toHaveTextContent('2026-06-15')
    expect(links[1]).toHaveTextContent('0 blocks')
  })

  it('shows a Shortfall indicator only for plans with a warning', () => {
    renderPlanHistoryList()

    const links = screen.getAllByRole('link')
    expect(links[0]).toHaveTextContent(/shortfall/i)
    expect(links[1]).not.toHaveTextContent(/shortfall/i)
  })

  it('renders empty-state message when plans is empty', () => {
    renderPlanHistoryList({ plans: [] })

    expect(screen.getByText(/no saved plans yet/i)).toBeInTheDocument()
  })

  it('renders empty-state CTA link to dashboard', () => {
    renderPlanHistoryList({ plans: [] })

    const dashboardLink = screen.getByRole('link', { name: /go to dashboard/i })
    expect(dashboardLink).toHaveAttribute('href', '/dashboard')
  })

  it('renders error alert with retry when isError', () => {
    const onRetry = vi.fn()
    renderPlanHistoryList({ isError: true, onRetry })

    expect(screen.getByRole('alert')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: /retry/i }))
    expect(onRetry).toHaveBeenCalled()
  })

  it('calls delete mutation only after confirmation', async () => {
    const deleteMutate = vi.fn()
    mockedUseDeletePlan.mockReturnValue({
      mutate: deleteMutate,
      isPending: false,
    } as unknown as ReturnType<typeof useDeletePlan>)

    renderPlanHistoryList()

    fireEvent.click(
      screen.getByRole('button', { name: /delete plan for 2026-06-14/i }),
    )

    expect(deleteMutate).not.toHaveBeenCalled()
    expect(
      screen.getByRole('alertdialog', { name: /delete plan/i }),
    ).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: /^delete plan$/i }))
    expect(deleteMutate).toHaveBeenCalledWith(
      1,
      expect.objectContaining({ onSuccess: expect.any(Function) }),
    )
  })
})
