import { afterEach, describe, expect, it, vi } from 'vitest'

import type {
  DailyPlanResponse,
  DailyPlanSummaryResponse,
  PageResponse,
} from '@/types/api'

import { ApiError } from '@/lib/api'
import {
  deletePlan,
  generateDailyPlan,
  getPlanById,
  getTodayPlan,
  listPlans,
} from '@/features/plans/api'

describe('plan summary types', () => {
  it('supports PageResponse<DailyPlanSummaryResponse> for listPlans', () => {
    const envelope: PageResponse<DailyPlanSummaryResponse> = {
      content: [
        {
          id: 1,
          planDate: '2026-06-14',
          createdAt: '2026-06-14T09:00:00Z',
          itemCount: 2,
          hasWarning: true,
          availableMinutes: 30,
        },
      ],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
    }

    expect(envelope.content[0]?.itemCount).toBe(2)
  })
})

describe('generateDailyPlan', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
    vi.useRealTimers()
  })

  it('generates a plan on success (201)', async () => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-06-15T10:00:00'))

    const generated: DailyPlanResponse = {
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
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify(generated), {
        status: 201,
        headers: { 'Content-Type': 'application/json' },
      }),
    )
    vi.stubGlobal('fetch', fetchMock)

    await expect(
      generateDailyPlan({ availableMinutes: 120 }),
    ).resolves.toEqual(generated)
    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining('/api/daily-plans/generate'),
      expect.objectContaining({
        method: 'POST',
        credentials: 'include',
        body: JSON.stringify({
          planDate: '2026-06-15',
          availableMinutes: 120,
        }),
      }),
    )
  })

  it('uses an explicit planDate when provided', async () => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-06-15T10:00:00'))

    const generated: DailyPlanResponse = {
      id: 2,
      planDate: '2026-06-16',
      createdAt: '2026-06-16T09:00:00Z',
      availableMinutes: null,
      warning: null,
      items: [],
    }
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify(generated), {
        status: 201,
        headers: { 'Content-Type': 'application/json' },
      }),
    )
    vi.stubGlobal('fetch', fetchMock)

    await expect(
      generateDailyPlan({
        availableMinutes: 90,
        planDate: '2026-06-16',
      }),
    ).resolves.toEqual(generated)
    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining('/api/daily-plans/generate'),
      expect.objectContaining({
        method: 'POST',
        credentials: 'include',
        body: JSON.stringify({
          planDate: '2026-06-16',
          availableMinutes: 90,
        }),
      }),
    )
  })
})

describe('getTodayPlan', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
    vi.useRealTimers()
  })

  it('returns the latest plan for today when one exists', async () => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-06-15T10:00:00'))

    const todayPlan: DailyPlanResponse = {
      id: 1,
      planDate: '2026-06-15',
      createdAt: '2026-06-15T09:00:00Z',
      availableMinutes: null,
      warning: null,
      items: [],
    }
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify(todayPlan), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      }),
    )
    vi.stubGlobal('fetch', fetchMock)

    await expect(getTodayPlan()).resolves.toEqual(todayPlan)
    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringMatching(/\/api\/daily-plans\/latest\?planDate=2026-06-15/),
      expect.objectContaining({ credentials: 'include' }),
    )
  })

  it('returns null when no plan exists for today', async () => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-06-15T10:00:00'))

    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        new Response(null, {
          status: 204,
        }),
      ),
    )

    await expect(getTodayPlan()).resolves.toBeNull()
  })
})

describe('listPlans', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('returns a paged envelope from GET /api/daily-plans', async () => {
    const envelope: PageResponse<DailyPlanSummaryResponse> = {
      content: [
        {
          id: 1,
          planDate: '2026-06-14',
          createdAt: '2026-06-14T09:00:00Z',
          itemCount: 1,
          hasWarning: false,
          availableMinutes: null,
        },
        {
          id: 2,
          planDate: '2026-06-15',
          createdAt: '2026-06-15T09:00:00Z',
          itemCount: 0,
          hasWarning: false,
          availableMinutes: null,
        },
      ],
      page: 1,
      size: 20,
      totalElements: 42,
      totalPages: 3,
    }
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify(envelope), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      }),
    )
    vi.stubGlobal('fetch', fetchMock)

    await expect(listPlans(1, 20)).resolves.toEqual(envelope)
    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringMatching(/\/api\/daily-plans\?page=1&size=20/),
      expect.objectContaining({ credentials: 'include' }),
    )
  })
})

describe('getPlanById', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('returns a plan from GET /api/daily-plans/{id}', async () => {
    const plan: DailyPlanResponse = {
      id: 42,
      planDate: '2026-06-14',
      createdAt: '2026-06-14T09:00:00Z',
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
            dueDate: '2026-06-14',
            estimatedMinutes: 45,
          },
        },
      ],
    }
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify(plan), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      }),
    )
    vi.stubGlobal('fetch', fetchMock)

    await expect(getPlanById(42)).resolves.toEqual(plan)
    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining('/api/daily-plans/42'),
      expect.objectContaining({ credentials: 'include' }),
    )
  })

  it('rethrows ApiError with status 404 when plan is not found', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        new Response(
          JSON.stringify({
            status: 404,
            message: 'Plan not found',
          }),
          {
            status: 404,
            headers: { 'Content-Type': 'application/json' },
          },
        ),
      ),
    )

    await expect(getPlanById(999)).rejects.toMatchObject({
      status: 404,
      message: 'Plan not found',
    })
    await expect(getPlanById(999)).rejects.toBeInstanceOf(ApiError)
  })
})

describe('deletePlan', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('deletes a plan on success (204)', async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(null, {
        status: 204,
      }),
    )
    vi.stubGlobal('fetch', fetchMock)

    await expect(deletePlan(1)).resolves.toBeUndefined()
    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining('/api/daily-plans/1'),
      expect.objectContaining({
        method: 'DELETE',
        credentials: 'include',
      }),
    )
  })

  it('rethrows ApiError with status 404 when plan is not found', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        new Response(
          JSON.stringify({
            status: 404,
            message: 'daily plan not found',
          }),
          {
            status: 404,
            headers: { 'Content-Type': 'application/json' },
          },
        ),
      ),
    )

    await expect(deletePlan(999)).rejects.toMatchObject({
      status: 404,
      message: 'daily plan not found',
    })
    await expect(deletePlan(999)).rejects.toBeInstanceOf(ApiError)
  })
})
