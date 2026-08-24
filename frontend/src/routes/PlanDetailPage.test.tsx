import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { PlanDetailPage } from '@/routes/PlanDetailPage'
import type { DailyPlanResponse } from '@/types/api'

vi.mock('@/features/plans/hooks', () => ({
  usePlan: vi.fn(),
  useDeletePlan: vi.fn(),
}))

vi.mock('sonner', () => ({
  toast: {
    success: vi.fn(),
  },
}))

import { useDeletePlan, usePlan } from '@/features/plans/hooks'

const mockedUsePlan = vi.mocked(usePlan)
const mockedUseDeletePlan = vi.mocked(useDeletePlan)

const samplePlan: DailyPlanResponse = {
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
}

function renderPlanDetailPage() {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  })

  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={['/plans/1']}>
        <Routes>
          <Route path="/plans" element={<h1>Plan history</h1>} />
          <Route path="/plans/:id" element={<PlanDetailPage />} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('PlanDetailPage', () => {
  beforeEach(() => {
    mockedUsePlan.mockReturnValue({
      plan: samplePlan,
      isPending: false,
      isError: false,
      error: null,
      refetch: vi.fn(),
    } as unknown as ReturnType<typeof usePlan>)
    mockedUseDeletePlan.mockReturnValue({
      mutate: vi.fn(),
      isPending: false,
    } as unknown as ReturnType<typeof useDeletePlan>)
  })

  it('deletes plan after confirmation and navigates to plan history', async () => {
    const deleteMutate = vi.fn(
      (_id: number, options?: { onSuccess?: () => void }) => {
        options?.onSuccess?.()
      },
    )
    mockedUseDeletePlan.mockReturnValue({
      mutate: deleteMutate,
      isPending: false,
    } as unknown as ReturnType<typeof useDeletePlan>)

    renderPlanDetailPage()

    fireEvent.click(screen.getByRole('button', { name: /^delete plan$/i }))

    expect(deleteMutate).not.toHaveBeenCalled()
    expect(
      screen.getByRole('alertdialog', { name: /delete plan/i }),
    ).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: /^delete plan$/i }))

    expect(deleteMutate).toHaveBeenCalledWith(
      1,
      expect.objectContaining({ onSuccess: expect.any(Function) }),
    )

    await waitFor(() => {
      expect(
        screen.getByRole('heading', { name: /plan history/i }),
      ).toBeInTheDocument()
    })
  })
})
