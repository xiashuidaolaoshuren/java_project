import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'

import { DailyPlanView } from '@/features/plans/DailyPlanView'
import type { DailyPlanResponse } from '@/types/api'

const samplePlan: DailyPlanResponse = {
  id: 1,
  planDate: '2026-06-15',
  createdAt: '2026-06-15T09:00:00Z',
  availableMinutes: null,
  warning: null,
  items: [
    {
      position: 2,
      task: {
        id: 11,
        title: 'Review pull requests',
        description: null,
        priority: 'MEDIUM',
        status: 'OPEN',
        dueDate: '2026-06-15',
        estimatedMinutes: 30,
      },
    },
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

const warningPlan: DailyPlanResponse = {
  id: 2,
  planDate: '2026-06-16',
  createdAt: '2026-06-16T09:00:00Z',
  availableMinutes: 30,
  warning: {
    minimumAvailableMinutes: 60,
    estimatedTasks: [{ taskId: 10, title: 'Write tests', estimatedMinutes: 60 }],
    unestimatedTasks: [{ taskId: 11, title: 'Tidy up' }],
  },
  items: [],
}

describe('DailyPlanView', () => {
  it('renders shortfall alert when the plan has a warning', () => {
    render(<DailyPlanView plan={warningPlan} />)

    const alert = screen.getByRole('alert')
    expect(alert).toHaveTextContent(/plan needs more focus time/i)
    expect(alert).toHaveTextContent(/at least 60 min/i)
    expect(alert).toHaveTextContent('Write tests — 60 min')
    expect(alert).toHaveTextContent('Tidy up — unknown duration')
  })

  it('renders no alert when the plan warning is null', () => {
    render(<DailyPlanView plan={samplePlan} />)

    expect(screen.getByText(/today's plan/i)).toBeInTheDocument()
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  })

  it('renders plan items in position order with task title and focus minutes', () => {
    render(<DailyPlanView plan={samplePlan} />)

    expect(screen.getByText(/today's plan/i)).toBeInTheDocument()

    const items = screen.getAllByRole('listitem')
    expect(items).toHaveLength(2)
    expect(items[0]).toHaveTextContent('Write tests')
    expect(items[0]).toHaveTextContent('45 min')
    expect(items[0]).toHaveTextContent('High')
    expect(items[0]).toHaveTextContent('Open')
    expect(items[0].querySelector('[data-priority="HIGH"]')).toBeInTheDocument()
    expect(items[0].querySelector('[data-status="OPEN"]')).toBeInTheDocument()
    expect(items[0].querySelector('[data-meta="priority"]')).toBeInTheDocument()
    expect(items[0].querySelector('[data-meta="status"]')).toBeInTheDocument()
    expect(items[0].querySelector('[data-meta="estimatedMinutes"]')).toBeInTheDocument()
    expect(items[1]).toHaveTextContent('Review pull requests')
    expect(items[1]).toHaveTextContent('30 min')
    expect(items[1]).toHaveTextContent('Medium')
    expect(items[1]).toHaveTextContent('Open')
    expect(items[1].querySelector('[data-priority="MEDIUM"]')).toBeInTheDocument()
    expect(items[1].querySelector('[data-status="OPEN"]')).toBeInTheDocument()
    expect(items[1].querySelector('[data-meta="priority"]')).toBeInTheDocument()
    expect(items[1].querySelector('[data-meta="status"]')).toBeInTheDocument()
    expect(items[1].querySelector('[data-meta="estimatedMinutes"]')).toBeInTheDocument()

    for (const item of items) {
      expect(item).toHaveClass('flex-wrap')
      const title = item.querySelector('.font-medium')
      expect(title).toHaveClass('min-w-0')
      const metaCluster = item.querySelector('[data-meta="priority"]')?.parentElement
      expect(metaCluster).toBeInTheDocument()
      expect(metaCluster).not.toHaveClass('shrink-0')
      expect(metaCluster).toHaveClass('min-w-0')
      expect(metaCluster).toHaveClass('justify-end')
    }
  })

  it('shows empty fallback when no plan is available', () => {
    render(<DailyPlanView plan={null} />)

    expect(
      screen.getByText(/no plan for today yet/i),
    ).toBeInTheDocument()
  })

  it('shows a skeleton card while loading', () => {
    render(<DailyPlanView plan={null} isPending />)

    expect(screen.getByRole('status', { name: /loading plan/i })).toBeInTheDocument()
    expect(screen.queryByText(/no plan for today yet/i)).not.toBeInTheDocument()
  })

  it('renders error alert with retry when isError', () => {
    const onRetry = vi.fn()
    render(<DailyPlanView plan={null} isError onRetry={onRetry} />)

    expect(screen.getByRole('alert')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: /retry/i }))
    expect(onRetry).toHaveBeenCalled()
  })
})
