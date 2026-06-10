import { afterEach, describe, expect, it, vi } from 'vitest'

import type { TaskResponse } from '@/types/api'

import { listTasks } from '@/features/tasks/api'

describe('listTasks', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('returns tasks on success', async () => {
    const tasks: TaskResponse[] = [
      {
        id: 1,
        title: 'Task A',
        description: 'First task',
        priority: 'HIGH',
        status: 'OPEN',
        dueDate: '2026-06-15',
        estimatedMinutes: 30,
      },
      {
        id: 2,
        title: 'Task B',
        description: null,
        priority: 'MEDIUM',
        status: 'IN_PROGRESS',
        dueDate: '2026-06-20',
        estimatedMinutes: 45,
      },
    ]
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify(tasks), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      }),
    )
    vi.stubGlobal('fetch', fetchMock)

    await expect(listTasks()).resolves.toEqual(tasks)
    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining('/api/tasks'),
      expect.objectContaining({ credentials: 'include' }),
    )
  })

  it('returns an empty array when the user has no tasks', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        new Response(JSON.stringify([]), {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        }),
      ),
    )

    await expect(listTasks()).resolves.toEqual([])
  })

  it('rethrows ApiError on non-2xx responses', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        new Response(
          JSON.stringify({
            status: 500,
            message: 'Server error',
          }),
          {
            status: 500,
            headers: { 'Content-Type': 'application/json' },
          },
        ),
      ),
    )

    await expect(listTasks()).rejects.toMatchObject({
      status: 500,
      message: 'Server error',
    })
  })
})
