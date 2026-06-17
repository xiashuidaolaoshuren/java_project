import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'

import { ApiError } from '@/lib/api'
import { GeneratePlanCard } from '@/features/plans/GeneratePlanCard'

vi.mock('@/features/plans/hooks', () => ({
  useGeneratePlan: vi.fn(),
}))

vi.mock('sonner', () => ({
  toast: {
    success: vi.fn(),
  },
}))

import { useGeneratePlan } from '@/features/plans/hooks'
import { toast } from 'sonner'

const mockedUseGeneratePlan = vi.mocked(useGeneratePlan)
const mockedToastSuccess = vi.mocked(toast.success)

function renderGeneratePlanCard() {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  })

  const view = render(
    <QueryClientProvider client={queryClient}>
      <GeneratePlanCard />
    </QueryClientProvider>,
  )

  return {
    ...view,
    rerenderCard: () =>
      view.rerender(
        <QueryClientProvider client={queryClient}>
          <GeneratePlanCard />
        </QueryClientProvider>,
      ),
  }
}

describe('GeneratePlanCard', () => {
  it('submits available minutes and disables generate while pending', () => {
    const mutate = vi.fn()
    mockedUseGeneratePlan.mockReturnValue({
      mutate,
      isPending: false,
      isError: false,
      error: null,
    } as unknown as ReturnType<typeof useGeneratePlan>)

    const { rerenderCard } = renderGeneratePlanCard()

    expect(
      screen.getByLabelText(/available focus time/i),
    ).toBeInTheDocument()

    fireEvent.change(screen.getByLabelText(/available focus time/i), {
      target: { value: '120' },
    })
    fireEvent.click(
      screen.getByRole('button', { name: /generate today's plan/i }),
    )

    expect(mutate).toHaveBeenCalledWith(
      { availableMinutes: 120 },
      expect.objectContaining({
        onSuccess: expect.any(Function),
      }),
    )

    mockedUseGeneratePlan.mockReturnValue({
      mutate,
      isPending: true,
      isError: false,
      error: null,
    } as unknown as ReturnType<typeof useGeneratePlan>)

    rerenderCard()

    expect(screen.getByRole('button', { name: /generating/i })).toBeDisabled()
  })

  it('shows inline provider error for 502 responses', () => {
    mockedUseGeneratePlan.mockReturnValue({
      mutate: vi.fn(),
      isPending: false,
      isError: true,
      error: new ApiError({
        status: 502,
        message: 'provider down',
      }),
    } as unknown as ReturnType<typeof useGeneratePlan>)

    renderGeneratePlanCard()

    expect(screen.getByRole('alert')).toHaveTextContent('provider down')
  })

  it('shows success toast after generate succeeds', () => {
    const mutate = vi.fn(
      (_payload: unknown, options?: { onSuccess?: () => void }) => {
        options?.onSuccess?.()
      },
    )
    mockedUseGeneratePlan.mockReturnValue({
      mutate,
      isPending: false,
      isError: false,
      error: null,
    } as unknown as ReturnType<typeof useGeneratePlan>)

    renderGeneratePlanCard()

    fireEvent.change(screen.getByLabelText(/available focus time/i), {
      target: { value: '90' },
    })
    fireEvent.click(
      screen.getByRole('button', { name: /generate today's plan/i }),
    )

    expect(mockedToastSuccess).toHaveBeenCalled()
  })
})
