import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { renderHook, waitFor } from '@testing-library/react'
import type { ReactNode } from 'react'
import { afterEach, describe, expect, it, vi } from 'vitest'

import type { DailyPlanResponse } from '@/types/api'

import {
  planDetailQueryKey,
  plansQueryKey,
  todayPlanQueryKey,
  useDeletePlan,
  useGeneratePlan,
  usePlan,
  usePlans,
  useTodayPlan,
} from '@/features/plans/hooks'

vi.mock('@/features/plans/api', () => ({
  deletePlan: vi.fn(),
  generateDailyPlan: vi.fn(),
  getPlanById: vi.fn(),
  getTodayPlan: vi.fn(),
  listPlans: vi.fn(),
}))

import {
  deletePlan,
  generateDailyPlan,
  getPlanById,
  getTodayPlan,
  listPlans,
} from '@/features/plans/api'

const mockedDeletePlan = vi.mocked(deletePlan)

const mockedGenerateDailyPlan = vi.mocked(generateDailyPlan)
const mockedGetPlanById = vi.mocked(getPlanById)
const mockedGetTodayPlan = vi.mocked(getTodayPlan)
const mockedListPlans = vi.mocked(listPlans)

function createWrapper(queryClient?: QueryClient) {
  const client =
    queryClient ??
    new QueryClient({
      defaultOptions: {
        queries: { retry: false },
        mutations: { retry: false },
      },
    })

  return function Wrapper({ children }: { children: ReactNode }) {
    return (
      <QueryClientProvider client={client}>{children}</QueryClientProvider>
    )
  }
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

describe('useGeneratePlan', () => {
  afterEach(() => {
    vi.clearAllMocks()
  })

  it('calls generateDailyPlan and refreshes today-plan query on success', async () => {
    mockedGenerateDailyPlan.mockResolvedValue(samplePlan)

    const queryClient = new QueryClient({
      defaultOptions: {
        queries: { retry: false },
        mutations: { retry: false },
      },
    })
    const invalidateSpy = vi.spyOn(queryClient, 'invalidateQueries')
    const setQueryDataSpy = vi.spyOn(queryClient, 'setQueryData')

    const { result } = renderHook(() => useGeneratePlan(), {
      wrapper: createWrapper(queryClient),
    })

    result.current.mutate({ availableMinutes: 120 })

    await waitFor(() => expect(result.current.isSuccess).toBe(true))

    expect(mockedGenerateDailyPlan).toHaveBeenCalledWith({
      availableMinutes: 120,
    })
    expect(setQueryDataSpy).toHaveBeenCalledWith(
      todayPlanQueryKey,
      samplePlan,
    )
    expect(invalidateSpy).toHaveBeenCalledWith({
      queryKey: todayPlanQueryKey,
    })
    expect(invalidateSpy).toHaveBeenCalledWith({
      queryKey: plansQueryKey,
    })
  })
})

describe('useTodayPlan', () => {
  afterEach(() => {
    vi.clearAllMocks()
  })

  it('uses a stable query key for today plan', () => {
    expect(todayPlanQueryKey).toEqual(['plans', 'today'])
  })

  it('returns today plan when the query succeeds', async () => {
    mockedGetTodayPlan.mockResolvedValue(samplePlan)

    const { result } = renderHook(() => useTodayPlan(), {
      wrapper: createWrapper(),
    })

    await waitFor(() => expect(result.current.isSuccess).toBe(true))

    expect(mockedGetTodayPlan).toHaveBeenCalled()
    expect(result.current.plan).toEqual(samplePlan)
    expect(result.current.hasPlan).toBe(true)
  })

  it('returns null plan state when no plan exists for today', async () => {
    mockedGetTodayPlan.mockResolvedValue(null)

    const { result } = renderHook(() => useTodayPlan(), {
      wrapper: createWrapper(),
    })

    await waitFor(() => expect(result.current.isSuccess).toBe(true))

    expect(result.current.plan).toBeNull()
    expect(result.current.hasPlan).toBe(false)
  })
})

describe('usePlans', () => {
  afterEach(() => {
    vi.clearAllMocks()
  })

  it('fetches plans on mount and exposes plans and isEmpty', async () => {
    mockedListPlans.mockResolvedValue([samplePlan])

    const { result } = renderHook(() => usePlans(), {
      wrapper: createWrapper(),
    })

    await waitFor(() => expect(result.current.isSuccess).toBe(true))

    expect(mockedListPlans).toHaveBeenCalled()
    expect(result.current.plans).toEqual([samplePlan])
    expect(result.current.isEmpty).toBe(false)
  })

  it('exposes isEmpty when no plans are returned', async () => {
    mockedListPlans.mockResolvedValue([])

    const { result } = renderHook(() => usePlans(), {
      wrapper: createWrapper(),
    })

    await waitFor(() => expect(result.current.isSuccess).toBe(true))

    expect(result.current.plans).toEqual([])
    expect(result.current.isEmpty).toBe(true)
  })
})

describe('usePlan', () => {
  afterEach(() => {
    vi.clearAllMocks()
  })

  it('fetches plan detail by id and exposes plan', async () => {
    mockedGetPlanById.mockResolvedValue(samplePlan)

    const { result } = renderHook(() => usePlan(1), {
      wrapper: createWrapper(),
    })

    await waitFor(() => expect(result.current.isSuccess).toBe(true))

    expect(mockedGetPlanById).toHaveBeenCalledWith(1)
    expect(result.current.plan).toEqual(samplePlan)
  })

  it('does not fetch when id is invalid', () => {
    renderHook(() => usePlan(Number.NaN), {
      wrapper: createWrapper(),
    })

    expect(mockedGetPlanById).not.toHaveBeenCalled()
  })
})

describe('useDeletePlan', () => {
  afterEach(() => {
    vi.clearAllMocks()
  })

  it('calls deletePlan and invalidates plan queries on success', async () => {
    mockedDeletePlan.mockResolvedValue(undefined)

    const queryClient = new QueryClient({
      defaultOptions: {
        queries: { retry: false },
        mutations: { retry: false },
      },
    })
    const invalidateSpy = vi.spyOn(queryClient, 'invalidateQueries')

    const { result } = renderHook(() => useDeletePlan(), {
      wrapper: createWrapper(queryClient),
    })

    result.current.mutate(1)

    await waitFor(() => expect(result.current.isSuccess).toBe(true))

    expect(mockedDeletePlan).toHaveBeenCalledWith(1)
    expect(invalidateSpy).toHaveBeenCalledWith({
      queryKey: todayPlanQueryKey,
    })
    expect(invalidateSpy).toHaveBeenCalledWith({
      queryKey: plansQueryKey,
    })
    expect(invalidateSpy).toHaveBeenCalledWith({
      queryKey: planDetailQueryKey(1),
    })
  })
})
