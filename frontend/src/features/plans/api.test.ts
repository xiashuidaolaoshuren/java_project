import { afterEach, describe, expect, it, vi } from 'vitest'

import type { DailyPlanResponse } from '@/types/api'

import { generateDailyPlan, getTodayPlan, listPlans } from '@/features/plans/api'

describe('generateDailyPlan', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('generates a plan on success (201)', async () => {
    const generated: DailyPlanResponse = {
      id: 1,
      planDate: '2026-06-15',
      createdAt: '2026-06-15T09:00:00Z',
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
        body: JSON.stringify({ availableMinutes: 120 }),
      }),
    )
  })
})

describe('getTodayPlan', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
    vi.useRealTimers()
  })

  it('returns the first plan for today when one exists', async () => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-06-15T10:00:00'))

    const todayPlan: DailyPlanResponse = {
      id: 1,
      planDate: '2026-06-15',
      createdAt: '2026-06-15T09:00:00Z',
      items: [],
    }
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify([todayPlan]), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      }),
    )
    vi.stubGlobal('fetch', fetchMock)

    await expect(getTodayPlan()).resolves.toEqual(todayPlan)
    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringMatching(/\/api\/daily-plans\?planDate=2026-06-15/),
      expect.objectContaining({ credentials: 'include' }),
    )
  })

  it('returns null when no plan exists for today', async () => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-06-15T10:00:00'))

    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        new Response(JSON.stringify([]), {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
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

  it('returns all saved plans from GET /api/daily-plans', async () => {
    const plans: DailyPlanResponse[] = [
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
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify(plans), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      }),
    )
    vi.stubGlobal('fetch', fetchMock)

    await expect(listPlans()).resolves.toEqual(plans)
    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringMatching(/\/api\/daily-plans$/),
      expect.objectContaining({ credentials: 'include' }),
    )
  })
})
