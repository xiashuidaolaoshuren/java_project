import type { ComponentProps } from 'react'
import { fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'

import { PlanHistoryList } from '@/features/plans/PlanHistoryList'
import type { DailyPlanResponse } from '@/types/api'

const samplePlans: DailyPlanResponse[] = [
  {
    id: 1,
    planDate: '2026-06-14',
    createdAt: '2026-06-14T09:00:00Z',
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

  return render(
    <MemoryRouter>
      <PlanHistoryList {...defaultProps} {...props} />
    </MemoryRouter>,
  )
}

describe('PlanHistoryList', () => {
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

  it('renders empty-state message when plans is empty', () => {
    renderPlanHistoryList({ plans: [] })

    expect(screen.getByText(/no saved plans yet/i)).toBeInTheDocument()
  })

  it('renders error alert with retry when isError', () => {
    const onRetry = vi.fn()
    renderPlanHistoryList({ isError: true, onRetry })

    expect(screen.getByRole('alert')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: /retry/i }))
    expect(onRetry).toHaveBeenCalled()
  })
})
